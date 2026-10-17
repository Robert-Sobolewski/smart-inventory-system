package com.sisie.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisie.core.model.Supplier;
import com.sisie.core.security.JwtTokenProvider;

/**
 * Testy warstwy HTTP SupplierController (bez kontekstu calej aplikacji,
 * serwis zamieniony na Mocka).
 */
@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc(addFilters = true)
@EnableMethodSecurity
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * JwtAuthenticationFilter (właczony przez addFilters=true) wymaga tego beanu,
     * ale w slajsie @WebMvcTest nie jest skanowany - wiec mockujemy go jak w
     * innych testach kontrolerow.
     */
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.sisie.core.service.SupplierService supplierService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /* ---------- getAllSuppliers ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_WORKER")
    @DisplayName("Sollte alle Lieferanten als JSON-Liste zurueckgeben")
    void getAll_ShouldReturnSupplierList() throws Exception {
        // 1. SETUP
        Supplier a = com.sisie.core.model.Supplier.builder()
                .id(1L).name("Lieferant A").email("a@example.de").build();
        Supplier b = com.sisie.core.model.Supplier.builder()
                .id(2L).name("Lieferant B").email("b@example.de").build();

        when(supplierService.getAllSuppliers()).thenReturn(List.of(a, b));

        // 2. ACT & ASSERT
        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(objectMapper.writeValueAsString(List.of(a, b))));

        verify(supplierService, times(1)).getAllSuppliers();
    }

    /* ---------- findById ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte bestehenden Lieferanten nach ID als JSON zurueckgeben")
    void getById_ShouldReturnExistingSupplier() throws Exception {
        // 1. SETUP
        Supplier existing = com.sisie.core.model.Supplier.builder()
                .id(1L).name("Lieferant A").email("a@example.de").build();

        when(supplierService.findById(eq(1L))).thenReturn(existing);

        // 2. ACT & ASSERT
        mockMvc.perform(get("/api/suppliers/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(objectMapper.writeValueAsString(existing)));

        verify(supplierService, times(1)).findById(eq(1L));
    }

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte 400 liefern, wenn Lieferant nicht existiert (EntityNotFoundException)")
    void getById_ShouldReturnBadRequestWhenMissing() throws Exception {
        // 1. SETUP
        when(supplierService.findById(eq(99L)))
                .thenThrow(new EntityNotFoundException(
                        "Dostawca z ID 99 nie został znaleziony"));

        // 2. ACT & ASSERT
        mockMvc.perform(get("/api/suppliers/{id}", 99L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Dostawca z ID 99 nie został znaleziony"));

        verify(supplierService, times(1)).findById(eq(99L));
    }

    /* ---------- create ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte neuen Lieferanten anlegen und Bestätigung zurückgeben")
    void create_ShouldCreateNewSupplier() throws Exception {
        // 1. SETUP
        com.sisie.core.model.Supplier neu =
                com.sisie.core.model.Supplier.builder()
                        .name("Neu").email("neu@example.de").build();

        Supplier created = com.sisie.core.model.Supplier.builder()
                .id(7L).name("Neu").email("neu@example.de").build();
        when(supplierService.create(any(com.sisie.core.model.Supplier.class)))
                .thenReturn(created);

        // 2. ACT & ASSERT
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(neu)))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("Lieferant Neu erfolgreich angelegt (ID: 7)"));

        verify(supplierService, times(1))
                .create(any(com.sisie.core.model.Supplier.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte 400 liefern, wenn E-Mail już istnieje (IllegalStateException)")
    void create_ShouldReturnBadRequestWhenEmailExists() throws Exception {
        // 1. SETUP
        com.sisie.core.model.Supplier neu =
                com.sisie.core.model.Supplier.builder()
                        .name("Neu").email("bestehend@example.de").build();

        when(supplierService.create(any(com.sisie.core.model.Supplier.class)))
                .thenThrow(new IllegalStateException(
                        "Dostawca z adresem e-mail bestehend@example.de już istnieje"));

        // 2. ACT & ASSERT
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(neu)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Dostawca z adresem e-mail bestehend@example.de już istnieje"));

        verify(supplierService, times(1))
                .create(any(com.sisie.core.model.Supplier.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_WORKER")
    @DisplayName("Sollte 400 liefern, wenn Nutzer keine MANAGER/ADMIN-Rolle hat (POST)")
    void create_ShouldReturnBadRequestWithoutManagerRole() throws Exception {
        // 1. SETUP - bez stubu: @PreAuthorize odrzuca request PRZED serwisem,
        // wiec supplierService w ogole nie zostanie wywolany

        // 2. ACT & ASSERT
        // Uwaga: GlobalExceptionHandler mapuje kazdy RuntimeException (takze
        // AccessDeniedException) na 400 Bad Request - to konwencja tego projektu
        // (porownaj ProductControllerTest.test_ShouldReturnBadRequestWehnUserHasNoPermission).
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Neu\",\"email\":\"neu@example.de\"}"))
                .andExpect(status().isBadRequest());

        verify(supplierService, never()).create(any(com.sisie.core.model.Supplier.class));
    }

    /* ---------- update ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte bestehenden Lieferanten aktualisieren und JSON zurückgeben")
    void update_ShouldUpdateExistingSupplier() throws Exception {
        // 1. SETUP
        com.sisie.core.model.Supplier updated =
                com.sisie.core.model.Supplier.builder()
                        .name("Neu").email("neu@example.de").build();

        Supplier saved = com.sisie.core.model.Supplier.builder()
                .id(3L).name("Neu").email("neu@example.de").build();
        when(supplierService.update(eq(3L), any(com.sisie.core.model.Supplier.class)))
                .thenReturn(saved);

        // 2. ACT & ASSERT
        mockMvc.perform(put("/api/suppliers/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(objectMapper.writeValueAsString(saved)));

        verify(supplierService, times(1))
                .update(eq(3L), any(com.sisie.core.model.Supplier.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte 400 liefern, gdy nowy e-mail już zajęty (IllegalStateException)")
    void update_ShouldReturnBadRequestWhenEmailTaken() throws Exception {
        // 1. SETUP
        com.sisie.core.model.Supplier updated =
                com.sisie.core.model.Supplier.builder()
                        .name("Neu").email("besetzt@example.de").build();

        when(supplierService.update(eq(3L), any(com.sisie.core.model.Supplier.class)))
                .thenThrow(new IllegalStateException(
                        "Adres e-mail besetzt@example.de jest już zajęty przez innego dostawcę"));

        // 2. ACT & ASSERT
        mockMvc.perform(put("/api/suppliers/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Adres e-mail besetzt@example.de jest już zajęty przez innego dostawcę"));
    }

    /* ---------- delete ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte Lieferanten löschen und Bestätigung zurückgeben")
    void delete_ShouldDeleteExistingSupplier() throws Exception {
        // 1. SETUP & ACT & ASSERT
        mockMvc.perform(delete("/api/suppliers/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("Lieferant mit ID 5 geloescht"));

        verify(supplierService, times(1)).delete(eq(5L));
    }

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Sollte 400 liefern beim Löschen nieistniejącego dostawcy (EntityNotFoundException)")
    void delete_ShouldReturnBadRequestWhenMissing() throws Exception {
        // 1. SETUP
        long supplierId = 99L;
        
        // Poprawna składnia dla metody void:
        doThrow(new EntityNotFoundException(String.format("Supplier with ID %d not found", supplierId)))
                .when(supplierService).delete(supplierId); 
                // Zauważ: .when(supplierService) - tutaj jest tylko obiekt, a .delete(supplierId) jest na końcu

        // 2. ACT & ASSERT
        mockMvc.perform(delete("/api/suppliers/{id}", supplierId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Supplier with ID 99 not found")); // Upewnij się, że string w jsonPath zgadza się z tym w EntityNotFoundException

        verify(supplierService, times(1)).delete(supplierId);
    }

}
