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
 * Service fuer das Lager-Dashboard. Aggregiert Bestandsdaten ueber alle
 * Produkte und Lagerplaetze hinweg (read-only, keine Buchungen).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

	private final ProductRepository productRepository;
	private final LocationRepository locationRepository;
	private final StockBatchRepository stockBatchRepository;

	/**
	 * Liefert eine Zeile pro Produkt mit Gesamtbestand und Low-Stock-Markierung.
	 * Produkte ohne jeglichen Bestand erscheinen mit totalQuantity = 0.
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

	/** Anzahl der Lagerplaetze im System. */
	public long countLocations() {
		return locationRepository.count();
	}

	/** Gesamtbestand aller Produkte in Stueck. */
	public int getTotalUnits() {
		return getStockOverview().stream()
				.mapToInt(StockSummaryItem::getTotalQuantity)
				.sum();
	}

	/** Anzahl der Produkte unter ihrem Schwellenwert (Low-Stock). */
	public long countLowStockProducts() {
		return getStockOverview().stream()
				.filter(StockSummaryItem::isLowStock)
				.count();
	}
}
