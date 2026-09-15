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
	 * Bucht Ware in das Lager ein (Goods Receipt).
	 */
	@Transactional
	public StockBatch receiveGoods(String sku, String locationCode, Integer quantity) {

		// 1. Validierung: Product and location
		Product product = productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product mit sku: " + sku + " nicht gefunden!"));

		Location location = locationRepository.findByLocationCode(locationCode)
				.orElseThrow(() -> new RuntimeException("Lagerplatz " + locationCode + " existiert nicht!"));

		// 2. Erstellung eines neuen Batches (FIFO- heutiges Datum)
		StockBatch sbatch = StockBatch.builder().product(product).location(location).quantity(quantity)
				.receivedDate(LocalDate.now()).build();
		
		stockBatchRepository.save(sbatch);
		return sbatch;
	}

	/**
	 * Buch Ware aus dem Lager aus unten Anwendung des FIFO-Prinzips (Goods Issue).
	 */
	@Transactional
	public void issueGoods(String sku, Integer quantityToIssue) {

		Product product = productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product nicht gefunden!"));

		// 1. Alle Batches fuer dieses Produkt holen, sortiert nach Datum (Aelteste
		// zuerst!)
		List<StockBatch> batches = stockBatchRepository.findByProductOrderByReceivedDateAsc(product);

		int remainingToIssue = quantityToIssue;

		for (StockBatch batch : batches) {
			if (remainingToIssue <= 0)
				break;

			int currentBatchQty = batch.getQuantity();

			if (currentBatchQty <= remainingToIssue) {
				// Batch wird komplett geleert
				remainingToIssue -= currentBatchQty;
				stockBatchRepository.delete(batch);// oder erstellt quantity als zero: batch.setQuantity(0);
			} else {
				// Batch wird teilweise geleert
				batch.setQuantity(currentBatchQty - remainingToIssue);
				remainingToIssue = 0;
			}
		}

		// Bestehlung ist zu groess
		if (remainingToIssue > 0) {
			throw new RuntimeException(
					"Nicht genuegend Bestand fuer SKU " + sku + ". Fehlende Menge: " + remainingToIssue);
		}

		// Alle geaenderten Batches speichern
		stockBatchRepository.saveAll(batches);
	}

	/**
	 * Berechnet den Gesamtbestand eines Produkts ueber alle Lagerplaetze hinweg.
	 */
	public Integer getTotalStock(String sku) {

		// 1. Validation
		Product product = productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product sku: " + sku + " nicht gefunden!"));

		return stockBatchRepository.findByProductOrderByReceivedDateAsc(product).stream()
				.mapToInt(StockBatch::getQuantity).sum();
	}

}
