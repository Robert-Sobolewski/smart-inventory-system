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
 * Testy jednostkowe SupplierService (czysty Mockito, bez kontekstu Springa).
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
    @DisplayName("Sollte alle Lieferanten zurueckgeben")
    void getAllSuppliers_ShouldReturnAll() {
        // 1. SETUP
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(1L, "Lieferant A", "a@example.de"),
                supplier(2L, "Lieferant B", "b@example.de")));

        // 2. ACT
        List<Supplier> result = service.getAllSuppliers();

        // 3. ASSERT
        assertEquals(2, result.size(), "Musi zwrocic dwoch dostawcow");
        verify(supplierRepository, times(1)).findAll();
    }

    /** findById **/

    @Test
    @DisplayName("Sollte Lieferanten nach ID finden")
    void findById_ShouldReturnSupplierWhenExists() {
        // 1. SETUP
        Supplier existing = supplier(1L, "Lieferant A", "a@example.de");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        // 2. ACT
        Supplier result = service.findById(1L);

        // 3. ASSERT
        assertEquals("Lieferant A", result.getName(), "Nazwa musi sie zgadzac");
    }

    @Test
    @DisplayName("Sollte EntityNotFoundException werfen, wenn Lieferant nicht existiert")
    void findById_ShouldThrowWhenMissing() {
        // 1. SETUP
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        Exception ex = assertThrows(EntityNotFoundException.class,
                () -> service.findById(99L));

        assertTrue(ex.getMessage().contains("nie został znaleziony"),
                "Komunikat musi informowac o braku dostawcy");
    }

    /** create **/

    @Test
    @DisplayName("Sollte neuen Lieferanten anlegen")
    void create_ShouldSaveNewSupplier() {
        // 1. SETUP
        Supplier neu = supplier(null, "Neu", "neu@example.de");
        when(supplierRepository.findByEmail("neu@example.de")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT
        Supplier result = service.create(neu);

        // 3. ASSERT + 4. VERIFY
        assertEquals("Neu", result.getName(), "Nazwa nowego dostawcy");
        verify(supplierRepository, times(1)).save(neu);
    }

    @Test
    @DisplayName("Sollte IllegalStateException werfen, wenn E-Mail existiert")
    void create_ShouldThrowWhenEmailExists() {
        // 1. SETUP
        Supplier neu = supplier(null, "Neu", "bestehend@example.de");
        Supplier vorhanden = supplier(1L, "Bestehender", "bestehend@example.de");
        when(supplierRepository.findByEmail("bestehend@example.de"))
                .thenReturn(Optional.of(vorhanden));

        // 2. ACT & 3. ASSERT
        Exception ex = assertThrows(IllegalStateException.class,
                () -> service.create(neu));

        assertTrue(ex.getMessage().contains("już istnieje"),
                "Komunikat musi informowac o zajetym adresie e-mail");
    }

    /** update **/

    @Test
    @DisplayName("Sollte Lieferanten aktualisieren")
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
        assertEquals("Neu", result.getName(), "Nazwa po aktualizacji");
        assertEquals("neu@example.de", result.getEmail(), "E-mail po aktualizacji");
        verify(supplierRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Sollte Aktualisierung mit gleicher E-Mail erlauben")
    void update_ShouldAllowSameEmail() {
        // 1. SETUP - taki sam adres e-mail, jaki ma juz ten dostawca
        Supplier existing = supplier(1L, "Alt", "alt@example.de");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        Supplier updated = supplier(null, "Neu", "alt@example.de"); // taki sam e-mail
        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ACT & 3. ASSERT - nie moze rzucic wyjatku o duplikacie e-maila
        assertDoesNotThrow(() -> service.update(1L, updated));
    }

    @Test
    @DisplayName("Sollte IllegalStateException werfen, wenn neue E-Mail bereits belegt ist")
    void update_ShouldThrowWhenNewEmailTakenByOther() {
        // 1. SETUP - inny dostawca juz ma ten adres e-mail
        Supplier existing = supplier(1L, "Alt", "alt@example.de");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        Supplier updated = supplier(null, "Neu", "besetzt@example.de");
        when(supplierRepository.findByEmail("besetzt@example.de"))
                .thenReturn(Optional.of(supplier(2L, "Inhaber", "besetzt@example.de")));

        // 2. ACT & 3. ASSERT - nowy e-mail inny od obecnego i juz zajety => wyjatek
        Exception ex = assertThrows(IllegalStateException.class,
                () -> service.update(1L, updated));

        assertTrue(ex.getMessage().contains("jest już zajęty"),
                "Komunikat musi informowac o zajetym adresie e-mail");

        // 4. VERIFY - nic nie moglo zostac zapisane
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    /** delete **/

    @Test
    @DisplayName("Sollte Lieferanten loeschen")
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
    @DisplayName("Sollte EntityNotFoundException werfen beim Loeschen nieistniejacego")
    void delete_ShouldThrowWhenMissing() {
        // 1. SETUP
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(EntityNotFoundException.class, () -> service.delete(99L));

        // 4. VERIFY - delete nie mogl zostac wywolany
        verify(supplierRepository, never()).deleteById(99L);
    }
}
