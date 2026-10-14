import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardService, StockSummaryItem } from './dashboard-service';

// httpResource fires its initial fetch via an effect (microtask) and updates state
// through a promise — wait one macrotask tick after construction / flush.
async function settle(): Promise<void> {
  await new Promise((r) => setTimeout(r, 0));
}

function row(overrides: Partial<StockSummaryItem> = {}): StockSummaryItem {
  return {
    sku: 'SKU-1',
    name: 'Produkt',
    category: null,
    totalQuantity: 0,
    minStockLevel: 5,
    lowStock: false,
    ...overrides,
  };
}

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // both resources auto-load on construction — drain any unconsumed requests
    for (const req of httpMock.match('/api/dashboard/stock-overview')) {
      req.flush([]);
    }
    for (const req of httpMock.match('/api/dashboard/location-count')) {
      req.flush(0);
    }
    httpMock.verify();
  });

  it('stockOverviewResource starts in loading state without a value', () => {
    const ref = service.stockOverviewResource;
    expect(ref.isLoading()).toBe(true);
    expect(ref.hasValue()).toBe(false);
    expect(ref.value()).toBeUndefined();
  });

  it('stockOverviewResource auto-fetches GET /api/dashboard/stock-overview and exposes the rows', async () => {
    const ref = service.stockOverviewResource;
    await settle(); // effect fires the request in a microtask
    const req = httpMock.expectOne('/api/dashboard/stock-overview');
    expect(req.request.method).toBe('GET');

    const data = [
      row({ sku: 'A-100', totalQuantity: 3 }),
      row({ sku: 'B-200', lowStock: true }),
    ];
    req.flush(data);
    await settle();

    // rows pass through untouched — no transformation on the service side
    expect(ref.hasValue()).toBe(true);
    expect(ref.value()).toEqual(data);
  });

  it('stockOverviewResource captures the backend error status on failure', async () => {
    const ref = service.stockOverviewResource;
    await settle();
    httpMock.expectOne('/api/dashboard/stock-overview').flush(null, {
      status: 500,
      statusText: 'Internal Server Error',
    });
    await settle();

    expect(ref.hasValue()).toBe(false);
    // Angular 22: resource.error() holds an HttpErrorResponse (plain object with status)
    const err = ref.error() as unknown as { status?: number };
    expect(err?.status).toBe(500);
  });

  it('stockOverviewResource reload() refetches and replaces the previous value', async () => {
    const ref = service.stockOverviewResource;
    await settle();
    httpMock.expectOne('/api/dashboard/stock-overview').flush([row({ sku: 'OLD' })]);
    await settle();
    expect(ref.value()?.map((r) => r.sku)).toEqual(['OLD']);

    // resolved -> true, the second request fires in a microtask
    expect(ref.reload()).toBe(true);
    await settle();
    httpMock.expectOne('/api/dashboard/stock-overview').flush([row({ sku: 'NEW' })]);
    await settle();
    expect(ref.value()?.map((r) => r.sku)).toEqual(['NEW']);
  });

  it('locationCountResource starts with defaultValue 0 before its fetch resolves', () => {
    const ref = service.locationCountResource;
    // defaultValue lets the UI render a counter without an empty state
    expect(ref.value()).toBe(0);
    expect(ref.isLoading()).toBe(true);
  });

  it('locationCountResource auto-fetches GET /api/dashboard/location-count and exposes the count', async () => {
    const ref = service.locationCountResource;
    await settle();
    // backend returns a plain JSON number (ResponseEntity<Integer>)
    httpMock.expectOne('/api/dashboard/location-count').flush(42);
    await settle();

    expect(ref.value()).toBe(42);
  });

  it('locationCountResource reload() refetches the count', async () => {
    const ref = service.locationCountResource;
    await settle();
    httpMock.expectOne('/api/dashboard/location-count').flush(10);
    await settle();
    expect(ref.value()).toBe(10);

    expect(ref.reload()).toBe(true);
    await settle();
    httpMock.expectOne('/api/dashboard/location-count').flush(7);
    await settle();
    expect(ref.value()).toBe(7);
  });
});
