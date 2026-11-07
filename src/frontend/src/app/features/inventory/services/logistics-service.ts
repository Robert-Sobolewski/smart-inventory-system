import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Location } from '../models/location';

// Payload for POST /api/logistics/receive — mirrors the backend ReceiveRequest DTO.
// NOTE: `loc` is the location CODE (String), not its ID — that's how ReceiveRequest.java defines it.
export interface ReceiveRequestPayload {
  sku: string;
  loc: string;
  qty: number;
}

// Payload for POST /api/logistics/issue — mirrors the backend IssueRequest DTO.
// No location given: the backend picks batches itself per FIFO (oldest first).
export interface IssueRequestPayload {
  sku: string;
  qty: number;
}

@Service()
export class LogisticsService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/logistics';

  // Location list for the dropdown in the goods-receipt form (same endpoint as AddLocationService)
  readonly locationResource = httpResource<Location[]>(() => '/api/locations');

  /**
   * Books a goods receipt (POST /api/logistics/receive).
   * The backend returns ResponseEntity<String> = plain text, not JSON.
   * The default responseType 'json' would run JSON.parse() on the text and turn
   * the 201 Created into an error (SyntaxError) — hence responseType: 'text'.
   */
  receiveGoods(payload: ReceiveRequestPayload): Observable<string> {
    return this.http.post<string>(`${this.API_URL}/receive`, payload, { responseType: 'text' });
  }

  /**
   * Books a goods issue (POST /api/logistics/issue) — FIFO on the backend side.
   * Plain text like receive -> responseType: 'text'.
   */
  issueGoods(payload: IssueRequestPayload): Observable<string> {
    return this.http.post<string>(`${this.API_URL}/issue`, payload, { responseType: 'text' });
  }

  /**
   * Total stock for a SKU (GET /api/logistics/stock/{sku}).
   * The backend returns plain text: "Gesamtbestand fuer X : N Stueck" — we parse the number with a regex.
   */
  getStock(sku: string): Observable<number> {
    // WITHOUT an explicit <string>: the overload with responseType 'text' is non-generic (TS2769, see pitfall #2)
    return this.http.get(`${this.API_URL}/stock/${encodeURIComponent(sku)}`, { responseType: 'text' })
      .pipe(
        map((text: string) => {
          const m = text.match(/:\s*(\d+)\s*Stueck/);
          if (!m) {
            throw new Error(`Unexpected stock response: ${text}`);
          }
          return Number(m[1]);
        }),
      );
  }

  constructor() {}
}
