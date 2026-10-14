import { Component, computed, inject } from '@angular/core';
import { DashboardService, StockSummaryItem } from '../../services/dashboard-service';

/**
 * Lager-Dashboard: read-only Kennzahlen und Bestandsuebersicht.
 * Alle Daten kommen aus dem Backend (DashboardController) als httpResource.
 */
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly dashboardService = inject(DashboardService);

  protected readonly stockOverview = this.dashboardService.stockOverviewResource;
  protected readonly locationCount = this.dashboardService.locationCountResource;

  /** Gesamtbestand aller Produkte (aus der Uebersicht berechnet, kein extra Request). */
  protected readonly totalUnits = computed(() => {
    const rows = this.stockOverview.value();
    if (!rows) return null;
    return rows.reduce((sum, row) => sum + row.totalQuantity, 0);
  });

  /** Anzahl der Produkte unter ihrem Schwellenwert. */
  protected readonly lowStockCount = computed(() => {
    const rows = this.stockOverview.value();
    if (!rows) return null;
    return rows.filter((row) => row.lowStock).length;
  });

  /** Sortiert: Low-Stock zuerst, dann nach SKU (stabile Anzeige). */
  protected readonly sortedRows = computed<StockSummaryItem[]>(() => {
    const rows = this.stockOverview.value();
    if (!rows) return [];
    return [...rows].sort((a, b) => {
      if (a.lowStock !== b.lowStock) return a.lowStock ? -1 : 1;
      return a.sku.localeCompare(b.sku);
    });
  });
}
