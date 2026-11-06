package com.sisie.core.model;

import jakarta.persistence.*;
import lombok.*;


/**
 * Represents a physical product in the industrial electronics stock.
 * This class serves as the mapping for the database table 'products'.
 * 
 * <p>A product is the basic unit of the system and is identified by a unique SKU.</p>
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
	 * Unique internal ID of the product (primary key).
	 * Generated automatically by the database.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Stock Keeping Unit (SKU).
	 * A globally unique article number for identifying the product.
	 * Example: RES-10K-OHM-001
	 */
	@Column(unique=true, nullable=false)
	private String sku;
	
	/**
	 * Human-readable name of the product (e.g. "Resistor 10k Ohm").
	 */
	@Column(nullable=false)
	private String name;
	
	/**
	 * Category for grouping products (e.g. "Passive Components", "Sensors").
	 */
	private String category;
	
	/**
	 * Detailed technical description of the product.
	 */
	private String description;
	
	/**
	 * Threshold for the stock level.
	 * If the quantity falls below this value, a low-stock alert is raised.
	 * The default value is 10 units.
	 */
	@Column(name = "min_stock_level")
	private Integer minStockLevel =10;
}
