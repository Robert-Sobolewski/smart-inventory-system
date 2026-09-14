package com.sisie.core.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sisie.core.model.Product;
import com.sisie.core.model.StockBatch;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
	
	/**
	 * Findet alle Batches eines Produkts, sortiert nach dem Eingangsdatum (aufsteigend).
	 * Das ist die Basis fuer FIFO: Die aeltesten Batches kommen zuerst!
	 */
	List<StockBatch> findByProductOrderByReceivedDateAsc(Product product);

}
