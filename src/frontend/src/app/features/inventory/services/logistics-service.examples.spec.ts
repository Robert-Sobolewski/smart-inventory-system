import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { LogisticsService } from './logistics-service';

describe('LogisticsService (examples)', () => {
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
    // fail if any request was never consumed — catches forgotten mocks
    httpMock.verify();
  });

  it('getStock parses the quantity from plain text', async () => {
    const qtyPromise = firstValueFrom(service.getStock('RES-10K-OHM-001'));
    const req = httpMock.expectOne('/api/logistics/stock/RES-10K-OHM-001');
    expect(req.request.method).toBe('GET');
    req.flush('Gesamtbestand fuer RES-10K-OHM-001 : 50 Stueck');
    expect(await qtyPromise).toBe(50);
  });

  it('getStock rejects on unexpected response format', async () => {
    const p = firstValueFrom(service.getStock('NOPE'));
    httpMock.expectOne('/api/logistics/stock/NOPE').flush('no number here');
    await expect(p).rejects.toThrow(/Unexpected stock response/);
  });

  it('issueGoods posts the payload to /issue', async () => {
    const msgPromise = firstValueFrom(service.issueGoods({ sku: 'SKU-001', qty: 5 }));
    const req = httpMock.expectOne('/api/logistics/issue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ sku: 'SKU-001', qty: 5 });
    req.flush('Warenausgang (FIFO) fuer SKU: SKU-001 erfolgreich durchgefuert!');
    expect(await msgPromise).toContain('erfolgreich');
  });
});
