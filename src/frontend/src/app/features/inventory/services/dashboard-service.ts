import { httpResource } from '@angular/common/http';
import { Service } from '@angular/core';

/**
 * Eine Zeile der Bestandsuebersicht (entspricht dem Backend-DTO StockSummaryItem).
 */
export interface StockSummaryItem {
  sku: string;
  name: string;
  category: string | null;
  totalQuantity: number;
  minStockLevel: number;
  lowStock: boolean;
}

/**
 * Read-only Datenquelle fuer das Lager-Dashboard.
 * httpResource = reaktive Ressource, automatisch von Angular nachverfolgt (Signals).
 */
@Service()
export class DashboardService {
  private readonly API_URL = '/api/dashboard';

  /** Bestandsuebersicht: eine Zeile pro Produkt mit Low-Stock-Markierung. */
  readonly stockOverviewResource = httpResource<StockSummaryItem[]>(
    () => `${this.API_URL}/stock-overview`,
  );

  /** Anzahl der Lagerplaetze im System (plain JSON number). */
  readonly locationCountResource = httpResource<number>(
    () => `${this.API_URL}/location-count`,
    { defaultValue: 0 },
  );
}
