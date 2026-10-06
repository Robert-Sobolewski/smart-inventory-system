package com.sisie.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Einzeiliger Bestandsbericht fuer ein Produkt auf dem Dashboard.
 * Aggregiert die Menge ueber alle Lagerplaetze und markiert Low-Stock-Faelle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSummaryItem {

	private String sku;
	private String name;
	private String category;

	/** Gesamtbestand des Produkts ueber alle Lagerplaetze (Stueck). */
	private Integer totalQuantity;

	/** Konfigurierter Schwellenwert aus der Produkttabelle. */
	private Integer minStockLevel;

	/** true, wenn totalQuantity unter dem Schwellenwert liegt. */
	private boolean lowStock;
}
