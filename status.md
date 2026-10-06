# STATUS — smart-inventory-system (stan na 2026-10-06)

## 🎯 Bieżąca faza: Dashboard — ✅ ZAKOŃCZONE I ZWERYFIKOWANE (commit `84a6efa`)
Read-only dashboard z KPI i tabelą stanów. Działa end-to-end (zweryfikowane w przeglądarce 2026-10-06).

### Co zrobione i zweryfikowane
- **Backend** (`/api/dashboard`, role WORKER/MANAGER/ADMIN):
  - `StockSummaryItem` DTO: `{sku, name, category, totalQuantity, minStockLevel, lowStock}`.
  - `DashboardService`: agreguje stan per produkt (suma batchy), liczy low-stock (`total < minStockLevel`, default 10 gdy NULL).
  - `DashboardController`: `GET /stock-overview` (lista), `GET /location-count`. Cienki controller — logika w serwisie (standard IHK).
- **Frontend** (`features/inventory/components/dashboard/` + `services/dashboard-service.ts`):
  - `httpResource<StockSummaryItem[]>` (Angular 22 signals) — auto-reactive, `.reload()` przy błędzie.
  - KPI cards: Produkty / Gesamtbestand / Lagerplätze / Low-Stock-Warnungen; tabela sortowana low-stock-first + badge OK/Low Stock.
  - Route `/dashboard` + link w `nav.html`.
- **Weryfikacja (empiryczna):**
  - `ng build` ✅, pełny zestaw testów frontendu: **14/14 passed** (12 plików).
  - E2E: KPI = 5 produktów / 23 szt. / 4 lokalizacje / 3 low-stock — spójne z danymi backendu; tabela renderuje poprawnie, sortowanie działa.

### ⚠️ PITFALL (nowy): serwis bez `@Service()` → NG0201
Klasa serwisowa z `httpResource` **bez dekoratora** nie jest rejestrowana w DI — komponent dostaje `NG0201: No provider found for DashboardService`. Objaw: pusta strona + błąd w konsoli, build przechodzi (błąd runtime). Fix: `@Service()` z `@angular/core` (spójne z `InventoryService`).

### ⏭️ Następne kroki
1. ✅ **Nawigacja w UI** — komponent `Nav` (`shared/components/nav`) + link Dashboard; aktywna pozycja podświetlona, menu ukryte na `/login`. Zrobione 2026-10-05/06.
2. Przenieść logikę biznesową Location z controllera do `LocationService` (standard IHK) — odłożone.
3. Faza 5: Docker + CI/CD (GitHub Actions). Potem Phase 6: Multi-Warehouse / Audit Logging / Swagger.

### Testy frontendu — stan po poprawkach użytkownika (2026-10-06)
Użytkownik skonfigurował **Vitest coverage** (`vitest.config.ts` + `test-setup.ts`, `@vitest/coverage-v8`) i naprawił 3 wcześniej padające specy (app/login/auth-service — localStorage). Obecnie: **14/14 passed**, `ng test frontend --watch=false --coverage` działa.

---

## ✅ Poprzednia faza: Logistyka — przyjęcie + wydanie towaru — ZAKOŃCZONE I ZWERYFIKOWANE (2026-10-04)
Oba formularze działają end-to-end (przyjęcie 2026-10-04 rano, wydanie po południu).

### Co zrobione i zweryfikowane
- **Backend** — endpointy istniały (`LogisticsController`): `POST /api/logistics/receive`, `/issue` (FIFO), `GET /stock/{sku}`. Rola: klasowy `@PreAuthorize("hasRole('MANAGER')")`.
  - Kontrakt receive: body `{sku, loc, qty}` — **`loc` to KOD lokalizacji (String), nie ID** (`ReceiveRequest.java`). Sukces = **201 + plain text**. Błędy = 400 JSON z `message` (GlobalExceptionHandler).
  - Kontrakt issue: body `{sku, qty}` (`IssueRequest.java`) — brak lokalizacji, backend sam wybiera batchy wg FIFO.
- **Frontend** — dwa komponenty w `features/inventory/components/`:
  - `GoodsReceipt` na `/logistics/receive`: SKU + dropdown lokalizacji (wartość = `locationCode`) + ilość.
  - `GoodsIssue` na `/logistics/issue`: SKU + ilość, **reaktywny podgląd stanu** (`GET /stock/{sku}`, debounce 400 ms) i **alert braku towaru**: `qty > stan` → czerwony komunikat „Nicht genug Bestand!" + submit zablokowany (decyzja v1: all-or-nothing).
  - Wspólny `LogisticsService`: `receiveGoods()`, `issueGoods()` (`responseType:'text'`), `getStock(sku)` — parsuje liczbę regexem z plain text `"Gesamtbestand fuer X : N Stueck"`.
- **Weryfikacja (empiryczna):**
  - `ng build` ✅, testy jednostkowe goods-receipt + goods-issue ✅.
  - E2E przyjęcie: submit `RES-10K-OHM-001`/`A-1-01`/50 → zielony komunikat; backend: stan = **50 Stueck** (było 0).
  - E2E wydanie: podgląd „Verfuegbar: 50 Stueck"; qty=99 → alert + submit disabled; qty=20 → sukces, formularz wyczyszczony; backend: stan = **30 Stueck** (FIFO ✅).
  - Error path przez UI: nieznany SKU → czerwone „Product mit sku: NOPE-999 nicht gefunden!" (przyjęcie) / „Product sku: NOPE-999 nicht gefunden!" (podgląd wydania).

### ⏭️ Następne kroki (do wyboru z użytkownikiem)
1. ✅ **Nawigacja w UI** — komponent `Nav` (`shared/components/nav`) z linkami do `/products`, `/locations`, `/logistics/receive`, `/logistics/issue`; aktywna pozycja podświetlona (`routerLinkActive`), menu ukryte na `/login`. Zrobione 2026-10-05.
2. Przenieść logikę biznesową Location z controllera do `LocationService` (standard IHK, semantyczna walidacja w service) — odłożone od fazy CRUD.
3. Kolejna encja CRUD (Supplier — wciąż Pending w milestone'ach) / Audit Logging (NFR3) / Swagger-OpenAPI.

### Decyzje domenowe (v1, udokumentowane w requirements.md §5 A1–A4 + roadmap Phase 6)
- Pojedynczy magazyn — brak encji Warehouse; `locationCode` globalnie unikalny.
- Brak miejsca → manager zakłada nową lokalizację (`/locations`) i przyjmuje tam resztę (Location nie ma pojemności).
- Wydanie = all-or-nothing; częściowe („wydano 100 z 190") = roadmapa Phase 6.

---

## ✅ Poprzednia faza: pełny CRUD dla Location — ZAKOŃCZONY (2026-10-03)

### Plan — status
1. ✅ Backend `GET /api/locations` + testy
2. ✅ Frontend: tabela lokalizacji pod formularzem na `/locations`, odświeżana po create (`httpResource.reload()`)
3. ✅ Backend `PUT /api/locations/{id}` (update kodu, check unikalności) + testy
4. ✅ Frontend: edycja kodu w tabeli (inline edit + save/cancel)
5. ✅ Backend `DELETE /api/locations/{id}` z guardem StockBatch + testy
6. ✅ Frontend: delete button (z `window.confirm`)

**Weryfikacja:** `LocationControllerTest` = **21/21 testów przechodzi** (`mvn.cmd surefire:test -Dtest=LocationControllerTest`). UI działa end-to-end (potwierdzone przez użytkownika).

### Reguła biznesowa dla DELETE (do IHK)
`StockBatch` ma `@ManyToOne @JoinColumn(name="location_id", nullable=false)` → Location.
**Jeśli na lokalizacji leży towar (`stockBatchRepository.countByLocation(loc) > 0`) → delete odrzuć:**
`"Lagerplatz ID X kann nicht geloescht werden, da dort noch N StockBatch(es) liegen!"`
Semantyczna walidacja domenowa — dokładnie to, o co IHK pyta.

### ⚠️ Zalecenie architektoniczne (NIE zrobione, na później)
Logika biznesowa (check istnienia, unikalności, guard) siedzi w `LocationController`. Standard IHK:
controller = walidacja syntaktyczna, **service = semantyczna**. Przenieść do `LocationService` przy okazji kolejnego sprintu.

---

## ✅ Co jest ZROBIONE i zweryfikowane (Location CRUD)

### Backend — `LocationController.java` (`/api/locations`)
- Klasowy `@PreAuthorize("hasRole('MANAGER')")`; odczyt nadpisany na `WORKER/MANAGER/ADMIN`.
- `GET /` → `List<Location>` (id + locationCode).
- `POST /create`, body `{locationCode}` → **plain text** 200 (`"Lagerplatz X wurde erfolgreich angelegt."`) lub 400 JSON przez GlobalExceptionHandler. Check unikalności.
- `PUT /{id}`, body `{locationCode}` → plain text; check istnienia + check czy nowy kod nie jest zajęty przez INNY id.
- `DELETE /{id}` → plain text; guard StockBatch (patrz wyżej).
- DTO: `LocationCreateRequest`, `LocationUpdateRequest` (`@Builder`+`@NoArgsConstructor`).

### Frontend — route `/locations` (komponent `AddLocation`)
- **List:** `httpResource<Location[]>(() => '/api/locations')` w `AddLocationService` → auto-reactive, `.reload()` po każdej mutacji.
- **Create:** form signal (`form(this.locationModel, f => required(f.locationCode))`), przycisk disabled przy invalid, komunikat z backendu, `reset({locationCode:""})`.
- **Update:** inline edit w tabeli (signals `editingId`, `editCode`), save/cancel.
- **Delete:** button + `window.confirm`.
- Error handlers parsują JSON z GlobalExceptionHandler (`body.message`) — bo przy `responseType:'text'` body błędu też jest surowym stringiem.

---

## ⚠️ PITFALLS (zweryfikowane empirycznie — NIE pomijać)

### 1. Spring plain text vs Angular JSON.parse (NAJWAŻNIEJSZY)
Backend zwraca `ResponseEntity<String>` = **plain text**. Angular HttpClient domyślnie (`responseType: 'json'`)
robi `JSON.parse()` → SyntaxError → **zamienia 200 OK w event błędu** (w `_module-chunk.mjs`: catch → `ok=false, body={error, text}`). Objaw: `next()` nigdy się nie wywołuje.
**Fix:** `{ responseType: 'text' }` w opcjach requesta (create/update/delete). Uwaga: przy `'text'` body błędu (400) też jest surowym stringiem → handler musi sam `JSON.parse`-ować i czytać `.message`.

### 2. Angular HttpClient DELETE + responseType:'text'
Overload z `responseType:'text'` jest **nie-genericzny** — jawny typ `<string>` eliminuje go z dopasowania (TS2769).
**Fix:** `this.http.delete(url, { responseType: 'text' })` BEZ jawnego genericu; metoda deklaruje `Observable<string>`.

### 3. Maven pod MSYS/Windows
Bashowy `mvn` **nie startuje** (`ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`) —
MSYS path conversion łamie glob `boot/plexus-classworlds-*.jar`.
**Fix:** używaj `mvn.cmd`: `/c/Programs/apache-maven-3.9.9/bin/mvn.cmd test -Dtest=LocationControllerTest`

### 4. Angular CLI pod tym środowiskiem
Własny Node Hermesa shadowuje nvm; yarn/corepack zepsute (MSYS path bug).
**Fix:** `node ./node_modules/@angular/cli/bin/ng.js <cmd>` (build/test).

### 5. ng serve nie hot-reloaduje proxy.conf.json / angular.json — restart po zmianie.
HMR resetuje stan aplikacji → po HMR trzeba się zalogować ponownie.

### 6. Testy frontendu: 3 pre-existing failures (NIE nasze)
`app.spec.ts`, `login.spec.ts`, `auth-service.spec.ts` padają (localStorage undefined w teście).
Potwierdzone przez `git stash`: padają też bez naszych zmian. Testy add-location przechodzą.

---

## 🛠️ Środowisko / komendy

| Co | Komenda |
|---|---|
| Backend testy (Location) | `cd src/backend && mvn.cmd surefire:test -Dtest=LocationControllerTest` |
| Backend restart | `cd src/backend && ./run-backend.sh restart` (background, czekać na port 8888) |
| Frontend build | `cd src/frontend && node ./node_modules/@angular/cli/bin/ng.js build` |
| Frontend testy | `... ng.js test --watch=false` |

- Backend: Spring Boot, port **8888**, SQLite `sisie_database.db`, package `com.sisie.core`.
- Frontend: Angular 22.1.7 (signal forms!), proxy `/api` → 8888.
- Użytkownicy: admin/Admin123!, manager/Manager123!, worker/Worker123!
- Token w localStorage: `auth_token`, `auth_username`.
- Konwencja testów backendu: `@WebMvcTest(XController.class)`, `@AutoConfigureMockMvc(addFilters=true)`,
  `@EnableMethodSecurity`, `@WithMockUser(authorities="ROLE_...")`, `@MockitoBean` repozytorium,
  asercje przez `jsonPath("$.message").value(...)`.

## 📁 Struktura plików (Location)
```
src/backend/src/main/java/com/sisie/core/
├── controller/LocationController.java      # GET /, POST /create, PUT /{id}, DELETE /{id} (+ test endpoint)
├── dto/{LocationCreateRequest,LocationUpdateRequest}.java   # {locationCode}, @Builder+@NoArgsConstructor
├── model/Location.java                     # id (IDENTITY), locationCode (unique, not null)
└── repository/LocationRepository.java      # JpaRepository + findByLocationCode
src/backend/src/test/java/com/sisie/core/controller/LocationControllerTest.java  # 21 testów ✅
src/frontend/src/app/features/inventory/
├── components/add-location/{add-location.ts,.html,.scss,.spec.ts}   # route /locations (CRUD UI)
├── components/goods-receipt/{goods-receipt.ts,.html,.scss,.spec.ts} # route /logistics/receive (Wareneingang) ✅ 2026-10-04
├── components/goods-issue/{goods-issue.ts,.html,.scss,.spec.ts}     # route /logistics/issue (Warenausgang, FIFO + alert braku towaru) ✅ 2026-10-04
└── services/
    ├── add-location-service{.ts,.spec.ts}                           # locationResource + create/update/delete
    └── logistics-service.ts                                         # receiveGoods() + issueGoods() + getStock() + locationResource
src/frontend/src/app/shared/components/nav/{nav.ts,.html,.scss,.spec.ts}  # menu główne (routerLinkActive), ukryte na /login ✅ 2026-10-05
```

## 🗄️ Baza (SQLite) — lokalizacje obecne
id=1 A-1-01, id=2 A-9-99, id=3 A-9-98 (A-9-9x to artefakty testów — użytkownik: zostawić).

## ⏭️ Następny krok (poza Location)
Opcje do wyboru z użytkownikiem:
1. Przenieść logikę biznesową Location z controllera do `LocationService` (standard IHK, semantyczna walidacja w service).
2. Kolejna encja CRUD (np. Supplier — wciąż Pending w milestone'ach).
3. Audit Logging (NFR3) / Swagger-OpenAPI.

---
---

# Historia projektu (archiwum z 20.09)

## 🚀 Milestone'y ukończone
- [x] **Phase 1: Analysis** -> `docs/01_analysis/requirements.md` finalized.
- [x] **Project Structure** -> Professional layered architecture (`controller`, `service`, `repository`, `model`, `dto`).
- [x] **Core Logistics Logic** -> FIFO (First-In-First-Out) implementation for goods receipt and issue.
- [ ] **Supplier Management** -> Implementation of `Supplier` and `ProductSupplier` entities (Pending).
- [x] **Persistence Fix** -> Fixed bug in `InventoryService` where stock batches were not being saved to the DB.
- [x] **Stock Tracking** -> Implemented `GET /api/logistics/stock/{sku}` using `@PathVariable`.
- [x] **API Testing Suite** -> Created and versioned `.rest` files for executable documentation.
- [x] **Sprint 3: Security & Access Management** -> User Entity with Roles, JWT (JJWT), Stateless Session, RBAC via `@PreAuthorize`, BCrypt.
- [>] **Sprint 4: Quality Assurance & Testing** -> MVC Slice Tests (`LogisticsController`), Security Constraints verification, Mockito for service decoupling.

## 🧠 Knowledge Base (mental model)
Model → DB tables; Repository → exclusive bridge to DB; Controller → API entry points (ResponseEntity + DTOs); Service → business logic (FIFO, Auth); DTO → decouples internal models from API contracts; JWT → stateless auth; RBAC → role-based access; MVC Slice Testing → `@WebMvcTest` + MockMvc.

## 🛠️ Technical Stack
- **Backend**: Java 21 / Spring Boot 3+ (w praktyce 4.1+)
- **Security**: Spring Security, JJWT (io.jsonwebtoken)
- **Database**: SQLite (Local Dev)
- **Testing**: JUnit 5, Mockito, JaCoCo, Pitest, IntelliJ/VS Code REST Client (`.rest` files)
- **Build Tool**: Maven

## 📌 Quick Start for Next Session
1. Run the application (`./run-backend.sh restart`).
2. Login via `POST /api/auth/login` to get a JWT Token.
3. Use the token in the Header (`Authorization: Bearer *** to access protected endpoints like `/api/logistics/stock/{sku}`.
