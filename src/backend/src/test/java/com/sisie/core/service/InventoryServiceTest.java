package com.sisie.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;

import com.sisie.core.model.Location;
import com.sisie.core.model.Product;
import com.sisie.core.model.StockBatch;
import com.sisie.core.repository.LocationRepository;
import com.sisie.core.repository.ProductRepository;
import com.sisie.core.repository.StockBatchRepository;

import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;

@ExtendWith(MockitoExtension.class) // Activate Mockito
class InventoryServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private StockBatchRepository stockBatchRepository;

	@Mock
	private LocationRepository locationRepository;

	@InjectMocks
	private InventoryService inventoryService;
	
	/** getTotalStock function **/

	@Test
	@DisplayName("Sollte die korrekte Gesamtmenge fuer eine SKU berechnen")
	void getTotalStock_ShouldReturnCorrectSum() {

		// 1. SETUP (Vorbereitung)
		String sku = "SKU-123";
		Product mockProduct = Product.builder().sku(sku).build();

		// wir definieren, was der Mock zurueckgeben soll, wenn er aufgerufen wird
		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));

		// wir simulieren zwei Batches im Lager: einen mit 10 Stueck und einen mit 5
		// Stueck
		StockBatch batch1 = StockBatch.builder().quantity(10).build();
		StockBatch batch2 = StockBatch.builder().quantity(5).build();

		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(mockProduct)).thenReturn(List.of(batch1, batch2));

		// 2. ACT (Ausfuehrung)
		int totalStock = inventoryService.getTotalStock(sku);

		// 3. ASSERT ( Ueberpruefung)
		assertEquals(15, totalStock, "Die Summe der Bestaende muss 15 sein");

	}

	@Test
	@DisplayName("Should throw RuntimeException when SKU not found")
	void getTotalStock_ShouldThrowException() {

		// 1. SETUP
		String sku = "SKU-NOT-EXIST";

		when(productRepository.findBySku(sku)).thenReturn(Optional.empty());

		// 3. ACT & ASSERT
		RuntimeException exception = assertThrows(RuntimeException.class, () -> inventoryService.getTotalStock(sku));

		assertTrue(exception.getMessage().contains("nicht gefunden"),
				"The Error message must contains 'nicht gefunden'");

	}

	@Test
	@DisplayName("Should return 0 , when Product exist but Stock is empty")
	void gettotalStock_ShouldReturnZeroWhenNoBatchesExist() {

		// 1. SETUP
		String sku = "SKU-EMPTY";
		Product mockProduct = Product.builder().sku(sku).build();

		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));
		// Wir simulieren eine LEERE Liste von Batches

		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(mockProduct)).thenReturn(List.of());

		// ACT
		int totalStock = inventoryService.getTotalStock(sku);

		// ASSERT
		assertEquals(0, totalStock, "When resources are empty, should return zero");
	}

	/** receiveGoods function **/
	
	@Test
	@DisplayName("Should throw Runtime exception when SKU not found")
	void receiveGoods_ShouldThowExceptionWhenSKUNotExist() {

		// 1. SETUP
		String sku = "SKU-NAN";
		String locationCode = "A-1-03";
		int quantity = 10;

		// 2. ACT & ASSERT
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> inventoryService.receiveGoods(sku, locationCode, quantity));

		assertTrue(exception.getMessage().contains("nicht gefunden"), "Error message must contains 'nicht gefunden'");

	}

	@Test
	@DisplayName("Should throw Runtime Exception when location Code not exist")
	void receiveGoods_ShouldThrowExceptionWhenLocationCodeNotExist() {

		// 1. SETUP
		String sku = "SKU-123";
		String locationCode = "LOCATION-NOT-EXIST";
		int quantity = 10;
		Product mockProduct = Product.builder().sku(sku).build();

		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));

		// 2. ACT & ASSERT
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> inventoryService.receiveGoods(sku, locationCode, quantity));

		assertTrue(exception.getMessage().contains("Lagerplatz " + locationCode),
				"Error message must contains 'LOCATION-NOT-EXIST'");

	}
	
	@Test
	@DisplayName("Should set Product in Stock")
	void receiveGoods_ShouldAddProductToStock() {
		
		// 1. SETUP
		String sku="SKU-123";
		String locationCode="A-1-04";
		Integer quantity = 10;
		
		Product mockProduct= Product.builder().sku(sku).build();
		Location mockLocation = Location.builder().locationCode(locationCode).build();
		
		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));
		when(locationRepository.findByLocationCode(locationCode)).thenReturn(Optional.of(mockLocation));
		
		// 2. ACT
		StockBatch stockBatch = inventoryService.receiveGoods(sku, locationCode, quantity);
		
		// 3. ASSERT
		assertAll("Verify returned results",
				() -> assertEquals(mockProduct, stockBatch.getProduct(),"Product must be the same."),
				() -> assertEquals(mockLocation, stockBatch.getLocation(),"Location must be the same."),
				() -> assertEquals(quantity, stockBatch.getQuantity(),"Quantity must be the same.")
				);
		
		// 4. VERIFY (Das Verhalten prüfen - DER PROFI-SCHRITT!)
        // Wir prüfen, ob das Repository die save-Methode genau EINMAL aufgerufen hat
        verify(stockBatchRepository, times(1)).save(any(StockBatch.class));
		
		
	}
	
	/** issueGoods function **/
	
	@Test
	@DisplayName("Should throw RuntimeException when SKU not found. ")
	void issueGoods_ShouldThrowExceptionWhenSKUNotExist() {
		
		// 1. SETUP
		String sku = "SKU-NOT-EXIST";
		Integer quantity = 5;
		
		when(productRepository.findBySku(sku)).thenReturn(Optional.empty());
		
		// 2. ACT
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> inventoryService.issueGoods(sku, quantity));
		
		// 3. ASSERT
		
		assertTrue(exception.getMessage().contains("Product nicht gefunden!"));
	}
	
	@Test
	@DisplayName("Should throw RuntimeException when quantity is less than zero")
	void issueGoods_ShouldThrowExceptionWhenQuantityIsLessThanZero() {
		
		// 1. SETUP
		String sku = "SKU-123";
		Integer qty = -10;
		Product mockProduct = Product.builder().sku(sku).build();
		StockBatch mockStockBatch = StockBatch.builder().product(mockProduct)
				.quantity(5).build();
		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(mockProduct))
		.thenReturn(List.of(mockStockBatch));
		
		// 2. ACT
		
		RuntimeException exception = assertThrows( RuntimeException.class,
				() -> inventoryService.issueGoods(sku, qty));
		
		// 3. ASSERT
		assertTrue(exception.getMessage().contains("Quantity must Positiv sein!."));
				
	}
	
	@Test
	@DisplayName("Should throw RuntimeException when order is too big")
	void issueGoods_ShouldThrowExceptionWhenOrderIsTooBig() {
		
		// 1. Setup
		String sku= "SKU-001";
		Integer requiredQuantity = 30;
		
		Product mockProduct =Product.builder().sku(sku).build();
		
		// erstellt zwei Stock Batche mit 10 Stueck
		StockBatch mockStockBatch1 = StockBatch.builder().product(mockProduct).quantity(10).
				receivedDate(LocalDate.now().minusWeeks(1)) .build();
		StockBatch mockStockBatch2 = StockBatch.builder().product(mockProduct).quantity(10).
				receivedDate(LocalDate.now().minusDays(3)).build();
		
		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(mockProduct))
		.thenReturn(List.of(mockStockBatch1,mockStockBatch2));
		
		// 2. Act
		RuntimeException exception = assertThrows(RuntimeException.class,
				()->inventoryService.issueGoods(sku, requiredQuantity));
		
		// 3. Assert
		assertTrue(exception.getMessage().contains("Nicht genuegend Bestand fuer SKU"));
		
		
	}
	
	@Test
	@DisplayName("Should return correct products quantity")
	void issueGoods_ShouldReturnCorrectProductsQuantity() {
		
		// 1. Setup
		String sku ="SKU-001";
		Integer requiredQuantity = 8;
		
		Product mockProduct = Product.builder().sku(sku).build();
		StockBatch mockStockBatch = StockBatch.builder().product(mockProduct).quantity(10).build();
		
		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(mockProduct))
		.thenReturn(List.of(mockStockBatch));
		
		// 2. Act
		inventoryService.issueGoods(sku, requiredQuantity);
		
		// 3. Assert
		assertEquals(2, mockStockBatch.getQuantity());
		
	}
		
	@Test
	@DisplayName("Should take older products first")
	void issueGoods_ShouldTakeOlderProductsFirst() {
		
		// 1. Setup
		String sku="SKU-001";
		Integer requiredQty=3;
		
		Product mockProduct = Product.builder().sku(sku).build();
		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));
		
		StockBatch mockStock1 = StockBatch.builder().product(mockProduct).quantity(5)
				.receivedDate(LocalDate.now().minusDays(4)).build();
		StockBatch mockStock2 = StockBatch.builder().product(mockProduct).quantity(5)
				.receivedDate(LocalDate.now().minusDays(2)).build();
		
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(mockProduct))
		.thenReturn(List.of(mockStock1,mockStock2));
		
		// 2. Act
		inventoryService.issueGoods(sku, requiredQty);
		
		// 3 Assert
		assertAll("Verify returned results",
				() -> assertEquals(2, mockStock1.getQuantity(),"Stock quantity should be equal."),
				() -> assertEquals(5, mockStock2.getQuantity(),"Stock amount should be equal.")
				);
	}
	
	@Test
	@DisplayName("Should take products from more than one stock")
	void issueGoods_ShouldTakeProductsFromMoreThanOneStock() {
		
		// 1. Setup
		String sku= "SKU-001";
		Integer requiredQty=7;
		
		Product mockProduct = Product.builder().sku(sku).build();
		when(productRepository.findBySku(sku)).thenReturn(Optional.of(mockProduct));
		
		StockBatch mockStock1 = StockBatch.builder().product(mockProduct).quantity(5)
				.receivedDate(LocalDate.now().minusDays(4)).build();
		StockBatch mockStock2 = StockBatch.builder().product(mockProduct).quantity(5)
				.receivedDate(LocalDate.now().minusDays(2)).build();
		when(stockBatchRepository.findByProductOrderByReceivedDateAsc(mockProduct))
		.thenReturn(List.of(mockStock1, mockStock2));
		
		// 2. Act
		inventoryService.issueGoods(sku, requiredQty);
		
		System.out.println("mstock1: "+ mockStock1.getQuantity()+", mstock2: "+mockStock2.getQuantity());
		// 3. Assert
		assertAll("Verify returned results",
				() -> assertEquals(0, mockStock1.getQuantity(),"Stock must be empty."),
				() -> assertEquals(3, mockStock2.getQuantity(),"Stock amount must be equal.")
				);
	}

}
