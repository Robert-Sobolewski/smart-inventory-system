import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
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
    // locationResource (httpResource) sends GET /api/locations ASYNCHRONOUSLY
    // (Angular effects — not at inject time, so it cannot be flushed
    // synchronously in beforeEach). We drain ALL remaining requests here,
    // before verify() checks that the queue is empty.
    for (const req of httpMock.match('/api/locations')) {
      req.flush([]);
    }
    // fail if any OTHER request was never consumed
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize locationResource', () => {
    expect(service.locationResource).toBeDefined();
  });

  it('should call receiveGoods and return the plain-text success message', async () => {
    // `loc` is the location CODE (String), not its ID — mirrors ReceiveRequest.java.
    const payload = { sku: 'SKU-001', loc: 'A-1-01', qty: 3 };
    // A realistic backend response (LogisticsController.java) — plain text!
    const expectedResponse = `Wareneingang gebucht: 3 Stueck von SKU-001 nach A-1-01`;

    // STEP 1: call the service method → the request lands in the testing backend queue.
    // firstValueFrom subscribes immediately, so the request goes out NOW (not after expectOne).
    const resultPromise = firstValueFrom(service.receiveGoods(payload));

    // STEP 2: "receive" the request from the queue and validate it.
    const req = httpMock.expectOne('/api/logistics/receive');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    // STEP 3: simulate the backend response → the Observable emits, the await completes.
    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });

  it('should call issueGoods and return the plain-text success message', async () => {
    // No location in the payload — FIFO is resolved on the backend side.
    const payload = { sku: 'SKU-001', qty: 2 };
    // A realistic backend response (LogisticsController.java) — plain text!
    const expectedResponse = `Warenausgang gebucht: 2 Stueck von SKU-001`;

    const resultPromise = firstValueFrom(service.issueGoods(payload));

    const req = httpMock.expectOne('/api/logistics/issue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });

  it('should call getStock and parse the number out of the plain-text response', async () => {
    // The backend returns: "Gesamtbestand fuer X : N Stueck" — we only need N.
    const expectedResponse = `Gesamtbestand fuer SKU-001 : 42 Stueck`;

    const resultPromise = firstValueFrom(service.getStock('SKU-001'));

    const req = httpMock.expectOne('/api/logistics/stock/SKU-001');
    expect(req.request.method).toBe('GET');

    req.flush(expectedResponse);

    expect(await resultPromise).toBe(42);
  });

  it('should URL-encode the SKU in the getStock request', async () => {
    const resultPromise = firstValueFrom(service.getStock('RES 10K OHM'));

    // encodeURIComponent turns spaces into %20 — expectOne must match the encoded URL.
    const req = httpMock.expectOne('/api/logistics/stock/RES%2010K%20OHM');
    expect(req.request.method).toBe('GET');

    req.flush(`Gesamtbestand fuer RES 10K OHM : 7 Stueck`);

    expect(await resultPromise).toBe(7);
  });

  it('should reject getStock when the response does not match the expected format', async () => {
    const unexpectedResponse = `Fehler: SKU unbekannt`;

    // firstValueFrom subscribes immediately → the request goes out NOW.
    const resultPromise = firstValueFrom(service.getStock('SKU-404'));

    const req = httpMock.expectOne('/api/logistics/stock/SKU-404');
    expect(req.request.method).toBe('GET');

    // The map() operator throws → firstValueFrom turns it into a rejected promise.
    req.flush(unexpectedResponse);

    await expect(resultPromise).rejects.toThrow(
      `Unexpected stock response: ${unexpectedResponse}`,
    );
  });
});
