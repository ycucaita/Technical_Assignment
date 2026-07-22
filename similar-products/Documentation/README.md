# Similar Products Service

A reactive, resilient microservice that aggregates "similar product" details from two
upstream endpoints into a single response. Built with **Java 25 + Spring Boot 4 (WebFlux)**,
following **hexagonal architecture (ports & adapters)** and **Domain-Driven Design**, with an
explicit circuit-breaker and graceful degradation on partial upstream failure.

> Written against Spring Boot 4.0.7 / Spring Framework 7 / Resilience4j 2.3.0 / Java 25.

---

## Table of contents

1. [Why hexagonal architecture + DDD](#why-hexagonal-architecture--ddd)
2. [Resilience: circuit breaker, retry, bulkhead, timeout](#resilience-circuit-breaker-retry-bulkhead-timeout)
3. [Caching strategy](#caching-strategy)
4. [Request tracing / logging](#request-tracing--logging)
5. [Technology selection and rationale](#technology-selection-and-rationale)
6. [Design patterns used](#design-patterns-used)
7. [Running the app](#running-the-app)
8. [Trade-offs made](#trade-offs-made)
9. [Possible improvements](#possible-improvements)

---

## Why hexagonal architecture + DDD

The service sits between two things that change independently and for different reasons: the
**business rule** ("aggregate similar products, degrade gracefully on partial failure") and the
**transport details** (WebFlux, WebClient, Resilience4j, Caffeine, Jackson). Hexagonal
architecture keeps those concerns from leaking into each other:

- The **domain** and **application** layers have zero framework imports. `GetSimilarProductsService`
  is a plain Java class — you can unit-test the entire orchestration logic (ordering, degradation,
  partial failure) with plain Mockito and Reactor's `StepVerifier`, with no Spring context, no
  WireMock, and sub-second test run times.
- **Ports** (interfaces) describe *what* the application needs (`SimilarProductIdsProvider`,
  `ProductDetailProvider`) and *what* it offers (`GetSimilarProductsUseCase`), without saying
  *how*.
- **Dependency Inversion** is enforced structurally: `infrastructure` depends on `application`
  depends on `domain`, never the other way around. `config` is the composition root — the one
  place allowed to see (and wire together) every layer.
- **DDD** shows up in a deliberately small but real model: `Product` and `ProductId` are the
  aggregate/value-object pair.

## Resilience: circuit breaker, retry, bulkhead, timeout

`ResilienceDecorator` composes all four concerns as **explicit Reactor operators** (not
annotations — see [Trade-offs](#trade-offs-made)), applied in this order, from the innermost call
outward:

```
call → Bulkhead → timeout → CircuitBreaker → Retry
```

- **Bulkhead** caps how many calls can be in flight to a given dependency at once, protecting it
  (and the connection pool) from being overwhelmed by a traffic spike or a slow downstream.
- **Timeout** puts a hard ceiling on a single call; a slow response is treated as a failure rather
  than left to hang and exhaust the pool.
- **Circuit breaker** sits *outside* the bulkhead, so once open it rejects calls immediately,
  without even consuming a bulkhead permit — fast-fail during an outage, no wasted concurrency
  budget.
- **Retry** is outermost and re-attempts only genuine transient faults.

**Classification is the key design decision here:** A `404` from the upstream is a normal,
expected outcome (the product genuinely doesn't exist) — not a fault:

| Outcome | Retried? | Counts toward breaker? | Rationale |
|---|---|---|---|
| `404` → `ProductNotFoundException` | No | No | Not a failure — a valid business answer |
| `5xx` / bad status → `UpstreamServerException` | Yes | Yes | Genuine transient server fault |
| Connection reset / DNS / read failure (`WebClientRequestException`) | Yes | Yes | Genuine transport fault |
| Timeout | Yes | Yes (as a *slow call*) | Counts toward the breaker's slow-call rate too |
| `CallNotPermittedException` (circuit open) | No | — | Already rejected; retrying would defeat the breaker |
| `BulkheadFullException` | No | — | Retrying would add more load to an already-saturated dependency |

## Caching strategy

`ReactiveCache<V>` wraps a Caffeine `AsyncCache<String, V>`, one instance per dependency
(`similar-ids`, `product-detail`), each independently sized and TTL'd.

- **Stampede protection**: Caffeine's `AsyncCache.get(key, loader)` coalesces concurrent misses for
  the *same key* onto a single in-flight `CompletableFuture` — a burst of simultaneous requests for
  the same product triggers exactly one upstream call, not one per caller. This is the property
  that matters most under the "high traffic / heavy load" requirement.
- **Failures are never cached**: if the loader's future completes exceptionally, Caffeine evicts it
  immediately, so a transient upstream error is never "stuck" being replayed to every subsequent
  caller until TTL expiry.


## Request tracing / logging

Every request is assigned a **correlation id**, propagated end-to-end through the logs — including
across the thread hops WebFlux introduces (Netty event-loop threads, parallel schedulers for the
fan-out) — which is the detail that makes log-based debugging actually work in a reactive app.

## Technology selection and rationale

| Concern | Chosen | Why | Alternative considered |
|---|---|---|---|
| Web stack | Spring WebFlux (Netty) | Non-blocking I/O: a small, fixed-size event-loop thread pool can hold open thousands of concurrent upstream calls, which matters directly for the fan-out (N detail calls per request) under load. Blocking Servlet stack would need a thread per in-flight call. | Spring MVC (Tomcat, blocking) |
| Resilience | Resilience4j 2.3.0 **core modules** (`circuitbreaker`, `retry`, `bulkhead`, `reactor`, `micrometer`), applied as Reactor operators | Framework-agnostic, no Spring coupling, works identically regardless of Boot version churn. Annotation-driven resilience (the `resilience4j-spring-boot3` starter) has unreliable auto-configuration on Spring Boot 4 + Jackson 3 at time of writing, and annotations are awkward to apply correctly to reactive (`Mono`/`Flux`) return types. | `resilience4j-spring-boot3` starter with `@CircuitBreaker`/`@Retry` annotations |
| Caching | Caffeine `AsyncCache`, wrapped manually | Native async loader with built-in stampede coalescing and no-cache-on-failure, with explicit control over composition with the resilience pipeline. | `@Cacheable` with Spring's reactive cache adapter (async cache mode, `sync=true`) |
| API docs | springdoc-openapi 3.0.3 (`-starter-webflux-ui`) | The 3.x line is the one compatible with Spring Boot 4; the 2.x line has a known breaking package move (`WebFluxProperties`) against Boot 4. | springfox (unmaintained, incompatible) |
| Testing (integration) | WireMock **standalone** (shaded) | The standalone artifact shades its own Jetty, avoiding a version clash with the Jetty transitively pulled in elsewhere on a Spring Boot 4 / Jackson 3 classpath. | Plain `wiremock-jre8` (non-shaded) |
| Immutability | Java `record` for `Product`, `ProductId` | Value semantics, `equals`/`hashCode`/`toString` for free, and a compact canonical constructor for validation (`ProductId` rejects blank ids). | Lombok `@Value` classes |

## Design patterns used

- **Ports & Adapters (Hexagonal Architecture)** — driving port (`GetSimilarProductsUseCase`) and
  driven ports (`SimilarProductIdsProvider`, `ProductDetailProvider`), with all framework code
  confined to adapters.
- **Dependency Inversion Principle** — the application depends on port *interfaces*; concrete
  adapters are wired in at the composition root (`config`), never referenced directly by the core.
- **Interface Segregation** — the outbound contract is split into two single-method interfaces
  rather than one fat "upstream client" interface, even though one adapter (`ProductApiClient`)
  happens to implement both.
- **Decorator** — `ResilienceDecorator.decorate(...)` wraps an arbitrary `Mono<T>` with the full
  cross-cutting resilience stack without the caller (the client) needing to know the composition.
- **Adapter** — `ProductApiClient` adapts the external HTTP contract to the domain-shaped ports;
  `ProductDtoMapper` / `ProductResponseMapper` adapt between wire DTOs and the domain model in each
  direction independently, so the domain `Product` never leaks onto the wire and the upstream DTO
  never leaks into the domain.
- **Factory Method** — `Product.of(...)` / `Product.idOnly(...)` are named constructors expressing
  domain intent, instead of a telescoping/null-heavy constructor call at every call site.


## Running the app

```bash
# 1. Start the upstream mock (e.g. the challenge's Docker image) so it's listening on :3001
# 2. Run the service
mvn spring-boot:run
# -> http://localhost:5000
```

- Swagger UI: `http://localhost:5000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:5000/v3/api-docs`
- Health: `http://localhost:5000/actuator/health`
- Prometheus metrics (includes circuit-breaker/retry/bulkhead gauges): `http://localhost:5000/actuator/prometheus`

```bash
curl -i http://localhost:5000/product/1/similar
```
## Trade-offs made

- **Resilience4j core modules over the Spring Boot starter:** Gained: version-safety and full
  control over reactive composition. Cost: no annotation-driven `@CircuitBreaker`/`@Retry` — every
  call site has to explicitly go through `ResilienceDecorator`.
- **Manual `ReactiveCache` over `@Cacheable`:** As of Spring 6.1+, `@Cacheable` *does* correctly
  support `Mono`/`Flux` return types when async cache mode is enabled, and even supports coalescing
  concurrent misses (`sync=true`) — so this is not a hard technical limitation on this Spring
  version. It was still built manually here because: (a) `@Cacheable` only intercepts calls that
  enter the bean from *outside*, via the Spring AOP proxy — the private `fetchSimilarIds`/
  `fetchDetail` methods being cached are called from within the same bean (`ProductApiClient`),
  which is a proxy self-invocation the annotation cannot intercept without restructuring the client
  onto a separate bean; and (b) mixing declarative caching with the programmatic Resilience4j
  operator chain is harder to read than one explicit `cache → resilience → HTTP` composition.
  Trade-off: ~30 lines of custom code to maintain, versus a one-line annotation, in exchange for
  guaranteed semantics (failed loads are never cached; stampede coalescing is explicit).
- **Correlation id via manual MDC/Reactor-context bridging, not Micrometer Tracing:** Gained: a
  minimal dependency footprint and full control over the header/log format.

## Possible improvements

Roughly in order of value for a production deployment:

- **Rate limiting on the inbound side:** Resilience4j's `RateLimiter` module (already implicitly
  available since the other core modules are on the classpath) could protect the service itself
  from being overwhelmed, complementing the outbound bulkheads.
- **Cache warm-up / precomputation for known-hot products:** If popular products are predictable
  (e.g. from analytics), a scheduled warm-up job could pre-populate the cache ahead of traffic
  spikes rather than relying purely on reactive stampede protection.
- **Idempotent request coalescing across replicas:** The current stampede protection is per-JVM
  (Caffeine is in-memory); a shared cache (Redis) would extend the same protection across a
  horizontally scaled fleet, at the cost of network round-trips replacing in-memory reads.
- **Bulkhead/circuit-breaker dashboards:** The Prometheus metrics are already exported; adding a
  ready-made Grafana dashboard JSON (breaker state transitions, bulkhead saturation, retry counts)
  would shorten the path from "metric exists" to "someone notices the outage."
- **Security:**  we can implement the security part base ond spring security.

