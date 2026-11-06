package com.sisie.core.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sisie.core.model.Location;
import com.sisie.core.model.Product;
import com.sisie.core.model.StockBatch;
import com.sisie.core.repository.LocationRepository;
import com.sisie.core.repository.ProductRepository;
import com.sisie.core.repository.StockBatchRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service fuer die Verwaltung des Lagerbestands. Implementiert die Kernlogik
 * fuer Wareneingang und FIFO-basierten Warenausgang.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

	private final ProductRepository productRepository;
	private final LocationRepository locationRepository;
	private final StockBatchRepository stockBatchRepository;

	/**
	 * Books goods into stock (goods receipt).
	 */
	@Transactional
	public StockBatch receiveGoods(String sku, String locationCode, Integer quantity) {

		// 1. Validation: product and location
		Product product = productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product with SKU " + sku + " not found!"));

		Location location = locationRepository.findByLocationCode(locationCode)
				.orElseThrow(() -> new RuntimeException("Storage location " + locationCode + " does not exist!"));

		// 2. Create a new batch (FIFO - today's date)
		StockBatch sbatch = StockBatch.builder().product(product).location(location).quantity(quantity)
				.receivedDate(LocalDate.now()).build();

		stockBatchRepository.save(sbatch);
		return sbatch;
	}

	/**
	 * Issues goods out of stock, applying the FIFO principle (goods issue).
	 */
	@Transactional
	public void issueGoods(String sku, Integer quantityToIssue) {

		Product product = productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product not found!"));

		// 1. Fetch all batches for this product, sorted by date (oldest first!)
		List<StockBatch> batches = stockBatchRepository.findByProductOrderByReceivedDateAsc(product);

		// When quantityToIssue is less or equal zero throw an exception
		if (quantityToIssue <= 0) {
			throw new RuntimeException("Quantity must be positive!");
		}
		int remainingToIssue = quantityToIssue;

		for (StockBatch batch : batches) {
			if (remainingToIssue <= 0)
				break;

			int currentBatchQty = batch.getQuantity();

			if (currentBatchQty <= remainingToIssue) {
				// The batch is completely emptied
				remainingToIssue -= currentBatchQty;
				
				batch.setQuantity(0); // mark the batch as fully consumed
			} else {
				// The batch is partially emptied
				batch.setQuantity(currentBatchQty - remainingToIssue);
				remainingToIssue = 0;
			}
		}

		// The requested quantity exceeds available stock
		if (remainingToIssue > 0) {
			throw new RuntimeException(
					"Not enough stock for SKU " + sku + ". Missing quantity: " + remainingToIssue);
		}

		// Save all modified batches
		stockBatchRepository.saveAll(batches);
	}

	/**
	 * Calculates the total stock of a product across all storage locations.
	 */
	public Integer getTotalStock(String sku) {

		// 1. Validation
		Product product = productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product with SKU " + sku + " not found!"));

		return stockBatchRepository.findByProductOrderByReceivedDateAsc(product).stream()
				.mapToInt(StockBatch::getQuantity).sum();
	}

}
