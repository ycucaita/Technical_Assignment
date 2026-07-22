package com.example.similarproducts.infrastructure.logging;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

/**
 * Bridges the correlation id between the Reactor {@code Context} and SLF4J's {@link MDC}.
 *
 * <p>Registered once with the global {@code ContextRegistry}. With
 * {@code Hooks.enableAutomaticContextPropagation()} active, Reactor invokes this accessor to
 * restore the id into the MDC of whatever worker thread runs each operator, and to clear it
 * afterwards — which is why every log line emitted anywhere in the request pipeline carries the id
 * even though the work is spread across Netty event-loop and scheduler threads.
 *
 * <p>The lifecycle methods beyond {@code key/getValue/setValue} intentionally omit {@code @Override}
 * so the class compiles against the minor variations of this interface across
 * context-propagation versions.
 */
public final class MdcCorrelationIdAccessor implements ThreadLocalAccessor<String> {

    @Override
    public Object key() {
        return CorrelationId.KEY;
    }

    @Override
    public String getValue() {
        return MDC.get(CorrelationId.KEY);
    }

    @Override
    public void setValue(String value) {
        if (value == null) {
            MDC.remove(CorrelationId.KEY);
        } else {
            MDC.put(CorrelationId.KEY, value);
        }
    }

    // --- lifecycle (no @Override on purpose; see class javadoc) ---

    public void setValue() {
        MDC.remove(CorrelationId.KEY);
    }

    public void restore() {
        MDC.remove(CorrelationId.KEY);
    }

    public void restore(String previousValue) {
        setValue(previousValue);
    }
}
