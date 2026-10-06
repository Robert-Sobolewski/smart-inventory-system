# Testing Guide — Frontend (Angular 22 + Vitest)

> Praktyczny przewodnik do pisania testów jednostkowych w tym projekcie.
> Wszystkie przykłady są **zweryfikowane** (`ng test frontend` → 24/24 passed, 2026-10-06).
> Konwencja: komunikaty UI po niemiecku, komentarze/testy po polsku (język nauki).

## 0. Setup — co już działa

| Element | Gdzie | Uwaga |
|---|---|---|
| Runner | **Vitest** (`@analogjs/vite-plugin-angular`) | Angular 22 domyślnie używa Vitest, nie Karma/Jasmine |
| Konfiguracja | `src/frontend/vitest.config.ts` | `globals: true`, `environment: 'jsdom'` |
| Coverage | `@vitest/coverage-v8` | `ng test frontend --watch=false --coverage` |
| Setup plik | `src/test-setup.ts` | ładowany przez `setupFiles` |

**Ważne:** `globals: true` oznacza, że `describe`, `it`, `expect`, `vi` są dostępne **globalnie — bez importu**. Nie musisz pisać `import { vi } from 'vitest'`.

Uruchamianie:
```bash
cd src/frontend
node ./node_modules/@angular/cli/bin/ng test frontend --watch=false            # pełny zestaw
node ./node_modules/@angular/cli/bin/ng test frontend --watch=false --coverage # + coverage
# pojedynczy plik (szybka pętla podczas pisania):
node ./node_modules/@angular/cli/bin/ng test frontend --watch=false -t "Status"
```

---

## 1. Test SERWISU — mockowanie HTTP bez sieci

Serwisy w tym projekcie używają `HttpClient` (i `httpResource`). W teście **nie łączymy się z backendem** — podpinamy `HttpTestingController`, który przechwytuje każdy request i pozwala ręcznie "odpowiedzieć" (`flush`).

Szkielet (patrz realny przykład: `services/logistics-service.examples.spec.ts`):

```ts
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';                 // ← TU, nie w /testing!
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
    httpMock.verify(); // FAIL, jeśli jakiś request nie został skonsumowany — łapie zapomniane mocki
  });

  it('getStock parsuje liczbę z plain text', async () => {
    const qtyPromise = firstValueFrom(service.getStock('RES-10K-OHM-001'));
    // expectOne: przechwyt requesta na ten URL (metoda + ścieżka muszą się zgadzać)
    const req = httpMock.expectOne('/api/logistics/stock/RES-10K-OHM-001');
    expect(req.request.method).toBe('GET');
    // flush: symuluje odpowiedź backendu (tu plain text, bo responseType:'text')
    req.flush('Gesamtbestand fuer RES-10K-OHM-001 : 50 Stueck');
    expect(await qtyPromise).toBe(50);
  });

  it('issueGoods wysyła payload do /issue', async () => {
    const msgPromise = firstValueFrom(service.issueGoods({ sku: 'SKU-001', qty: 5 }));
    const req = httpMock.expectOne('/api/logistics/issue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ sku: 'SKU-001', qty: 5 }); // walidacja BODY
    req.flush('Warenausgang (FIFO) fuer SKU: SKU-001 erfolgreich durchgefuert!');
    expect(await msgPromise).toContain('erfolgreich');
  });
});
```

### Pułapki serwisów HTTP w tym projekcie
1. **`provideHttpClient` jest w `@angular/common/http`, NIE w `/testing`.** Import z `/testing` da błąd kompilacji (TS2305). To najczęstszy błąd przy pierwszym teście serwisu.
2. **Plain text vs JSON.** Backend zwraca `ResponseEntity<String>` = plain text. Serwis używa `{ responseType: 'text' }`. W teście `req.flush(...)` musi więc dostarczać **tekst**, nie obiekt — inaczej test nie odzwierciedla rzeczywistości.
3. **`httpResource` (nie Observable).** Pola typu `readonly locationResource = httpResource<...>()` to *sygnały*, nie Observable. Nie mockujesz ich przez `HttpTestingController.flush` wprost — sprawdzasz je po `fixture.detectChanges()` jako `component.locationResource.value()`.
4. **⚠️ `httpResource` wysyła GET ASYNCHRONICZNIE (efekty Angulara).** Jeśli serwis ma pole `httpResource`, to przy `TestBed.inject(Serwis)` request GET idzie do kolejki testing backendu — ale **nie synchronicznie** (nie da się go `expectOne().flush()` w `beforeEach` — rzuca "found none"). Rozwiązanie: w `afterEach` drenuj pozostałe requesty PRZED `verify()`:
   ```ts
   afterEach(() => {
     for (const req of httpMock.match('/api/locations')) {
       req.flush([]); // symulowana odpowiedź dla auto-fetch z httpResource
     }
     httpMock.verify(); // teraz kolejka jest pusta → nie ma "open requests"
   });
   ```
5. **Kolejność: najpierw wywołaj metodę, potem `expectOne`, potem `flush`.** `firstValueFrom(...)` subskrybuje **natychmiast** — request idzie do kolejki w momencie wywołania metody serwisu. Jeśli zrobisz `expectOne()` PRZED wywołaniem → "found none" (request jeszcze nie istnieje). Jeśli zapomnisz `flush()` → Observable nigdy nie emituje → **timeout 5000ms**.

---

## 2. Test KOMPONENTU — logika sygnałów (bez renderowania)

Komponenty trzymają stan w **sygnałach** (`signal`, `computed`). Logikę biznesową testujemy, sterując tymi sygnałami i sprawdzając wyniki `computed`. To najszybsza warstwa testów — nie ruszamy DOM.

Problem: sygnały są `protected` (encapsulacja). W teście robimy **typed cast** do małego widoku — kompilator nadal sprawdza każdy dostęp, bez `any`:

```ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { GoodsIssue } from './goods-issue';

interface IssueFormModel { sku: string; qty: number | null; }

// Mały typed view — tylko to, co test dotyka. Bez `any`.
type TestableGoodsIssue = {
  overStock: import('@angular/core').Signal<boolean>;
  canSubmit: import('@angular/core').Signal<boolean>;
  issueModel: import('@angular/core').WritableSignal<IssueFormModel>;
  stockAvailable: import('@angular/core').WritableSignal<number | null>;
  stockLoading: import('@angular/core').WritableSignal<boolean>;
};

describe('GoodsIssue (logika)', () => {
  let fixture: ComponentFixture<GoodsIssue>;
  let component: TestableGoodsIssue;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoodsIssue],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(GoodsIssue);
    component = fixture.componentInstance as unknown as TestableGoodsIssue; // cast RAZ
  });

  it('overStock jest true tylko gdy qty > dostępny stan', () => {
    component.stockAvailable.set(50);
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 20 }));
    expect(component.overStock()).toBe(false);

    component.issueModel.update((m) => ({ ...m, qty: 99 }));
    expect(component.overStock()).toBe(true);
  });

  it('canSubmit wymaga poprawnego formularza + znanego stanu + braku niedoboru', () => {
    component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 5 }));
    expect(component.canSubmit()).toBe(false);      // stan jeszcze nieznany

    component.stockAvailable.set(50);
    expect(component.canSubmit()).toBe(true);       // stan znany i wystarczający

    component.issueModel.update((m) => ({ ...m, qty: 99 }));
    expect(component.canSubmit()).toBe(false);      // niedobór → blokada
  });
});
```

### Pułapki komponentów sygnałowych
1. **`Pick<Comp, 'x'>` nie działa na `protected`.** Kompilator: `TS2344 ... does not satisfy keyof GoodsIssue`. Zamiast tego zdefiniuj własny `type TestableX = { ... }` (jak wyżej) i zrób `as unknown as TestableX`.
2. **Typ `computed()` to `Signal<T>`, NIE `Computed<T>`.** `@angular/core` nie eksportuje typu `Computed` — użycie `import('@angular/core').Computed<...>` da `TS2694 ... has no exported member 'Computed'`. Używaj `Signal<boolean>`.
3. **`update()` vs `set()`.** Dla obiektów (model formularza) używaj `issueModel.update(m => ({...m, qty: 5}))`, żeby zachować pozostałe pola. `set()` podmienia cały obiekt.

---

## 3. Test HTML / SZABLONU — co widzi użytkownik

Ta warstwa renderuje komponent (`fixture.detectChanges()`) i sprawdza **DOM** — czy alert się pokazał, czy przycisk jest zablokowany, czy tekst się pojawił. To testy "z perspektywy użytkownika".

```ts
it('pokazuje alert niedoboru i blokuje submit gdy qty > stan', () => {
  component.stockAvailable.set(13);
  component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 20 }));
  fixture.detectChanges(); // ← renderuje szablon po zmianie sygnałów

  const alert = fixture.nativeElement.querySelector('.stock-error');
  expect(alert).toBeTruthy();
  expect(alert.textContent).toContain('Nicht genug Bestand!');

  const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type=submit]');
  expect(button.disabled).toBe(true); // atrybut disabled na <button>
});

it('ukrywa alert gdy qty mieści się w stanie', () => {
  component.stockAvailable.set(13);
  component.issueModel.update((m) => ({ ...m, sku: 'SKU-001', qty: 5 }));
  fixture.detectChanges();
  expect(fixture.nativeElement.querySelector('.stock-error')).toBeNull(); // brak elementu = ukryty
});
```

### Pułapki testów szablonu
1. **Zawsze `fixture.detectChanges()` po zmianie sygnału**, zanim sprawdzisz DOM — inaczej szablon nie odświeży się i zobaczysz stary stan.
2. **Selektory muszą zgadzać się z `.html`.** Używaj klas semantycznych (`.stock-error`, `.stock-state`) albo atrybutów (`button[type=submit]`), nie kolejności dzieci — kruche przy refaktoringu.
3. **`disabled` to atrybut, nie tekst.** `expect(button.disabled).toBe(true)` — nie `textContent`.

---

## 4. Testy zależności (DI) — SPY vs zwykła funkcja ⚠️

Gdy komponent zależy od serwisów (`AuthService`, `Router`) i chcesz sprawdzić, **czy metoda została wywołana**, mock musi być **spy'em** — nie zwykłą funkcją. To najczęstszy błąd (spotkany w `status.spec.ts`).

### Dlaczego zwykła funkcja nie działa
```ts
// ❌ ZWYKŁA strzałka niczego NIE zapisuje → .toHaveBeenCalled() rzuca:
//    "TypeError: [Function logout] is not a spy or a call to a spy!"
logout: () => { isLogoutCalled = true; },   // ręczny flag = workaround, nie test
navigate: (arg) => {},                        // pusta funkcja, zero historii wywołań
```

`vi.fn()` tworzy **spy** — funkcję, która *zapisuje każde wywołanie* (ile razy, z jakimi argumentami). Dopiero na spy możesz assertować.

### Poprawnie (`status.spec.ts`)
```ts
beforeEach(async () => {
  mockAuthService = {
    isAuthenticated: signal(true),
    getUsername: () => 'manager',   // zwykła funkcja OK — nie assertujemy na niej
    logout: vi.fn(),                // ← SPY (assertujemy, że wywołany)
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

it('onLogout wywołuje authService.logout() i router.navigate("/login")', () => {
  const fixture = TestBed.createComponent(Status);
  fixture.componentInstance.onLogout();

  expect(mockAuthService.logout).toHaveBeenCalledTimes(1);   // dokładnie raz
  expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']); // z tym argumentem
});
```

### Zasada do zapamiętania
| Co chcesz sprawdzić | Użyj w mocku |
|---|---|
| Tylko **zwrócić** wartość (np. `getUsername()`) | zwykła funkcja: `() => 'manager'` ✅ |
| **Sprawdzić, że metoda została wywołana** / z jakimi argami | **spy**: `vi.fn()` ✅ |

- `vi.fn()` — czysty spy (zwraca `undefined`, chyba że podasz implementację: `vi.fn(() => 'x')`).
- `vi.spyOn(realObj, 'method')` — spy na *istniejącej* metodzie realnego obiektu (zachowuje oryginalną logikę). Do mocków obiektowych standardem jest `vi.fn()`.

---

## 5. Podsumowanie warstw testów (co i kiedy)

| Warstwa | Co testuje | Narzędzie | Przykład w repo |
|---|---|---|---|
| **Serwis** | logika HTTP, parsowanie odpowiedzi | `HttpTestingController` + `firstValueFrom` | `logistics-service.examples.spec.ts` |
| **Komponent (logika)** | sygnały / `computed`, reguły biznesowe | typed cast na sygnały, bez DOM | `goods-issue.examples.spec.ts` (sekcja logiki) |
| **Komponent (HTML)** | renderowanie szablonu, alerty, disabled | `fixture.detectChanges()` + `querySelector` | `goods-issue.examples.spec.ts` (sekcja HTML) |
| **Zależności DI** | czy komponent wywołuje serwis/router | `vi.fn()` spy + `useValue` provider | `status.spec.ts` |

> Standard IHK: **controller = walidacja syntaktyczna, service = semantyczna (logika biznesowa).**
> W frontendzie analogicznie: komponent = UI + orkiestracja, serwis = komunikacja z API. Testy serwisu sprawdzają *semantykę* (parsowanie, payload), testy komponenta — *syntaktykę* (czy UI reaguje poprawnie na stan).
