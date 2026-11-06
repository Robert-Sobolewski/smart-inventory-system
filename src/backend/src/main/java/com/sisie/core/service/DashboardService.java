package com.sisie.core.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sisie.core.dto.StockSummaryItem;
import com.sisie.core.model.Product;
import com.sisie.core.repository.LocationRepository;
import com.sisie.core.repository.ProductRepository;
import com.sisie.core.repository.StockBatchRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for the stock dashboard. Aggregates stock data across all
 * products and storage locations (read-only, no bookings).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

	private final ProductRepository productRepository;
	private final LocationRepository locationRepository;
	private final StockBatchRepository stockBatchRepository;

	/**
	 * Returns one row per product with total stock and low-stock marking.
	 * Products without any stock appear with totalQuantity = 0.
	 */
	public List<StockSummaryItem> getStockOverview() {
		return productRepository.findAll().stream()
				.map(this::toSummary)
				.toList();
	}

	private StockSummaryItem toSummary(Product product) {
		int total = stockBatchRepository.findByProductOrderByReceivedDateAsc(product).stream()
				.mapToInt(batch -> batch.getQuantity() == null ? 0 : batch.getQuantity())
				.sum();
		int minLevel = product.getMinStockLevel() == null ? 10 : product.getMinStockLevel();

		return StockSummaryItem.builder()
				.sku(product.getSku())
				.name(product.getName())
				.category(product.getCategory())
				.totalQuantity(total)
				.minStockLevel(minLevel)
				.lowStock(total < minLevel)
				.build();
	}

	/** Number of storage locations in the system. */
	public long countLocations() {
		return locationRepository.count();
	}

	/** Total stock of all products (units). */
	public int getTotalUnits() {
		return getStockOverview().stream()
				.mapToInt(StockSummaryItem::getTotalQuantity)
				.sum();
	}

	/** Number of products below their threshold (low stock). */
	public long countLowStockProducts() {
		return getStockOverview().stream()
				.filter(StockSummaryItem::isLowStock)
				.count();
	}
}
