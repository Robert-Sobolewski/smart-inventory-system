# Testing Guide — Frontend (Angular 22 + Vitest)

> A practical guide to writing unit tests in this project.
> All examples are **verified** (`ng test frontend` → 24/24 passed, 2026-10-06).
> Convention: UI messages in German (domain language), comments and tests in English.

## 0. Setup — what already works

| Element | Where | Note |
|---|---|---|
| Runner | **Vitest** (`@analogjs/vite-plugin-angular`) | Angular 22 uses Vitest by default, not Karma/Jasmine |
| Configuration | `src/frontend/vitest.config.ts` | `globals: true`, `environment: 'jsdom'` |
| Coverage | `@vitest/coverage-v8` | `ng test frontend --watch=false --coverage` |
| Setup file | `src/test-setup.ts` | loaded via `setupFiles` |

**Important:** `globals: true` means that `describe`, `it`, `expect`, `vi` are available **globally — without imports**. You do not need to write `import { vi } from 'vitest'`.

Running the tests:
```bash
cd src/frontend
node ./node_modules/@angular/cli/bin/ng test frontend --watch=false            # full suite
node ./node_modules/@angular/cli/bin/ng test frontend --watch=false --coverage # + coverage
# single file (fast loop while writing):
node ./node_modules/@angular/cli/bin/ng test frontend --watch=false -t "Status"
```

---

## 1. SERVICE TESTS — mocking HTTP without a network

Services in this project use `HttpClient` (and `httpResource`). In tests we **do not connect to the backend** — we plug in `HttpTestingController`, which intercepts every request and lets you manually "respond" (`flush`).

Skeleton (see the real example: `services/logistics-service.examples.spec.ts`):

```ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';                 // ← HERE, not in /testing!
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { LogisticsService } from './logistics-service';

describe('LogisticsService', () => {
  let service: LogisticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LogisticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // FAILS if any request was not consumed — catches forgotten mocks
  });

  it('getStock parses a number from plain text', async () => {
    const qtyPromise = firstValueFrom(service.getStock('RES-10K-OHM-001'));
    // expectOne: intercepts the request for this URL (method + path must match)
    const req = httpMock.expectOne('/api/logistics/stock/RES-10K-OHM-001');
    expect(req.request.method).toBe('GET');
    // flush: simulates the backend response (plain text here, because responseType:'text')
    req.flush('Gesamtbestand fuer RES-10K-OHM-001 : 50 Stueck');
    expect(await qtyPromise).toBe(50);
  });

  it('issueGoods sends a payload to /issue', async () => {
    const msgPromise = firstValueFrom(service.issueGoods({ sku: 'SKU-001', qty: 5 }));
    const req = httpMock.expectOne('/api/logistics/issue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ sku: 'SKU-001', qty: 5 }); // BODY validation
    req.flush('Warenausgang (FIFO) fuer SKU: SKU-001 erfolgreich durchgefuert!');
    expect(await msgPromise).toContain('erfolgreich');
  });
});
```

### HTTP service pitfalls in this project
1. **`provideHttpClient` lives in `@angular/common/http`, NOT in `/testing`.** Importing it from `/testing` is a compile error (TS2305). This is the most common mistake on your first service test.
2. **Plain text vs JSON.** The backend returns `ResponseEntity<String>` = plain text. The service uses `{ responseType: 'text' }`. So in tests, `req.flush(...)` must deliver **text**, not an object — otherwise the test does not reflect reality.
3. **`httpResource` (not Observable).** Fields like `readonly locationResource = httpResource<...>()` are *signals*, not Observables. You do not mock them via `HttpTestingController.flush` directly — you check them after `fixture.detectChanges()` as `component.locationResource.value()`.
4. **⚠️ `httpResource` sends its GET ASYNCHRONOUSLY (Angular effects).** If a service has an `httpResource` field, then on `TestBed.inject(Service)` the GET request goes into the testing backend queue — but **not synchronously** (you cannot `expectOne().flush()` it in `beforeEach` — it throws "found none"). Solution: drain leftover requests in `afterEach` BEFORE `verify()`:
   ```ts
   afterEach(() => {
     for (const req of httpMock.match('/api/locations')) {
       req.flush([]); // simulated response for the auto-fetch from httpResource
     }
     httpMock.verify(); // now the queue is empty → no "open requests"
   });
   ```
5. **Order: call the method first, then `expectOne`, then `flush`.** `firstValueFrom(...)` subscribes **immediately** — the request enters the queue at the moment the service method is called. If you do `expectOne()` BEFORE the call → "found none" (the request does not exist yet). If you forget `flush()` → the Observable never emits → **timeout 5000ms**.

---

## 2. COMPONENT TESTS — signal logic (without rendering)

Components keep their state in **signals** (`signal`, `computed`). We test business logic by driving those signals and checking the results of `computed`. This is the fastest test layer — we do not touch the DOM.

Problem: signals are `protected` (encapsulation). In tests we make a **typed cast** to a small view — the compiler still checks every access, no `any`:

```ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { GoodsIssue } from './goods-issue';

interface IssueFormModel { sku: string; qty: number | null; }

// Small typed view — only what the test touches. No `any`.
type TestableGoodsIssue = {
  overStock: import('@angular/core').Signal<boolean>;
  canSubmit: import('@angular/core').Signal<boolean>;
  issueModel: import('@angular/core').WritableSignal<IssueFormModel>;
  stockAvailable: import('@angular/core').WritableSignal<number | null>;
  stockLoading: import('@angular/core').WritableSignal<boolean>;
};

describe('GoodsIssue (logic)', () => {
  let fixture: ComponentFixture<GoodsIssue>;
  let component: TestableGoodsIssue;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoodsIssue],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(GoodsIssue);
    component = fixture.componentInstance as unknown as TestableGoodsIssue; // cast ONCE
  });

  it('overStock is true only when qty > available stock', () => {
    component.stockAvailable.set(50);
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 20 }));
    expect(component.overStock()).toBe(false);

    component.issueModel.update((m) => ({ ...m, qty: 99 }));
    expect(component.overStock()).toBe(true);
  });

  it('canSubmit requires a valid form + known stock + no shortage', () => {
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 5 }));
    expect(component.canSubmit()).toBe(false);      // stock still unknown

    component.stockAvailable.set(50);
    expect(component.canSubmit()).toBe(true);       // stock known and sufficient

    component.issueModel.update((m) => ({ ...m, qty: 99 }));
    expect(component.canSubmit()).toBe(false);      // shortage → blocked
  });
});
```

### Signal-component pitfalls
1. **`Pick<Comp, 'x'>` does not work on `protected`.** Compiler: `TS2344 ... does not satisfy keyof GoodsIssue`. Instead define your own `type TestableX = { ... }` (as above) and cast with `as unknown as TestableX`.
2. **The type of `computed()` is `Signal<T>`, NOT `Computed<T>`.** `@angular/core` does not export a `Computed` type — using `import('@angular/core').Computed<...>` gives `TS2694 ... has no exported member 'Computed'`. Use `Signal<boolean>`.
3. **`update()` vs `set()`.** For objects (form models) use `issueModel.update(m => ({...m, qty: 5}))` to keep the remaining fields. `set()` replaces the whole object.

---

## 3. HTML / TEMPLATE TESTS — what the user sees

This layer renders the component (`fixture.detectChanges()`) and checks the **DOM** — whether an alert appeared, whether a button is disabled, whether some text showed up. These are "user perspective" tests.

```ts
it('shows a shortage alert and blocks submit when qty > stock', () => {
  component.stockAvailable.set(13);
  component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 20 }));
  fixture.detectChanges(); // ← renders the template after signal changes

  const alert = fixture.nativeElement.querySelector('.stock-error');
  expect(alert).toBeTruthy();
  expect(alert.textContent).toContain('Nicht genug Bestand!');

  const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type=submit]');
  expect(button.disabled).toBe(true); // the disabled attribute on <button>
});

it('hides the alert when qty fits within stock', () => {
  component.stockAvailable.set(13);
  component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 5 }));
  fixture.detectChanges();
  expect(fixture.nativeElement.querySelector('.stock-error')).toBeNull(); // no element = hidden
});
```

### Template test pitfalls
1. **Always `fixture.detectChanges()` after a signal change** before checking the DOM — otherwise the template is not refreshed and you see stale state.
2. **Selectors must match the `.html`.** Use semantic classes (`.stock-error`, `.stock-state`) or attributes (`button[type=submit]`), not child order — brittle under refactoring.
3. **`disabled` is an attribute, not text.** `expect(button.disabled).toBe(true)` — not `textContent`.

---

## 4. DEPENDENCY (DI) TESTS — SPY vs plain function ⚠️

When a component depends on services (`AuthService`, `Router`) and you want to check **whether a method was called**, the mock must be a **spy** — not a plain function. This is the most common mistake (encountered in `status.spec.ts`).

### Why a plain function does not work
```ts
// ❌ A PLAIN arrow records NOTHING → .toHaveBeenCalled() throws:
//    "TypeError: [Function logout] is not a spy or a call to a spy!"
logout: () => { isLogoutCalled = true; },   // manual flag = workaround, not a test
navigate: (arg) => {},                        // empty function, zero call history
```

`vi.fn()` creates a **spy** — a function that *records every call* (how many times, with which arguments). Only on a spy can you assert.

### Correctly (`status.spec.ts`)
```ts
beforeEach(async () => {
  mockAuthService = {
    isAuthenticated: signal(true),
    getUsername: () => 'manager',   // plain function OK — we do not assert on it
    logout: vi.fn(),                // ← SPY (we assert that it was called)
  };
  mockRouter = { navigate: vi.fn() }; // ← SPY

  await TestBed.configureTestingModule({
    imports: [Status],
    providers: [
      { provide: Router, useValue: mockRouter },
      { provide: AuthService, useValue: mockAuthService },
    ],
  }).compileComponents();
});

it('onLogout calls authService.logout() and router.navigate("/login")', () => {
  const fixture = TestBed.createComponent(Status);
  fixture.componentInstance.onLogout();

  expect(mockAuthService.logout).toHaveBeenCalledTimes(1);   // exactly once
  expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']); // with this argument
});
```

### Rule to remember
| What you want to check | Use in the mock |
|---|---|
| Only **return** a value (e.g. `getUsername()`) | plain function: `() => 'manager'` ✅ |
| **Check that a method was called** / with which args | **spy**: `vi.fn()` ✅ |

- `vi.fn()` — a pure spy (returns `undefined`, unless you provide an implementation: `vi.fn(() => 'x')`).
- `vi.spyOn(realObj, 'method')` — a spy on an *existing* method of a real object (keeps the original logic). For object mocks the standard is `vi.fn()`.

---

## 5. Test layers summary (what and when)

| Layer | What it tests | Tooling | Example in repo |
|---|---|---|---|
| **Service** | HTTP logic, response parsing | `HttpTestingController` + `firstValueFrom` | `logistics-service.examples.spec.ts` |
| **Component (logic)** | signals / `computed`, business rules | typed cast to signals, no DOM | `goods-issue.examples.spec.ts` (logic section) |
| **Component (HTML)** | template rendering, alerts, disabled | `fixture.detectChanges()` + `querySelector` | `goods-issue.examples.spec.ts` (HTML section) |
| **DI dependencies** | whether the component calls service/router | `vi.fn()` spy + `useValue` provider | `status.spec.ts` |

> IHK standard: **controller = syntactical validation, service = semantic (business logic).**
> The frontend is analogous: component = UI + orchestration, service = API communication. Service tests check *semantics* (parsing, payload), component tests — *syntax* (whether the UI reacts correctly to state).
