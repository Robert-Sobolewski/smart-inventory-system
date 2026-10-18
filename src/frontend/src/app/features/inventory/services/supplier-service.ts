import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { Supplier, SupplierRequest } from '../models/supplier';

@Service()
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/suppliers';

  // Reactive resource that fetches the supplier list (auto-tracked by Angular)
  readonly supplierResource = httpResource<Supplier[]>(() => this.API_URL);

  createSupplier(payload: SupplierRequest): Observable<string> {
    // Backend zwraca ResponseEntity<String> = plain text, nie JSON.
    // Domyślny responseType 'json' robi JSON.parse() i zamienia 200 OK na błąd (SyntaxError),
    // dlatego next() nigdy się nie wywoływał. Stąd responseType: 'text'.
    return this.http.post<string>(this.API_URL, payload, { responseType: 'text' });
  }

  updateSupplier(id: number, payload: SupplierRequest): Observable<Supplier> {
    // PUT /api/suppliers/{id} zwraca obiekt Supplier (JSON), nie plain text —
    // stąd domyślny responseType 'json' i generic <Supplier>.
    return this.http.put<Supplier>(`${this.API_URL}/${id}`, payload);
  }

  deleteSupplier(id: number): Observable<string> {
    // DELETE /api/suppliers/{id} -> ResponseEntity<String>, stąd też responseType: 'text'.
    // UWAGA: bez jawnego <string>! Overload z responseType:'text' jest nie-genericzny,
    // a jawny typ eliminuje go z dopasowania (TS2769). Zwraca Observable<string>.
    return this.http.delete(`${this.API_URL}/${id}`, { responseType: 'text' });
  }

  constructor() {}
}
