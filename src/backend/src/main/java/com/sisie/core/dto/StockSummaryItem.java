package com.sisie.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single-row stock report for one product on the dashboard.
 * Aggregates quantity across all storage locations and marks low-stock cases.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSummaryItem {

	private String sku;
	private String name;
	private String category;

	/** Total stock of the product across all storage locations (units). */
	private Integer totalQuantity;

	/** Configured threshold from the product table. */
	private Integer minStockLevel;

	/** true when totalQuantity is below the threshold. */
	private boolean lowStock;
}
