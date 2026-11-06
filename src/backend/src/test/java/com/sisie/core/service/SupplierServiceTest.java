package com.sisie.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sisie.core.model.Supplier;
import com.sisie.core.repository.SupplierRepository;

/**
 * Unit tests for SupplierService (pure Mockito, no Spring context).
 */
@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService service;

    private static Supplier supplier(Long id, String name, String email) {
        return Supplier.builder().id(id).name(name).email(email).build();
    }

    /** getAllSuppliers **/

    @Test
    @DisplayName("Should return all suppliers")
    void getAllSuppliers_ShouldReturnAll() {
        // 1. SETUP
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(1L, "Lieferant A", "a@example.de"),
                supplier(2L, "Lieferant B", "b@example.de")));

        // 2. ACT
        List<Supplier> result = service.getAllSuppliers();

        // 3. ASSERT
        assertEquals(2, result.size(), "Must return two suppliers");
        verify(supplierRepository, times(1)).findAll();
    }

    /** findById **/

    @Test
    @DisplayName("Should find a supplier by ID")
    void findById_ShouldReturnSupplierWhenExists() {
        // 1. SETUP
        Supplier existing = supplier(1L, "Lieferant A", "a@example.de");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        // 2. ACT
        Supplier result = service.findById(1L);

        // 3. ASSERT
        assertEquals("Lieferant A", result.getName(), "The name must match");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when the supplier does not exist")
    void findById_ShouldThrowWhenMissing() {
        // 1. SETUP
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        Exception ex = assertThrows(EntityNotFoundException.class,
                () -> service.findById(99L));

        assertTrue(ex.getMessage().contains("not found"),
                "The error must inform about a missing supplier");
    }

    /** create **/

    @Test
    @DisplayName("Should create a new supplier")
    void create_ShouldSaveNewSupplier() {
        // 1. SETUP
        Supplier neu = supplier(null, "Neu", "neu@example.de");
        when(supplierRepository.findByEmail("neu@example.de")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        Supplier result = service.create(neu);

        // 3. ASSERT + 4. VERIFY
        assertEquals("Neu", result.getName(), "Name of the new supplier");
        verify(supplierRepository, times(1)).save(neu);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when the e-mail already exists")
    void create_ShouldThrowWhenEmailExists() {
        // 1. SETUP
        Supplier neu = supplier(null, "Neu", "bestehend@example.de");
        Supplier vorhanden = supplier(1L, "Bestehender", "bestehend@example.de");
        when(supplierRepository.findByEmail("bestehend@example.de"))
                .thenReturn(Optional.of(vorhanden));

        // 2. ACT & 3. ASSERT
        Exception ex = assertThrows(IllegalStateException.class,
                () -> service.create(neu));

        assertTrue(ex.getMessage().contains("already exists"),
                "The error must inform about a taken e-mail address");
    }

    /** update **/

    @Test
    @DisplayName("Should update a supplier")
    void update_ShouldUpdateExisting() {
        // 1. SETUP
        Supplier existing = supplier(1L, "Alt", "alt@example.de");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        Supplier updated = supplier(null, "Neu", "neu@example.de");
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        Supplier result = service.update(1L, updated);

        // 3. ASSERT + VERIFY
        assertEquals("Neu", result.getName(), "Name after update");
        assertEquals("neu@example.de", result.getEmail(), "E-mail after update");
        verify(supplierRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Should allow an update with the same e-mail")
    void update_ShouldAllowSameEmail() {
        // 1. SETUP - the same e-mail address this supplier already has
        Supplier existing = supplier(1L, "Alt", "alt@example.de");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        Supplier updated = supplier(null, "Neu", "alt@example.de"); // same e-mail
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT & 3. ASSERT - must not throw a duplicate-e-mail exception
        assertDoesNotThrow(() -> service.update(1L, updated));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when the new e-mail is already taken")
    void update_ShouldThrowWhenNewEmailTakenByOther() {
        // 1. SETUP - another supplier already has this e-mail address
        Supplier existing = supplier(1L, "Alt", "alt@example.de");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        Supplier updated = supplier(null, "Neu", "besetzt@example.de");
        when(supplierRepository.findByEmail("besetzt@example.de"))
                .thenReturn(Optional.of(supplier(2L, "Inhaber", "besetzt@example.de")));

        // 2. ACT & 3. ASSERT - new e-mail differs from the current one and is already taken => exception
        Exception ex = assertThrows(IllegalStateException.class,
                () -> service.update(1L, updated));

        assertTrue(ex.getMessage().contains("is already used by another supplier"),
                "The error must inform about a taken e-mail address");

        // 4. VERIFY - nothing must have been saved
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    /** delete **/

    @Test
    @DisplayName("Should delete a supplier")
    void delete_ShouldDeleteExisting() {
        // 1. SETUP
        when(supplierRepository.findById(5L))
                .thenReturn(Optional.of(supplier(5L, "X", "x@example.de")));

        // 2. ACT
        service.delete(5L);

        // 3. VERIFY
        verify(supplierRepository, times(1)).deleteById(5L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a non-existent supplier")
    void delete_ShouldThrowWhenMissing() {
        // 1. SETUP
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(EntityNotFoundException.class, () -> service.delete(99L));

        // 4. VERIFY - delete must not have been called
        verify(supplierRepository, never()).deleteById(99L);
    }
}
