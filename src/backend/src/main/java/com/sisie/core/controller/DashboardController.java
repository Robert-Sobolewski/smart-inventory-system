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
 * Dashboard controller. Provides read-only key figures and the
 * stock overview for the frontend dashboard.
 *
 * <p>All roles with read access (WORKER, MANAGER, ADMIN) can view the
 * dashboard; no data is modified.</p>
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	/** Stock overview: one row per product with low-stock marking. */
	@GetMapping("/stock-overview")
	public List<StockSummaryItem> getStockOverview() {
		return dashboardService.getStockOverview();
	}

	/** Number of storage locations in the system. */
	@GetMapping("/location-count")
	public long getLocationCount() {
		return dashboardService.countLocations();
	}

	/** Total stock of all products (units). */
	@GetMapping("/total-units")
	public int getTotalUnits() {
		return dashboardService.getTotalUnits();
	}

	/** Number of products below their threshold (low stock). */
	@GetMapping("/low-stock-count")
	public long getLowStockCount() {
		return dashboardService.countLowStockProducts();
	}
}
