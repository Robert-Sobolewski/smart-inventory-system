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
 * Repraesentiert eine spezifische Charge von Produkten an einem Lagerplatz.
 * Dies ist die zentralle Klasse fuer die FIFO-Logik (first-in-first-out).
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
	 * Verknuepfung zum Produkt. Viele Batches koennen dasselbe Produkt enthalten.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="product_id", nullable = false)
	private Product product;
	
	/**
	 * Verknuepfung zum Lagerplatz. Viele Batches koennen an einem Ort liegen.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="location_id", nullable = false)
	private Location location;
	
	/**
	 * Die aktuelle Menge in dieser spezifischen Charge.
	 */
	@Column(nullable = false)
	private Integer quantity;
	
	/**
	 * Das Datum des Wareneingangs. Essentiel fuer FIFO.
	 */
	@Column(nullable = false)
	private LocalDate receivedDate;
	
	/**
	 * Optionales Ablaufdatum oder Garantieende.
	 */
	private LocalDate expiryDate;
	
}
