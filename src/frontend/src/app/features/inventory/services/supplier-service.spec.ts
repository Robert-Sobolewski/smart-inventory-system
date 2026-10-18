import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { SupplierService } from './supplier-service';

describe('SupplierService', () => {
  let service: SupplierService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SupplierService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // supplierResource (httpResource) wysyła GET /api/suppliers ASYNCHRONICZNIE
    // (efekty Angulara — nie w momencie inject, więc nie da się go flushować
    // synchronicznie w beforeEach). Drenujemy tu WSZYSTKIE pozostałe requesty,
    // zanim verify() sprawdzi czy kolejka jest pusta.
    for (const req of httpMock.match('/api/suppliers')) {
      req.flush([]);
    }
    // fail if any OTHER request was never consumed
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize supplierResource', () => {
    expect(service.supplierResource).toBeDefined();
  });

  it('should call createSupplier and return success string', async () => {
    const payload = { name: 'ACME GmbH', email: 'sales@acme.de' };
    // Realistyczna odpowiedź backendu (SupplierController.java) — plain text!
    const expectedResponse = `Lieferant ACME GmbH erfolgreich angelegt (ID: 1)`;

    // KROK 1: wywołaj metodę serwisu → request trafia do kolejki testing backendu.
    // firstValueFrom subskrybuje natychmiast, więc request idzie TERAZ (nie po expectOne).
    const resultPromise = firstValueFrom(service.createSupplier(payload));

    // KROK 2: "odbierz" request z kolejki i zwaliduj go.
    // UWAGA: predicate matcher — POST /api/suppliers ma TEN SAM URL co GET listy
    // (supplierResource), więc expectOne('/api/suppliers') znalazłby dwa requesty.
    // Matcher dostaje HttpRequest (nie TestRequest) -> właściwości .method/.url bezpośrednio.
    const req = httpMock.expectOne(
      (r) => r.method === 'POST' && r.url === '/api/suppliers',
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    // KROK 3: symuluj odpowiedź backendu → Observable emituje, await się kończy.
    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });

  it('should call updateSupplier and return the updated supplier object', async () => {
    const supplierId = 1;
    // Backend update() wymaga non-null email -> UI zawsze wysyła wszystkie trzy pola
    const payload = { name: 'ACME AG', email: 'sales@acme.de', address: 'Musterstr. 1' };
    // PUT /api/suppliers/{id} zwraca obiekt Supplier (JSON), nie plain text!
    const expectedResponse = { id: supplierId, ...payload };

    const resultPromise = firstValueFrom(service.updateSupplier(supplierId, payload));

    const req = httpMock.expectOne(`/api/suppliers/${supplierId}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);

    req.flush(expectedResponse);

    expect(await resultPromise).toEqual(expectedResponse);
  });

  it('should call deleteSupplier and return success string', async () => {
    const supplierId = 1;
    // Realistyczna odpowiedź backendu (SupplierController.java) — plain text!
    const expectedResponse = `Lieferant mit ID ${supplierId} geloescht`;

    const resultPromise = firstValueFrom(service.deleteSupplier(supplierId));

    const req = httpMock.expectOne(`/api/suppliers/${supplierId}`);
    expect(req.request.method).toBe('DELETE');

    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });
});
