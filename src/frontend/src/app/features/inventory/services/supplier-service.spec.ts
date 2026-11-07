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
    // supplierResource (httpResource) sends GET /api/suppliers ASYNCHRONOUSLY
    // (Angular effects — not at inject time, so it cannot be flushed
    // synchronously in beforeEach). We drain ALL remaining requests here,
    // before verify() checks that the queue is empty.
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
    // A realistic backend response (SupplierController.java) — plain text!
    const expectedResponse = `Lieferant ACME GmbH erfolgreich angelegt (ID: 1)`;

    // STEP 1: call the service method → the request lands in the testing backend queue.
    // firstValueFrom subscribes immediately, so the request goes out NOW (not after expectOne).
    const resultPromise = firstValueFrom(service.createSupplier(payload));

    // STEP 2: "receive" the request from the queue and validate it.
    // NOTE: predicate matcher — POST /api/suppliers has THE SAME URL as the list GET
    // (supplierResource), so a plain expectOne('/api/suppliers') would find two requests.
    // The matcher receives the HttpRequest (not TestRequest) -> .method/.url properties directly.
    const req = httpMock.expectOne(
      (r) => r.method === 'POST' && r.url === '/api/suppliers',
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    // STEP 3: simulate the backend response → the Observable emits, the await completes.
    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });

  it('should call updateSupplier and return the updated supplier object', async () => {
    const supplierId = 1;
    // The backend update() requires a non-null email -> the UI always sends all three fields
    const payload = { name: 'ACME AG', email: 'sales@acme.de', address: 'Musterstr. 1' };
    // PUT /api/suppliers/{id} returns the Supplier object (JSON), not plain text!
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
    // A realistic backend response (SupplierController.java) — plain text!
    const expectedResponse = `Lieferant mit ID ${supplierId} geloescht`;

    const resultPromise = firstValueFrom(service.deleteSupplier(supplierId));

    const req = httpMock.expectOne(`/api/suppliers/${supplierId}`);
    expect(req.request.method).toBe('DELETE');

    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });
});
