package com.sisie.core.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

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
import com.sisie.core.dto.ProductRequest;
import com.sisie.core.model.Product;
import com.sisie.core.repository.ProductRepository;
import com.sisie.core.security.JwtTokenProvider;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = true)
@EnableMethodSecurity
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private ProductRepository productRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// test database

	@Test
	@WithMockUser(authorities = "ROLE_WORKER")
	@DisplayName("Should return success message and count when user has ROLE_WORKER")
	void test_ShouldReturnTestDatabaseSuccessful() throws Exception {

		// Setup
		long mockCount = 10L;
		String mockSku = "SKU-123";

		Product mockProduct = Product.builder().sku(mockSku).build();
		when(productRepository.count()).thenReturn(mockCount);

		// Act & Assert

		mockMvc.perform(get("/api/products/test")).andExpect(status().isOk())
				.andExpect(content().string("Datenbank-Check OK! Anzahl Produkte: 10"));

		verify(productRepository, times(1)).count();
	}

	@Test
	@WithMockUser(authorities = "ROLE_USER")
	@DisplayName("Should return bad request when user has no required role")
	void test_ShouldReturnBadRequestWehnUserHasNoPermission() throws Exception {

		// Setup
		long mockCount = 10L;
		String mockSku = "SKU-123";

		Product mockProduct = Product.builder().sku(mockSku).build();
		when(productRepository.count()).thenReturn(mockCount);

		// Act & Assert

		mockMvc.perform(get("/api/products/test")).andExpect(status().isBadRequest());

		verifyNoInteractions(productRepository);
	}

	// getproduct

	@Test
	@WithMockUser(authorities = "ROLE_WORKER")
	@DisplayName("Should return bad request when SKU not exist.")
	void get_ShouldReturnBadRequestWhenSKUNotExist() throws Exception {

		// Setup
		String mockSku = "SKU-123";

		// Act & Assert
		mockMvc.perform(get("/api/products/{sku}", mockSku)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Product mit SKU: SKU-123 nicht gefunden!"));

		verify(productRepository, times(1)).findBySku(mockSku);
	}
	
	
	@Test
	@WithMockUser(authorities = "ROLE_MANAGER")
	@DisplayName("Should return existing product when user has ROLE_MANAGER")
	void get_ShouldReturnExistingProduct() throws Exception{
		
		// Setup
		String sku ="SIS-SEN-042";
		String name = "Sisie SmartNode_TepmHumidity";
		String category ="sensors";
		
		Product mockProduct =Product.builder()
				.sku(sku)
				.name(name)
				.category(category)
				.build();
		when(productRepository.findBySku(sku))
		.thenReturn(Optional.of(mockProduct));
		
		// Act & Assert
		mockMvc.perform(get("/api/products/{sku}",sku))
		.andExpect(status().isOk())
		.andExpect(content().json(objectMapper.writeValueAsString(mockProduct)));
		
		verify(productRepository,times(1)).findBySku(sku);
		
	}
	
	// create product
	
	@Test
	@WithMockUser(authorities = "ROLE_MANAGER")
	@DisplayName("Should return Bad request when SKU exist")
	void create_ShouldReturnBadRequestWhenProductWithSKUExist() throws Exception{
		
		// Setup
		String sku ="SIS-SEN-042";
		String name = "Sisie SmartNode_TepmHumidity";
		String category ="sensors";
		Product mockProduct = Product.builder()
				.sku(sku)
				.name(name)
				.category(category)
				.build();
		ProductRequest mockRequest = ProductRequest.builder()
				.sku(sku)
				.name(name)
				.category(category)
				.build();
		when(productRepository.findBySku(sku))
		.thenReturn(Optional.of(mockProduct));
		
		// Act & Assert
		mockMvc.perform(post("/api/products/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockRequest)))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.message").value("Product mit SKU SIS-SEN-042 existiert bereits!"));
		
		verify(productRepository,times(1)).findBySku(sku);
	}
	
	@Test
	@WithMockUser(authorities = "ROLE_MANAGER")
	@DisplayName("Should create new product when user is ROLE_MANAGER")
	void create_ShouldCreateNewProductWhenUserIsRoleManager() throws Exception{
		
		// Setup
		String sku ="SIS-SEN-042";
		String name = "Sisie SmartNode_TepmHumidity";
		String category ="sensors";
//		Product mockProduct = Product.builder()
//				.sku(sku)
//				.name(name)
//				.category(category)
//				.build();
		ProductRequest mockRequest = ProductRequest.builder()
				.sku(sku)
				.name(name)
				.category(category)
				.build();
		when(productRepository.findBySku(sku))
		.thenReturn(Optional.empty());
		
		// Act & Assert
		mockMvc.perform(post("/api/products/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockRequest)))
		.andExpect(status().isOk())
		.andExpect(content().string("Product SIS-SEN-042 erfolgreich angelegt!"));
		
		verify(productRepository,times(1)).findBySku(sku);
	}

}
