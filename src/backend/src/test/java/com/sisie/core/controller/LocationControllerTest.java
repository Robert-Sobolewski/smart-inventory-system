package com.sisie.core.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sisie.core.dto.LocationUpdateRequest;
import com.sisie.core.model.Location;
import com.sisie.core.repository.LocationRepository;
import com.sisie.core.repository.StockBatchRepository;
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

	// new dependency of the controller (delete guard) -> must be mocked for ALL tests
	@MockitoBean
	private StockBatchRepository stockBatchRepository;

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

	// --- get all locations function ---

	@Test
	@WithMockUser(authorities = "ROLE_WORKER")
	@DisplayName("Should return all locations when user has ROLE_WORKER")
	void getAll_ShouldReturnAllLocationsWhenUserHasRoleWorker() throws Exception {

		// Setup
		List<Location> mockLocations = List.of(
				Location.builder().id(1L).locationCode("A-1-01").build(),
				Location.builder().id(2L).locationCode("B-2-05").build());

		when(locationRepository.findAll()).thenReturn(mockLocations);

		// Act & Assert
		mockMvc.perform(get("/api/locations")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].locationCode").value("A-1-01"))
				.andExpect(jsonPath("$[1].locationCode").value("B-2-05"));

		verify(locationRepository, times(1)).findAll();
	}

	@Test
	@WithMockUser(authorities = "ROLE_MANAGER")
	@DisplayName("Should return empty list when no locations exist")
	void getAll_ShouldReturnEmptyListWhenNoLocationsExist() throws Exception {

		// Setup
		when(locationRepository.findAll()).thenReturn(List.of());

		// Act & Assert
		mockMvc.perform(get("/api/locations")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		verify(locationRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("Should return bad request when user has no required role")
	@WithMockUser(authorities = "ROLE_USER") // user without permission
	void getAll_Forbidden() throws Exception {

		// Act & Assert
		mockMvc.perform(get("/api/locations")).andExpect(status().isBadRequest());

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

	// --- update location function (PUT /api/locations/{id}) ---

	@Test
	@DisplayName("Should return bad request when user has no required role")
	@WithMockUser(authorities = "ROLE_USER") // user without permission
	void update_Forbidden() throws Exception {

		// Setup
		Long id = 1L;
		String locationCode = "A-1-99";
		LocationUpdateRequest mockLocationRequest = LocationUpdateRequest.builder().locationCode(locationCode).build();

		mockMvc.perform(put("/api/locations/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest());

		verifyNoInteractions(locationRepository);
	}

	@Test
	@DisplayName("Should return bad request when locationCode is null")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void update_ShouldReturnExceptionWhenLocationCodeIsNull() throws Exception {

		// Setup
		Long id = 1L;
		LocationUpdateRequest mockLocationRequest = LocationUpdateRequest.builder().locationCode(null).build();

		mockMvc.perform(put("/api/locations/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("locationCode can't be null or empty"));

		verifyNoInteractions(locationRepository);
	}

	@Test
	@DisplayName("Should return bad request when locationCode is blank")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void update_ShouldReturnExceptionWhenLocationCodeIsBlank() throws Exception {

		// Setup
		Long id = 1L;
		LocationUpdateRequest mockLocationRequest = LocationUpdateRequest.builder().locationCode("   ").build();

		mockMvc.perform(put("/api/locations/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("locationCode can't be null or empty"));

		verifyNoInteractions(locationRepository);
	}

	@Test
	@DisplayName("Should return bad request when location with id does not exist")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void update_ShouldReturnExceptionWhenLocationDoesNotExist() throws Exception {

		// Setup
		Long id = 99L;
		String locationCode = "A-1-99";
		LocationUpdateRequest mockLocationRequest = LocationUpdateRequest.builder().locationCode(locationCode).build();

		when(locationRepository.findById(id)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/locations/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Lagerplatz mit ID 99 existiert nicht!"));

		verify(locationRepository, times(1)).findById(id);
	}

	@Test
	@DisplayName("Should return bad request when new code is already used by another location")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void update_ShouldReturnExceptionWhenCodeAlreadyUsedByOtherLocation() throws Exception {

		// Setup
		Long id = 1L;
		String newCode = "B-2-05"; // belongs to another location (id=2)
		LocationUpdateRequest mockLocationRequest = LocationUpdateRequest.builder().locationCode(newCode).build();

		when(locationRepository.findById(id)).thenReturn(Optional.of(Location.builder().id(1L).locationCode("A-1-01").build()));
		when(locationRepository.findByLocationCode(newCode))
				.thenReturn(Optional.of(Location.builder().id(2L).locationCode(newCode).build()));

		mockMvc.perform(put("/api/locations/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Lagerplatz B-2-05 existiert bereits!"));

		verify(locationRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should successfully update location code")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void update_ShouldSuccessfullyUpdateLocation() throws Exception {

		// Setup
		Long id = 1L;
		String newCode = "C-3-07";
		Location existingLocation = Location.builder().id(id).locationCode("A-1-01").build();
		LocationUpdateRequest mockLocationRequest = LocationUpdateRequest.builder().locationCode(newCode).build();

		when(locationRepository.findById(id)).thenReturn(Optional.of(existingLocation));
		when(locationRepository.findByLocationCode(newCode)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/locations/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isOk())
				.andExpect(content().string("Lagerplatz ID 1 wurde erfolgreich aktualisiert."));

		// entity must carry the new code before save
		assertEquals(newCode, existingLocation.getLocationCode());
		verify(locationRepository, times(1)).save(existingLocation);
	}

	@Test
	@DisplayName("Should allow keeping the same code (no false duplicate)")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void update_ShouldAllowKeepingSameCode() throws Exception {

		// Setup: PUT with the SAME code -> findByLocationCode finds itself, must NOT fail
		Long id = 1L;
		String sameCode = "A-1-01";
		Location existingLocation = Location.builder().id(id).locationCode(sameCode).build();
		LocationUpdateRequest mockLocationRequest = LocationUpdateRequest.builder().locationCode(sameCode).build();

		when(locationRepository.findById(id)).thenReturn(Optional.of(existingLocation));
		when(locationRepository.findByLocationCode(sameCode)).thenReturn(Optional.of(existingLocation));

		mockMvc.perform(put("/api/locations/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(mockLocationRequest))).andExpect(status().isOk())
				.andExpect(content().string("Lagerplatz ID 1 wurde erfolgreich aktualisiert."));

		verify(locationRepository, times(1)).save(existingLocation);
	}

	// --- delete location function (DELETE /api/locations/{id}) ---

	@Test
	@DisplayName("Should return bad request when user has no required role")
	@WithMockUser(authorities = "ROLE_USER") // user without permission
	void delete_Forbidden() throws Exception {

		// Act & Assert
		mockMvc.perform(delete("/api/locations/1")).andExpect(status().isBadRequest());

		verifyNoInteractions(locationRepository);
		verifyNoInteractions(stockBatchRepository);
	}

	@Test
	@DisplayName("Should return bad request when location with id does not exist")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void delete_ShouldReturnExceptionWhenLocationDoesNotExist() throws Exception {

		// Setup
		Long id = 99L;
		when(locationRepository.findById(id)).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/locations/" + id)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Lagerplatz mit ID 99 existiert nicht!"));

		verifyNoInteractions(stockBatchRepository);
	}

	@Test
	@DisplayName("Should NOT delete location when stock batches are still on it (guard)")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void delete_ShouldNotDeleteWhenStockBatchesExist() throws Exception {

		// Setup: location exists, but 2 batches lie on it
		Long id = 1L;
		Location existingLocation = Location.builder().id(id).locationCode("A-1-01").build();

		when(locationRepository.findById(id)).thenReturn(Optional.of(existingLocation));
		when(stockBatchRepository.countByLocation(existingLocation)).thenReturn(2L);

		mockMvc.perform(delete("/api/locations/" + id)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Lagerplatz ID 1 kann nicht geloescht werden, da dort noch 2 StockBatch(es) liegen!"));

		verify(locationRepository, never()).delete(any());
	}

	@Test
	@DisplayName("Should successfully delete location when no stock batches exist on it")
	@WithMockUser(authorities = "ROLE_MANAGER")
	void delete_ShouldSuccessfullyDeleteEmptyLocation() throws Exception {

		// Setup: location exists and is empty (no batches)
		Long id = 1L;
		Location existingLocation = Location.builder().id(id).locationCode("A-1-01").build();

		when(locationRepository.findById(id)).thenReturn(Optional.of(existingLocation));
		when(stockBatchRepository.countByLocation(existingLocation)).thenReturn(0L);

		mockMvc.perform(delete("/api/locations/" + id)).andExpect(status().isOk())
				.andExpect(content().string("Lagerplatz ID 1 wurde erfolgreich geloescht."));

		verify(locationRepository, times(1)).delete(existingLocation);
	}

}
