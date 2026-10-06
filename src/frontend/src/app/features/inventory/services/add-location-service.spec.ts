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
    // locationResource (httpResource) wysyła GET /api/locations ASYNCHRONICZNIE
    // (efekty Angulara — nie w momencie inject, więc nie da się go flushować
    // synchronicznie w beforeEach). Drenujemy tu WSZYSTKIE pozostałe requesty,
    // zanim verify() sprawdzi czy kolejka jest pusta.
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
    // Realistyczna odpowiedź backendu (LocationController.java:79) — plain text!
    const expectedResponse = `Lagerplatz ${mockLocationCode} wurde erfolgreich angelegt.`;

    // KROK 1: wywołaj metodę serwisu → request trafia do kolejki testing backendu.
    // firstValueFrom subskrybuje natychmiast, więc request idzie TERAZ (nie po expectOne).
    const resultPromise = firstValueFrom(service.createLocation(mockLocationCode));

    // KROK 2: "odbierz" request z kolejki i zwaliduj go.
    const req = httpMock.expectOne('/api/locations/create');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ locationCode: mockLocationCode });

    // KROK 3: symuluj odpowiedź backendu → Observable emituje, await się kończy.
    req.flush(expectedResponse);

    expect(await resultPromise).toBe(expectedResponse);
  });
});
