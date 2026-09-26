package com.sisie.core.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.dto.ProductRequest;
import com.sisie.core.model.Product;
import com.sisie.core.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Product Kontroller Lombok erstellt automatisch den Konstructor fur das
 * Repository (Dependency Injection)
 */
@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class ProductController {

	private final ProductRepository productRepository;

    /**
     * Get all products.
     *
     * @return List of products
     */
    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


	/**
	 * Sucht ein Produkt ueber die SKU
	 * 
	 * @return Product
	 */
    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
	@GetMapping("/{sku}")
	public Product getProduct(@PathVariable String sku) {

		return productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product mit SKU: " + sku + " nicht gefunden!"));
	}

	/**
	 * Erstellt ein neues Produkt oder gibt das bestehende zurrueck, falls die SKU
	 * existiert.
	 * 
	 * @return - Bestaetingung erstellung Prozess
	 */
    @PreAuthorize("hasRole('MANAGER')")
	@PostMapping("/create")
	public ResponseEntity<String> createProduct(@RequestBody ProductRequest request) {

		Optional<Product> existing = productRepository.findBySku(request.getSku());
		if (existing.isPresent()) {
			throw new RuntimeException(String.format("Product mit SKU %s existiert bereits!", request.getSku()));
		}

		Product p = Product.builder().sku(request.getSku()).name(request.getName()).category(request.getCategory())
				.description(request.getDescription()).build();
		productRepository.save(p);
		return ResponseEntity.ok(String.format("Product %s erfolgreich angelegt!", request.getSku()));

	}

    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
	@GetMapping("/test")
	public String testDatabase() {

		// Alle Produkte auslesen, um zu pruefen ob es geklappt hat
		long count = productRepository.count();

		return String.format("Datenbank-Check OK! Anzahl Produkte: %d", count);
	}
}
