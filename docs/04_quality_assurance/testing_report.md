# Quality Assurance & Testing Report - SIS-IE
Last Updated: October 14, 2026

## 1. Testing Strategy
To ensure industrial stability and maintainability, a **Testing Pyramid** approach was implemented. This ensures that logic is verified at the lowest possible level before being tested in integration.

### 1.1 Test Levels
| Level | Tooling | Focus | Goal |
| :--- | :--- | :--- | :--- |
| **Unit Tests** | JUnit 5, Mockito | Business Logic (`Service` layer) | 100% logic coverage of FIFO and Auth rules. |
| **Slice Tests** | `@WebMvcTest`, `MockMvc` | API Contract (`Controller` layer) | Verify HTTP Status codes, DTO mapping, and Security roles. |
| **Integration** | `.rest` files / H2 DB | End-to-End Flow | Verification of the full request-response cycle. |

## 2. Coverage Analysis (JaCoCo)
The project utilizes **JaCoCo** for automated code coverage measurement.

### 2.1 Final Metrics
- **Overall Instruction Coverage**: ~97%
- **Branch Coverage**: ~97%
- **Critical Path Coverage**: 100% (`com.sisie.core.service`, `com.sisie.core.controller`)

### 2.2 Interpretation of Results
The high coverage rate proves that the system is robust against regressions. The remaining 3% of uncovered code resides primarily in the main application entry point (`SmartInventorySystemApplication`), which contains no business logic and does not require testing according to industry standards.

## 3. Mutation Testing (Pitest)
To ensure the *quality* of the tests, **Pitest** was integrated. This prevents "fake coverage" by introducing mutants into the code to see if the tests can detect them.

## 4. Frontend Testing (Angular 22 + Vitest)
The Angular SPA is tested with **Vitest** (`@analogjs/vite-plugin-angular`, jsdom environment — config in `src/frontend/vitest.config.ts`). Status as of October 14, 2026:

- **15 spec files / 116 tests — all passing** (`yarn test:run` in `src/frontend/`)
- Coverage via `@vitest/coverage-v8` (`yarn test:coverage`, HTML report in `coverage/`)
- Service specs use `HttpTestingController` + `provideHttpClientTesting()` (Angular 22 subpath `@angular/common/http/testing`) — no network, deterministic
- Component specs render real templates with mocked services; signal-based resources are tested through their public API (`value()`, `isLoading()`, `reload()`)

> Note: `ng test` also works (same specs, full type checking) but uses its own zone.js environment and does not load `src/test-setup.ts`; the Vitest commands in `package.json` are the source of truth for "tests pass".
