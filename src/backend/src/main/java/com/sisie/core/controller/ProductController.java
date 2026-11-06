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
 * Product controller. Lombok automatically generates the constructor for the
 * repository (dependency injection).
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
	 * Looks up a product by its SKU.
	 * 
	 * @return Product
	 */
    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
	@GetMapping("/{sku}")
	public Product getProduct(@PathVariable String sku) {

		return productRepository.findBySku(sku)
				.orElseThrow(() -> new RuntimeException("Product with SKU " + sku + " not found!"));
	}

	/**
	 * Creates a new product. If the SKU already exists, a RuntimeException is thrown.
	 * 
	 * @return - confirmation of the creation process
	 */
    @PreAuthorize("hasRole('MANAGER')")
	@PostMapping("/create")
	public ResponseEntity<String> createProduct(@RequestBody ProductRequest request) {

		Optional<Product> existing = productRepository.findBySku(request.getSku());
		if (existing.isPresent()) {
			throw new RuntimeException(String.format("Product with SKU %s already exists!", request.getSku()));
		}

		Product p = Product.builder().sku(request.getSku()).name(request.getName()).category(request.getCategory())
				.description(request.getDescription()).build();
		productRepository.save(p);
		return ResponseEntity.ok(String.format("Product %s erfolgreich angelegt!", request.getSku()));

	}

    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
	@GetMapping("/test")
	public String testDatabase() {

		// Read all products back to verify it worked
		long count = productRepository.count();

		return String.format("Datenbank-Check OK! Anzahl Produkte: %d", count);
	}
}
