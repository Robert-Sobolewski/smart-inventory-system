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
 * Unit tests for DashboardService (pure Mockito, no Spring context).
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
		return Product.builder().id(id).sku(sku).name("Product " + sku)
				.category("Test").minStockLevel(minStockLevel).build();
	}

	private static StockBatch batch(Product product, int quantity) {
		return StockBatch.builder().product(product).quantity(quantity).build();
	}

	/** getStockOverview **/

	@Test
	@DisplayName("A product without stock batches should return total stock 0 and low-stock flag")
	void getStockOverview_ShouldReturnZeroAndLowStockWhenNoBatches() {
		// 1. SETUP - product without any stock batches, minStockLevel = null
		Product p = product(1L, "SKU-A", null);
		when(productRepository.findAll()).thenReturn(List.of(p));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(p))
				.thenReturn(List.of());

		// 2. ACT
		List<StockSummaryItem> result = service.getStockOverview();

		// 3. ASSERT - the default threshold is 10, so 0 < 10 => low stock
		assertEquals(1, result.size(), "There must be exactly one entry");
		StockSummaryItem item = result.get(0);
		assertEquals("SKU-A", item.getSku());
		assertEquals(0, item.getTotalQuantity(), "No batches means zero units");
		assertEquals(10, item.getMinStockLevel(), "null means the default threshold of 10");
		assertTrue(item.isLowStock(), "zero units is always a low-stock state");
	}

	@Test
	@DisplayName("Multiple batches should be summed into the total quantity")
	void getStockOverview_ShouldSumQuantitiesAcrossBatches() {
		// 1. SETUP - two batches of the same product
		Product p = product(2L, "SKU-B", 5);
		when(productRepository.findAll()).thenReturn(List.of(p));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(p))
				.thenReturn(List.of(batch(p, 7), batch(p, 3)));

		// 2. ACT
		List<StockSummaryItem> result = service.getStockOverview();

		// 3. ASSERT - 7 + 3 = 10 >= threshold 5 => not low stock
		assertEquals(10, result.get(0).getTotalQuantity(), "The sum of batches must be 10");
		assertEquals(5, result.get(0).getMinStockLevel());
		assertFalse(result.get(0).isLowStock(), "10 units with a threshold of 5 is not low stock");
	}

	@Test
	@DisplayName("Stock below the threshold should be marked as low stock")
	void getStockOverview_ShouldFlagLowStockBelowThreshold() {
		// 1. SETUP - stock below the configured threshold
		Product p = product(3L, "SKU-C", 20);
		when(productRepository.findAll()).thenReturn(List.of(p));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(p))
				.thenReturn(List.of(batch(p, 15)));

		// 2. ACT
		List<StockSummaryItem> result = service.getStockOverview();

		// 3. ASSERT - 15 < 20 => low stock
		assertEquals(15, result.get(0).getTotalQuantity());
		assertTrue(result.get(0).isLowStock(), "15 units with a threshold of 20 is low stock");
	}

	/** countLocations **/

	@Test
	@DisplayName("The location count should come from the repository")
	void countLocations_ShouldReturnRepositoryCount() {
		// 1. SETUP
		when(locationRepository.count()).thenReturn(4L);

		// 2. ACT
		long result = service.countLocations();

		// 3. ASSERT + VERIFY
		assertEquals(4L, result, "Must return the count from the repository");
		verify(locationRepository, times(1)).count();
	}

	/** getTotalUnits **/

	@Test
	@DisplayName("The total quantity should be summed across all products")
	void getTotalUnits_ShouldSumAcrossAllProducts() {
		// 1. SETUP - two products: 7 units and 3 units
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
		assertEquals(10, result, "The sum across all products must be 10");
	}

	/** countLowStockProducts **/

	@Test
	@DisplayName("Only products below the threshold should be counted")
	void countLowStockProducts_ShouldCountOnlyBelowThreshold() {
		// 1. SETUP - one product below the threshold, the other above it
		Product low = product(1L, "SKU-LOW", 20);
		Product ok = product(2L, "SKU-OK", 5);
		when(productRepository.findAll()).thenReturn(List.of(low, ok));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(low))
				.thenReturn(List.of(batch(low, 1)));   // 1 < 20 => low stock
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(ok))
				.thenReturn(List.of(batch(ok, 50)));  // 50 >= 5 => OK

		// 2. ACT
		long result = service.countLowStockProducts();

		// 3. ASSERT - only one product is below the threshold
		assertEquals(1L, result, "Only products below the threshold must be counted");
	}
}
