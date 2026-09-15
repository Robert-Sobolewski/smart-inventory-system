package com.sisie.core.model;

import jakarta.persistence.*;
import lombok.*;


/**
 * Repraesentiert ein physisches Produkt im Lagerbestand der Industrieelektronik.
 * Diese Klasse dent als Mapping fur die Datenbanktabelle 'products'.
 * 
 * <p>Ein Produkt ist die grundlegende Einheit des Systems und eindeutige SKU identifiziert.</p>
 * 
 * @author rrr
 * @version 0.1
 * 
 */
@Entity
@Table(name="products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	/**
	 * Eindeutige interne Id des Produkts (Primaerschluessel).
	 * Wird automatisch durch die Datenbank generiert.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Stock Keeping Unit (SKU).
	 * Eine weltweit eindeutige Artikelnummer zur Identifikation des Produkts.
	 * Beispiel: RES-10K-OHM-001
	 */
	@Column(unique=true, nullable=false)
	private String sku;
	
	/**
	 * Der menschenlesbare Name des Produkts (z.B. "Widerstand 10k Ohm").
	 */
	@Column(nullable=false)
	private String name;
	
	/**
	 * Kategorie zur Gruppierung von Produkten (z.B. "Passivbauelemente", "Sensoren").
	 */
	private String category;
	
	/**
	 * Detaillierte technische Beschreibung des Produkts.
	 */
	private String description;
	
	/**
	 * Der Schwellenwert fur den Lagerbestand.
	 * Wenn die Menge unter diesen Wert faellt, wird ein Low-Stock Alert ausgeloest.
	 * Standardwert ist 10 Stueck.
	 */
	@Column(name = "min_stock_level")
	private Integer minStockLevel =10;
}
