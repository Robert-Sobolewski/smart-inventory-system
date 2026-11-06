package com.sisie.core.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sisie.core.model.Location;
import com.sisie.core.model.Product;
import com.sisie.core.model.StockBatch;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
	
	/**
	 * Finds all batches of a product, sorted by receipt date (ascending).
	 * This is the basis for FIFO: the oldest batches come first!
	 */
	List<StockBatch> findByProductOrderByReceivedDateAsc(Product product);

	/**
	 * Counts all batches at a storage location.
	 * Used by the delete guard: an occupied location must not be deleted.
	 */
	long countByLocation(Location location);
}
