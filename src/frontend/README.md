# Frontend

Angular 22 SPA for the Smart Inventory System. Generated with [Angular CLI](https://github.com/angular/angular-cli) v22.1.8, package manager: **Yarn 4**.

## Prerequisites

- Node.js 20+ (LTS), Yarn 4 (`corepack enable`)
- Backend (Spring Boot) running on `http://127.0.0.1:8888` — the dev server proxies `/api/*` to it (see `proxy.conf.json`)

## Development server

```bash
yarn start        # = ng serve
```

Open `http://localhost:4200/`. The app hot-reloads on every source change. API calls (`/api/...`) are proxied to the backend at `127.0.0.1:8888` — **start the backend first**, otherwise requests will fail with 504/ECONNREFUSED.

## Code scaffolding

```bash
yarn ng generate component feature-name/component-name
```

For all available schematics (`component`, `directive`, `pipe`, ...): `yarn ng generate --help`.

## Building

```bash
yarn build        # = ng build (production, optimized) -> dist/frontend/browser/
yarn watch        # development build, rebuilds on change
```

The production build is what you deploy; the dev server (`yarn start`) is for local work.

## Running unit tests

This project uses **[Vitest](https://vitest.dev/)** as its test runner (jsdom environment, Angular setup in `src/test-setup.ts`, config in `vitest.config.ts`). Use the scripts from `package.json`:

| Command | What it does |
|---|---|
| `yarn test:run` | Runs **all tests once** and exits — use this for CI / final verification |
| `yarn test:vitest` | **Watch mode** — re-runs affected tests on every file change, use while writing code |
| `yarn test:coverage` | Tests + coverage report (HTML in `coverage/`) |

> **Note about `ng test`:** it also works and runs the same specs through Angular's unit-test builder (`@angular/build:unit-test`, with full type checking). However, that builder uses its own zone.js environment and does **not** load `src/test-setup.ts`, so helpers like `fakeAsync()` can fail there. For this project, prefer the Vitest commands above — they are the source of truth for "tests pass".

## Running end-to-end tests

No e2e framework is configured yet (Angular CLI ships without one). If needed later: [Playwright](https://playwright.dev/) or [Cypress](https://www.cypress.io/).

## Additional Resources

- [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli)
- [Vitest documentation](https://vitest.dev/)
