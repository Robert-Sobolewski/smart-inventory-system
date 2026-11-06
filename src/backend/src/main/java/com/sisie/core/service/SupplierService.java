package com.sisie.core.service;

import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sisie.core.model.Supplier;
import com.sisie.core.repository.SupplierRepository;

/**
 * Service for managing suppliers.
 */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;

    /**
     * Returns all suppliers.
     */
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return repository.findAll();
    }

    /**
     * Returns a supplier by its identifier.
     *
     * @throws EntityNotFoundException if no supplier exists with the given ID
     */
    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Supplier with ID %d not found".formatted(id)));
    }

    /**
     * Creates a new supplier.
     *
     * @throws IllegalStateException if a supplier with the given email address already exists
     */
    @Transactional
    public Supplier create(Supplier supplier) {
        if (repository.findByEmail(supplier.getEmail()).isPresent()) {
            throw new IllegalStateException(
                    "A supplier with email address %s already exists".formatted(supplier.getEmail()));
        }
        return repository.save(supplier);
    }

    /**
     * Updates a supplier's data (name, email, address).
     *
     * @throws EntityNotFoundException if no supplier exists with the given ID
     * @throws IllegalStateException   if the new email address is already used by another supplier
     */
    @Transactional
    public Supplier update(Long id, Supplier updated) {
        Supplier existing = findById(id);
        String newEmail = updated.getEmail();
        if (!newEmail.equals(existing.getEmail())
                && repository.findByEmail(newEmail).isPresent()) {
            throw new IllegalStateException(
                    "Email address %s is already used by another supplier".formatted(newEmail));
        }
        existing.setName(updated.getName());
        existing.setAddress(updated.getAddress());
        existing.setEmail(newEmail);
        return repository.save(existing);
    }

    /**
     * Deletes a supplier by its identifier.
     *
     * @throws EntityNotFoundException if no supplier exists with the given ID
     */
    @Transactional
    public void delete(Long id) {
        findById(id); // throws if the supplier does not exist
        repository.deleteById(id);
    }
}
