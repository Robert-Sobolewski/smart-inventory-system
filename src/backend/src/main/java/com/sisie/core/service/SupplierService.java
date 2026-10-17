package com.sisie.core.service;

import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sisie.core.model.Supplier;
import com.sisie.core.repository.SupplierRepository;

/**
 * Serwis do zarządzania dostawcami (Supplier).
 */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository repository;

    /**
     * Zwraca wszystkich dostawców.
     */
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return repository.findAll();
    }

    /**
     * Zwraca dostawcę po identyfikatorze.
     *
     * @throws EntityNotFoundException gdy dostawca o podanym ID nie istnieje
     */
    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Dostawca z ID %d nie został znaleziony".formatted(id)));
    }

    /**
     * Tworzy nowego dostawcę.
     *
     * @throws IllegalStateException gdy dostawca z podanym adresem e-mail już istnieje
     */
    @Transactional
    public Supplier create(Supplier supplier) {
        if (repository.findByEmail(supplier.getEmail()).isPresent()) {
            throw new IllegalStateException(
                    "Dostawca z adresem e-mail %s już istnieje".formatted(supplier.getEmail()));
        }
        return repository.save(supplier);
    }

    /**
     * Aktualizuje dane dostawcy (nazwa, e-mail, adres).
     *
     * @throws EntityNotFoundException gdy dostawca o podanym ID nie istnieje
     * @throws IllegalStateException   gdy nowy adres e-mail jest już zajęty przez innego dostawcę
     */
    @Transactional
    public Supplier update(Long id, Supplier updated) {
        Supplier existing = findById(id);
        String newEmail = updated.getEmail();
        if (!newEmail.equals(existing.getEmail())
                && repository.findByEmail(newEmail).isPresent()) {
            throw new IllegalStateException(
                    "Adres e-mail %s jest już zajęty przez innego dostawcę".formatted(newEmail));
        }
        existing.setName(updated.getName());
        existing.setAddress(updated.getAddress());
        existing.setEmail(newEmail);
        return repository.save(existing);
    }

    /**
     * Usuwa dostawcę po identyfikatorze.
     *
     * @throws EntityNotFoundException gdy dostawca o podanym ID nie istnieje
     */
    @Transactional
    public void delete(Long id) {
        findById(id); // rzuca wyjątek, jeśli dostawca nie istnieje
        repository.deleteById(id);
    }
}
