package com.sisie.core.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.model.Supplier;

/**
 * Supplier Kontroller. Steuert die HTTP-Endpoints fuer Lieferanten.
 */
@RestController
@RequestMapping("/api/suppliers")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class SupplierController {

    private final com.sisie.core.service.SupplierService supplierService;

    public SupplierController(com.sisie.core.service.SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /** Liefert alle Lieferanten zurueck. */
    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
    @GetMapping
    public List<Supplier> getAllSuppliers() {
        return supplierService.getAllSuppliers();
    }

    /** Sucht einen Lieferanten anhand der ID. */
    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
    @GetMapping("/{id}")
    public Supplier getById(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    /** Erstellt einen neuen Lieferanten. */
    @PostMapping
    public ResponseEntity<String> createSupplier(@RequestBody Supplier supplier) {
        com.sisie.core.model.Supplier created = supplierService.create(supplier);
        return ResponseEntity.ok("Lieferant " + created.getName() + " erfolgreich angelegt (ID: " + created.getId() + ")");
    }

    /** Aktualisiert einen bestehenden Lieferanten anhand der ID. */
    @PutMapping("/{id}")
    public Supplier updateSupplier(@PathVariable Long id, @RequestBody Supplier updated) {
        return supplierService.update(id, updated);
    }

    /** Loescht einen Lieferanten anhand der ID. */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSupplier(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.ok("Lieferant mit ID " + id + " geloescht");
    }
}
