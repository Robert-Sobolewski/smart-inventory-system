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
 * HTTP-layer tests for SupplierController (no full application context;
 * the service is replaced with a mock).
 */
@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc(addFilters = true)
@EnableMethodSecurity
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * JwtAuthenticationFilter (enabled via addFilters=true) requires this bean,
     * but it is not scanned in a @WebMvcTest slice - so we mock it like in the
     * other controller tests.
     */
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.sisie.core.service.SupplierService supplierService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /* ---------- getAllSuppliers ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_WORKER")
    @DisplayName("Should return all suppliers as a JSON list")
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
    @DisplayName("Should return an existing supplier by ID as JSON")
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
    @DisplayName("Should return 400 when the supplier does not exist (EntityNotFoundException)")
    void getById_ShouldReturnBadRequestWhenMissing() throws Exception {
        // 1. SETUP
        when(supplierService.findById(eq(99L)))
                .thenThrow(new EntityNotFoundException(
                        "Supplier with ID 99 not found"));

        // 2. ACT & ASSERT
        mockMvc.perform(get("/api/suppliers/{id}", 99L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Supplier with ID 99 not found"));

        verify(supplierService, times(1)).findById(eq(99L));
    }

    /* ---------- create ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Should create a new supplier and return a confirmation")
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
    @DisplayName("Should return 400 when the email already exists (IllegalStateException)")
    void create_ShouldReturnBadRequestWhenEmailExists() throws Exception {
        // 1. SETUP
        com.sisie.core.model.Supplier neu =
                com.sisie.core.model.Supplier.builder()
                        .name("Neu").email("bestehend@example.de").build();

        when(supplierService.create(any(com.sisie.core.model.Supplier.class)))
                .thenThrow(new IllegalStateException(
                        "A supplier with email address bestehend@example.de already exists"));

        // 2. ACT & ASSERT
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(neu)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A supplier with email address bestehend@example.de already exists"));

        verify(supplierService, times(1))
                .create(any(com.sisie.core.model.Supplier.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_WORKER")
    @DisplayName("Should return 400 when the user has no MANAGER/ADMIN role (POST)")
    void create_ShouldReturnBadRequestWithoutManagerRole() throws Exception {
        // 1. SETUP - no stub: @PreAuthorize rejects the request BEFORE the service,
        // so supplierService is never invoked at all

        // 2. ACT & ASSERT
        // Note: GlobalExceptionHandler maps every RuntimeException (including
        // AccessDeniedException) to 400 Bad Request - this is a convention of this project
        // (see ProductControllerTest.test_ShouldReturnBadRequestWehnUserHasNoPermission).
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Neu\",\"email\":\"neu@example.de\"}"))
                .andExpect(status().isBadRequest());

        verify(supplierService, never()).create(any(com.sisie.core.model.Supplier.class));
    }

    /* ---------- update ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Should update an existing supplier and return JSON")
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
    @DisplayName("Should return 400 when the new email is already taken (IllegalStateException)")
    void update_ShouldReturnBadRequestWhenEmailTaken() throws Exception {
        // 1. SETUP
        com.sisie.core.model.Supplier updated =
                com.sisie.core.model.Supplier.builder()
                        .name("Neu").email("besetzt@example.de").build();

        when(supplierService.update(eq(3L), any(com.sisie.core.model.Supplier.class)))
                .thenThrow(new IllegalStateException(
                        "Email address besetzt@example.de is already used by another supplier"));

        // 2. ACT & ASSERT
        mockMvc.perform(put("/api/suppliers/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Email address besetzt@example.de is already used by another supplier"));
    }

    /* ---------- delete ---------- */

    @Test
    @WithMockUser(authorities = "ROLE_MANAGER")
    @DisplayName("Should delete a supplier and return a confirmation")
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
    @DisplayName("Should return 400 when deleting a non-existent supplier (EntityNotFoundException)")
    void delete_ShouldReturnBadRequestWhenMissing() throws Exception {
        // 1. SETUP
        long supplierId = 99L;
        
        // Correct syntax for a void method:
        doThrow(new EntityNotFoundException(String.format("Supplier with ID %d not found", supplierId)))
                .when(supplierService).delete(supplierId); 
                // Note: .when(supplierService) - only the object here; .delete(supplierId) goes at the end

        // 2. ACT & ASSERT
        mockMvc.perform(delete("/api/suppliers/{id}", supplierId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Supplier with ID 99 not found")); // Make sure the string in jsonPath matches the one in EntityNotFoundException

        verify(supplierService, times(1)).delete(supplierId);
    }

}
