import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AddLocationService } from './add-location-service';

describe('AddLocationService', () => {
  let service: AddLocationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AddLocationService);
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

  it('should call createLocation and return success string', async () => {
    const mockLocationCode = 'LOC-123';
    // A realistic backend response (LocationController.java:79) — plain text!
    const expectedResponse = `Lagerplatz ${mockLocationCode} wurde erfolgreich angelegt.`;

    // STEP 1: call the service method → the request lands in the testing backend queue.
    // firstValueFrom subscribes immediately, so the request goes out NOW (not after expectOne).
    const resultPromise = firstValueFrom(service.createLocation(mockLocationCode));

    // STEP 2: "receive" the request from the queue and validate it.
    const req = httpMock.expectOne('/api/locations/create');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ locationCode: mockLocationCode });

    // STEP 3: simulate the backend response → the Observable emits, the await completes.
    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });

  it('should call updateLocation and return success string', async () => {
    const locationId = 1;
    const updatedLocationCode = 'A-1-013';
    const expectResponse = `Lagerplatz ID ${locationId} + wurde erfolgreich aktualisiert.`;
    const resultPromise = firstValueFrom(service.updateLocation(locationId, updatedLocationCode));

    const req = httpMock.expectOne(`/api/locations/${locationId}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ locationCode: updatedLocationCode });
    req.flush(expectResponse);
    expect(await resultPromise).toBe(expectResponse);
  });

  it('should call deleteLocation and return success string', async () => {
    const locationId = 1;
    const expectResponse = `Lagerplatz ID ${locationId} wurde erfolgreich geloescht.`;
    const resultPromise = firstValueFrom(service.deleteLocation(locationId));
    const req = httpMock.expectOne(`/api/locations/${locationId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(expectResponse);
    expect(await resultPromise).toBe(expectResponse);
  });
});
