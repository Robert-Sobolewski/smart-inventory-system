import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { Location } from '../models/location';

@Service()
export class AddLocationService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/locations';

  // Reactive resource that fetches the location list (auto-tracked by Angular)
  readonly locationResource = httpResource<Location[]>(() => this.API_URL);

  createLocation(locationCode: string): Observable<string> {
    // The backend returns ResponseEntity<String> = plain text, not JSON.
    // The default responseType 'json' does a JSON.parse() and turns the 200 OK into an error (SyntaxError),
    // which is why next() was never called. Hence responseType: 'text'.
    return this.http.post<string>(`${this.API_URL}/create`, { locationCode }, { responseType: 'text' });
  }

  updateLocation(id: number, locationCode: string): Observable<string> {
    // PUT /api/locations/{id} -> ResponseEntity<String>, hence responseType: 'text' as well
    return this.http.put<string>(`${this.API_URL}/${id}`, { locationCode }, { responseType: 'text' });
  }

  deleteLocation(id: number): Observable<string> {
    // DELETE /api/locations/{id} -> ResponseEntity<String>, hence responseType: 'text' as well.
    // NOTE: without an explicit <string>! The overload with responseType:'text' is non-generic,
    // and an explicit type eliminates it from the matching (TS2769). Returns Observable<string>.
    return this.http.delete(`${this.API_URL}/${id}`, { responseType: 'text' });
  }

  constructor() {}
}
