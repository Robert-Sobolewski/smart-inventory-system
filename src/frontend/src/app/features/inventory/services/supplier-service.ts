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
    // The backend returns ResponseEntity<String> = plain text, not JSON.
    // The default responseType 'json' does a JSON.parse() and turns the 200 OK into an error (SyntaxError),
    // which is why next() was never called. Hence responseType: 'text'.
    return this.http.post<string>(this.API_URL, payload, { responseType: 'text' });
  }

  updateSupplier(id: number, payload: SupplierRequest): Observable<Supplier> {
    // PUT /api/suppliers/{id} returns the Supplier object (JSON), not plain text —
    // hence the default responseType 'json' and the <Supplier> generic.
    return this.http.put<Supplier>(`${this.API_URL}/${id}`, payload);
  }

  deleteSupplier(id: number): Observable<string> {
    // DELETE /api/suppliers/{id} -> ResponseEntity<String>, hence responseType: 'text' as well.
    // NOTE: without an explicit <string>! The overload with responseType:'text' is non-generic,
    // and an explicit type eliminates it from the matching (TS2769). Returns Observable<string>.
    return this.http.delete(`${this.API_URL}/${id}`, { responseType: 'text' });
  }

  constructor() {}
}
