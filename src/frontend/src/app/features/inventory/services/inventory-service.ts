import { Service, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductCreateRequest } from '../models/product';

@Service()
export class InventoryService {

  private readonly http = inject(HttpClient);
  private readonly API_URL = "/api/products";

  // Reactive resource that fetches the product list (auto-tracked by Angular)
  readonly productResource = httpResource<Product[]>(() => this.API_URL);

  /**
   * Sends a new product to the backend (POST /api/products/create).
   * The server only allows the MANAGER role for this endpoint.
   */
  addProduct(payload: ProductCreateRequest): Observable<string> {
    return this.http.post<string>(`${this.API_URL}/create`, payload);
  }

  constructor() {}
}
