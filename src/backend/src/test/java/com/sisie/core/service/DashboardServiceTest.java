package com.sisie.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sisie.core.dto.StockSummaryItem;
import com.sisie.core.model.Product;
import com.sisie.core.model.StockBatch;
import com.sisie.core.repository.LocationRepository;
import com.sisie.core.repository.ProductRepository;
import com.sisie.core.repository.StockBatchRepository;

/**
 * Testy jednostkowe DashboardService (czysty Mockito, bez kontekstu Springa).
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private LocationRepository locationRepository;

	@Mock
	private StockBatchRepository stockBatchRepository;

	@InjectMocks
	private DashboardService service;

	private static Product product(Long id, String sku, Integer minStockLevel) {
		return Product.builder().id(id).sku(sku).name("Produkt " + sku)
				.category("Test").minStockLevel(minStockLevel).build();
	}

	private static StockBatch batch(Product product, int quantity) {
		return StockBatch.builder().product(product).quantity(quantity).build();
	}

	/** getStockOverview **/

	@Test
	@DisplayName("Produkt ohne Bestandsbaetze soll Gesamtbestand 0 und Low-Stock liefern")
	void getStockOverview_ShouldReturnZeroAndLowStockWhenNoBatches() {
		// 1. SETUP - produkt bez zadnych partii magazynowych, minStockLevel = null
		Product p = product(1L, "SKU-A", null);
		when(productRepository.findAll()).thenReturn(List.of(p));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(p))
				.thenReturn(List.of());

		// 2. ACT
		List<StockSummaryItem> result = service.getStockOverview();

		// 3. ASSERT - domyślny prog to 10, wiec 0 < 10 => lowStock
		assertEquals(1, result.size(), "Musi byc jedna pozycja");
		StockSummaryItem item = result.get(0);
		assertEquals("SKU-A", item.getSku());
		assertEquals(0, item.getTotalQuantity(), "Brak partii oznacza zero sztuk");
		assertEquals(10, item.getMinStockLevel(), "Null ma znaczenie domyślnego progu 10");
		assertTrue(item.isLowStock(), "Zero sztuk to zawsze stan niskiego magazynu");
	}

	@Test
	@DisplayName("Mehrere Baetze sollen zur Gesamtmenge summiert werden")
	void getStockOverview_ShouldSumQuantitiesAcrossBatches() {
		// 1. SETUP - dwie partie tego samego produktu
		Product p = product(2L, "SKU-B", 5);
		when(productRepository.findAll()).thenReturn(List.of(p));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(p))
				.thenReturn(List.of(batch(p, 7), batch(p, 3)));

		// 2. ACT
		List<StockSummaryItem> result = service.getStockOverview();

		// 3. ASSERT - 7 + 3 = 10 >= progu 5 => nie low stock
		assertEquals(10, result.get(0).getTotalQuantity(), "Suma partii musi wynosic 10");
		assertEquals(5, result.get(0).getMinStockLevel());
		assertFalse(result.get(0).isLowStock(), "10 sztuk przy progu 5 to nie low stock");
	}

	@Test
	@DisplayName("Bestand unter dem Schwellenwert soll als Low-Stock markiert werden")
	void getStockOverview_ShouldFlagLowStockBelowThreshold() {
		// 1. SETUP - stan nizszy od skonfigurowanego progu
		Product p = product(3L, "SKU-C", 20);
		when(productRepository.findAll()).thenReturn(List.of(p));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(p))
				.thenReturn(List.of(batch(p, 15)));

		// 2. ACT
		List<StockSummaryItem> result = service.getStockOverview();

		// 3. ASSERT - 15 < 20 => low stock
		assertEquals(15, result.get(0).getTotalQuantity());
		assertTrue(result.get(0).isLowStock(), "15 sztuk przy progu 20 to low stock");
	}

	/** countLocations **/

	@Test
	@DisplayName("Lagerplatzanzahl soll aus dem Repository kommen")
	void countLocations_ShouldReturnRepositoryCount() {
		// 1. SETUP
		when(locationRepository.count()).thenReturn(4L);

		// 2. ACT
		long result = service.countLocations();

		// 3. ASSERT + VERIFY
		assertEquals(4L, result, "Musi zwracac liczbe z repository");
		verify(locationRepository, times(1)).count();
	}

	/** getTotalUnits **/

	@Test
	@DisplayName("Gesamtmenge soll ueber alle Produkte summiert werden")
	void getTotalUnits_ShouldSumAcrossAllProducts() {
		// 1. SETUP - dwa produkty: 7 sztuk i 3 sztuki
		Product a = product(1L, "SKU-A", null);
		Product b = product(2L, "SKU-B", null);
		when(productRepository.findAll()).thenReturn(List.of(a, b));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(a))
				.thenReturn(List.of(batch(a, 7)));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(b))
				.thenReturn(List.of(batch(b, 3)));

		// 2. ACT
		int result = service.getTotalUnits();

		// 3. ASSERT - 7 + 3 = 10
		assertEquals(10, result, "Suma wszystkich produktow musi wynosic 10");
	}

	/** countLowStockProducts **/

	@Test
	@DisplayName("Nur Produkte unter dem Schwellenwert sollen gezahlt werden")
	void countLowStockProducts_ShouldCountOnlyBelowThreshold() {
		// 1. SETUP - jeden produkt pod progiem, drugi nad progiem
		Product low = product(1L, "SKU-LOW", 20);
		Product ok = product(2L, "SKU-OK", 5);
		when(productRepository.findAll()).thenReturn(List.of(low, ok));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(low))
				.thenReturn(List.of(batch(low, 1)));   // 1 < 20 => low stock
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(ok))
				.thenReturn(List.of(batch(ok, 50)));  // 50 >= 5 => OK

		// 2. ACT
		long result = service.countLowStockProducts();

		// 3. ASSERT - tylko jeden produkt pod progiem
		assertEquals(1L, result, "Tylko produkty pod progiem maja byc policzone");
	}
}
