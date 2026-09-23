package com.sisie.core.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
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
import com.sisie.core.dto.LocationCreateRequest;
import com.sisie.core.model.Location;
import com.sisie.core.repository.LocationRepository;
import com.sisie.core.security.JwtTokenProvider;

import jakarta.persistence.Entity;

@WebMvcTest(LocationController.class)
@AutoConfigureMockMvc(addFilters = true)
@EnableMethodSecurity
class LocationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private LocationRepository locationRepository;

	// -- test database function ---

	@Test
	@WithMockUser(authorities = "ROLE_WORKER")
	@DisplayName("Should return success message and count when user has ROLE_WORKER")
	void test_ShouldReturnTestDatabaseSuccessful() throws Exception {

		// Setup
		String mockLocationCode = "A-1-12";
		long mockCount = 5L;

		Location mockLocation = Location.builder().locationCode(mockLocationCode).build();
		when(locationRepository.count()).thenReturn(mockCount);

		// Act & Assert
		mockMvc.perform(get("/api/locations/test")).andExpect(status().isOk())
				.andExpect(content().string("Datenbank-Test erfolgreich! Aktuelle Anzahl an Lokationen: 5"));

		verify(locationRepository, times(1)).save(any());
	}

	@Test
	@DisplayName("Should return bad request when user has no required role")
	@WithMockUser(authorities = "ROLE_USER") // user without permission
	void testDatabase_Forbidden() throws Exception {
		mockMvc.perform(get("/api/locations/test")).andExpect(status().isBadRequest());

		verifyNoInteractions(locationRepository);
	}

	// --- create location function ---

	@Test
	@DisplayName("Should return bad request when user has no required role")
	@WithMockUser(authorities = "ROLE_USER") // user without permission
	void create_Forbiden() throws Exception {

		// Setup
		String locationCode = "A-1-11";
		LocationCreateRequest mockLocationRequest = LocationCreateRequest.builder().locationCode(locationCode).build();

		when(locationRepository.findByLocationCode(locationCode)).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/locations/create").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest());

		verifyNoInteractions(locationRepository);
	}

	@Test
	@DisplayName("Should return runtime exception when request locationCode is null")
	@WithMockUser(authorities = "ROLE_MANAGER") // user without permission
	void create_ShoulReturnExceptionWhenLocationCodeIsNull() throws Exception {

		// Setup
		String locationCode = null;
		LocationCreateRequest mockLocationRequest = LocationCreateRequest.builder().locationCode(locationCode).build();

		mockMvc.perform(post("/api/locations/create").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest())
				// extract message from json
				.andExpect(jsonPath("$.message").value("locationCode can't be null or empty"));

		verifyNoInteractions(locationRepository);
	}

	@Test
	@DisplayName("Should return runtime exception when request locationCode is blank")
	@WithMockUser(authorities = "ROLE_MANAGER") // user without permission
	void create_ShoulReturnExceptionWhenLocationCodeIsBlank() throws Exception {

		// Setup
		String locationCode = "";
		LocationCreateRequest mockLocationRequest = LocationCreateRequest.builder().locationCode(locationCode).build();

		mockMvc.perform(post("/api/locations/create").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest())
				// extract message from json
				.andExpect(jsonPath("$.message").value("locationCode can't be null or empty"));

		verifyNoInteractions(locationRepository);
	}

	@Test
	@DisplayName("Should return runtime exception when location exist")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void create_ShouldReturnExceptionWhenLocationExist() throws Exception {

		// Setup
		String mockLocation = "A-1-12";
		LocationCreateRequest mockLocationRequest = LocationCreateRequest.builder().locationCode(mockLocation).build();
		Location mockSearchLocation = Location.builder().locationCode(mockLocation).build();

		when(locationRepository.findByLocationCode(mockLocation)).thenReturn(Optional.of(mockSearchLocation));

		// Act & Assert

		mockMvc.perform(post("/api/locations/create").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Lagerplatz A-1-12 existiert bereits!"));

		verify(locationRepository, times(1)).findByLocationCode(anyString());

	}

	@Test
	@DisplayName("Should successfuly add new location")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void create_ShouldSuccessfulyAddNewLocation() throws Exception {

		// Setup
		String mockLocation = "A-1-22";
		Location mockSearchLocation = Location.builder().locationCode(mockLocation).build();
		LocationCreateRequest mockLocationRequest = LocationCreateRequest.builder().locationCode(mockLocation).build();

		when(locationRepository.findByLocationCode(mockLocation)).thenReturn(Optional.empty());

		// Act & Assert
		mockMvc.perform(post("/api/locations/create").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isOk())
				.andExpect(content().string(String.format("Lagerplatz %s wurde erfolgreich angelegt.", mockLocation)));

		verify(locationRepository, times(1)).save(any());

	}

}
