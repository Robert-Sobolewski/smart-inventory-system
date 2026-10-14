package com.sisie.core.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.dto.StockSummaryItem;
import com.sisie.core.service.DashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Dashboard-Kontroller. Liefert read-only Kennzahlen und die
 * Bestandsuebersicht fuer das Frontend-Dashboard.
 *
 * <p>Alle Rollen mit Lesezugriff (WORKER, MANAGER, ADMIN) koennen das
 * Dashboard sehen; es werden keine Daten geaendert.</p>
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	/** Bestandsuebersicht: eine Zeile pro Produkt mit Low-Stock-Markierung. */
	@GetMapping("/stock-overview")
	public List<StockSummaryItem> getStockOverview() {
		return dashboardService.getStockOverview();
	}

	/** Anzahl der Lagerplaetze im System. */
	@GetMapping("/location-count")
	public long getLocationCount() {
		return dashboardService.countLocations();
	}

	/** Gesamtbestand aller Produkte in Stueck. */
	@GetMapping("/total-units")
	public int getTotalUnits() {
		return dashboardService.getTotalUnits();
	}

	/** Anzahl der Produkte unter ihrem Schwellenwert (Low-Stock). */
	@GetMapping("/low-stock-count")
	public long getLowStockCount() {
		return dashboardService.countLowStockProducts();
	}
}
