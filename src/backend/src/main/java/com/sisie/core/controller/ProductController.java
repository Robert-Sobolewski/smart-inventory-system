package com.sisie.core.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.model.Product;
import com.sisie.core.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Product Kontroller
 * Lombok erstellt automatisch den Konstructor fur das Repository (Dependency Injection)
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
	
	private final ProductRepository productRepository;

	@GetMapping("/test")
	public String testDatabase() {
		
		// Ein Produkt erstellen
		Product product = Product.builder()
				.sku("TEST-001")
				.name("Test Chip")
				.category("Electronics")
				.build();
		
		// In die Datenbank speichern wenn Produkt Id not existiert in Datenbank
//		productRepository.save(product);
		Product p =productRepository.findById(product.getId()).orElseGet(()->{
			return productRepository.save(product);
		});
		
		// Alle Produkte auslesen, um zu pruefen ob es geklappt hat
		long count = productRepository.count();
		
		return String.format("Datenbank-Test erfolgreich! Aktuelle Anzahl an Produkten: %d", count);
	}
}
