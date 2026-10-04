import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Location } from '../models/location';

// Payload for POST /api/logistics/receive — mirrors the backend ReceiveRequest DTO.
// UWAGA: `loc` to KOD lokalizacji (String), nie jej ID — tak definiuje to ReceiveRequest.java.
export interface ReceiveRequestPayload {
  sku: string;
  loc: string;
  qty: number;
}

// Payload for POST /api/logistics/issue — mirrors the backend IssueRequest DTO.
// Brak lokalizacji: backend sam wybiera batchy wg FIFO (najstarsze najpierw).
export interface IssueRequestPayload {
  sku: string;
  qty: number;
}

@Service()
export class LogisticsService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/logistics';

  // Lista lokalizacji do dropdownu w formularzu przyjęcia (ten sam endpoint co AddLocationService)
  readonly locationResource = httpResource<Location[]>(() => '/api/locations');

  /**
   * Bucht Wareneingang ein (POST /api/logistics/receive).
   * Backend zwraca ResponseEntity<String> = plain text, nie JSON.
   * Domyślny responseType 'json' zrobiłby JSON.parse() na tekście i zamienił
   * 201 Created na błąd (SyntaxError) — stąd responseType: 'text'.
   */
  receiveGoods(payload: ReceiveRequestPayload): Observable<string> {
    return this.http.post<string>(`${this.API_URL}/receive`, payload, { responseType: 'text' });
  }

  /**
   * Bucht Warenausgang aus (POST /api/logistics/issue) — FIFO po stronie backendu.
   * Plain text jak receive -> responseType: 'text'.
   */
  issueGoods(payload: IssueRequestPayload): Observable<string> {
    return this.http.post<string>(`${this.API_URL}/issue`, payload, { responseType: 'text' });
  }

  /**
   * Gesamtbestand dla SKU (GET /api/logistics/stock/{sku}).
   * Backend zwraca plain text: "Gesamtbestand fuer X : N Stueck" — parsujemy liczbę regexem.
   */
  getStock(sku: string): Observable<number> {
    // BEZ jawnego <string>: overload z responseType 'text' jest nie-genericzny (TS2769, patrz pitfall #2)
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
