package com.sisie.core.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a specific batch of products at a storage location.
 * This is the central class for the FIFO logic (first-in-first-out).
 */
@Entity
@Table(name="stock_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Link to the product. Many batches can contain the same product.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="product_id", nullable = false)
	private Product product;
	
	/**
	 * Link to the storage location. Many batches can be at one location.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="location_id", nullable = false)
	private Location location;
	
	/**
	 * The current quantity in this specific batch.
	 */
	@Column(nullable = false)
	private Integer quantity;
	
	/**
	 * The goods-receipt date. Essential for FIFO.
	 */
	@Column(nullable = false)
	private LocalDate receivedDate;
	
	/**
	 * Optional expiry or warranty-end date.
	 */
	private LocalDate expiryDate;
	
}
