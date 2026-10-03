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
    // Backend zwraca ResponseEntity<String> = plain text, nie JSON.
    // Domyślny responseType 'json' robi JSON.parse() i zamienia 200 OK na błąd (SyntaxError),
    // dlatego next() nigdy się nie wywoływał. Stąd responseType: 'text'.
    return this.http.post<string>(`${this.API_URL}/create`, { locationCode }, { responseType: 'text' });
  }

  updateLocation(id: number, locationCode: string): Observable<string> {
    // PUT /api/locations/{id} -> ResponseEntity<String>, stąd też responseType: 'text'
    return this.http.put<string>(`${this.API_URL}/${id}`, { locationCode }, { responseType: 'text' });
  }

  deleteLocation(id: number): Observable<string> {
    // DELETE /api/locations/{id} -> ResponseEntity<String>, stąd też responseType: 'text'.
    // UWAGA: bez jawnego <string>! Overload z responseType:'text' jest nie-genericzny,
    // a jawny typ eliminuje go z dopasowania (TS2769). Zwraca Observable<string>.
    return this.http.delete(`${this.API_URL}/${id}`, { responseType: 'text' });
  }

  constructor() {}
}
