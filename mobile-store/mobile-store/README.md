# 📱 Mobile Store Single Page Application (SPA)

A high-performance, modular Single Page Application built for browsing and purchasing mobile devices. Designed using **Clean Architecture**, **SOLID principles**, and **Component-Driven Design**, this solution prioritizes maintainability, strict layer separation, and seamless client-side performance.

---

## 🏗️ Architectural Overview

The application follows a **Modular App** architecture with strict separation of concerns, ensuring that the codebase remains decoupled and easily adaptable.


> **Architectural Highlights**
> * **Clean Architecture:** Core business domain logic, state management, and infrastructure (API requests) are completely decoupled from UI presentation.
> * **SOLID Principles:**
> * *Single Responsibility (SRP):* Modules like `api.js`, `CartContext.jsx`, and `ProductItem.jsx` each have a single, well-defined domain responsibility.
> * *Dependency Inversion (DIP):* React UI components consume abstract custom hooks rather than coupling directly to `localStorage` or `fetch` APIs.
> 
> 
> * **Client-Side Caching Strategy:** Custom transparent caching layer over `localStorage` enforcing a strict **1-hour expiration window** to minimize redundant network calls.
> * **Responsive Material Design:** Custom Material-UI theme implementing the required brand identity (`#E8A07C`) with full accessibility compliance.
> 
> 

---

## 🎯 Architectural Decisions & Advantages

| Decision | Strategic Benefit |
| --- | --- |
| **Vite over CRA** | Near-instantaneous Server Start and Hot Module Replacement (HMR) for optimal developer velocity and optimized production bundling. |
| **Modular SPA Layout** | Fluid client-side navigation without full page reloads, preserving client state across views while eliminating server rendering overhead. |
| **Context API + LocalStorage** | Eliminates the bundle weight and boilerplate of external state libraries (e.g., Redux) for this scope, delivering lightweight, persistent state. |
| **Colocated Unit Tests** | Placing `.test.js` files adjacent to source code ensures high visibility of coverage, seamless refactoring, and natural MFE extraction. |

---

## 🚀 Quick Start

### Prerequisites

* **Node.js**: `v18.0.0` or higher
* **npm**: `v9.0.0` or higher

### Installation & Execution

```bash
# 1. Clone the repository
git clone <repository-url>
cd mobile-store

# 2. Install dependencies
npm install

# 3. Start development server
npm run START

```

---

## 📜 Available Scripts

The project provides standard CLI scripts to manage development, compilation, testing, and code quality:

| Script | Command | Description |
| --- | --- | --- |
| **`npm run START`** | `vite` | Starts local development server at `http://localhost:5173` with HMR. |
| **`npm run BUILD`** | `vite build` | Compiles and optimizes production build artifacts into `/dist`. |
| **`npm run TEST`** | `vitest run` | Executes complete unit and UI test suite via Vitest & React Testing Library. |
| **`npm run LINT`** | `eslint .` | Runs ESLint static analysis to enforce code formatting and hook rules. |

---


## 🔮 Roadmap & Scaling Strategy

1. **Microfrontend Module Federation:** Extract the catalog (PLP) and checkout (PDP/Cart) into independent Microfrontends using Vite Module Federation.
2. **TypeScript Migration:** Introduce strict typing to formalize domain data contracts between API response models and presentation layers.
3. **Optimistic UI Updates:** Reflect item additions instantly in the UI before network requests complete, backed by rollback handlers for enhanced perceived performance.
4. **List Virtualization:** Implement windowed rendering (`react-window`) to smoothly render thousands of catalog items in the PLP without degrading DOM performance.

```

```