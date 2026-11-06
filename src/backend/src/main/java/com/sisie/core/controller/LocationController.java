package com.sisie.core.controller;

import java.util.List;
import java.util.Optional;

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

import com.sisie.core.dto.LocationCreateRequest;
import com.sisie.core.dto.LocationUpdateRequest;
import com.sisie.core.model.Location;
import com.sisie.core.repository.LocationRepository;
import com.sisie.core.repository.StockBatchRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class LocationController {

	private final LocationRepository locationRepository;
	private final StockBatchRepository stockBatchRepository;

	
	@GetMapping("/test")
    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
	public String testDatabase() {

		// Create a location, e.g. "A-01-12" (shelf A, level 1, slot 12)
		Location location = Location.builder().locationCode("A-01-12").build();

		// Save to the database
		locationRepository.save(location);

		// Read all locations back to verify it worked
		long count = locationRepository.count();
		return String.format("Datenbank-Test erfolgreich! Aktuelle Anzahl an Lokationen: %d", count);
	}

	/**
	 * Returns all storage locations as a list.
	 *
	 * @return List of locations
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')")
	public List<Location> getAllLocations() {
		return locationRepository.findAll();
	}

	@PostMapping("/create")
	public ResponseEntity<String> createLocation(@RequestBody LocationCreateRequest request) {
		
		if(request.getLocationCode() ==null || request.getLocationCode().isBlank())  {
			throw new RuntimeException("locationCode can't be null or empty");
		}

		// Check whether the code already exists
		Optional<Location> exists = locationRepository.findByLocationCode(request.getLocationCode());

		if (exists.isPresent()) {
			throw new RuntimeException("Storage location " + request.getLocationCode() + " already exists!");
		}

		Location loc = Location.builder().locationCode(request.getLocationCode()).build();

		locationRepository.save(loc);

		return ResponseEntity.ok("Lagerplatz " + request.getLocationCode() + " wurde erfolgreich angelegt.");
	}

	/**
	 * Updates the location code of an existing storage location.
	 *
	 * @param id      ID of the storage location
	 * @param request new locationCode
	 * @return success message
	 */
	@PutMapping("/{id}")
	public ResponseEntity<String> updateLocation(@PathVariable Long id, @RequestBody LocationUpdateRequest request) {

		if (request.getLocationCode() == null || request.getLocationCode().isBlank()) {
			throw new RuntimeException("locationCode can't be null or empty");
		}

		// Check whether the location exists at all
		Optional<Location> location = locationRepository.findById(id);

		if (location.isEmpty()) {
			throw new RuntimeException("Storage location with ID " + id + " does not exist!");
		}

		// Check whether the new code is already taken by ANOTHER location
		Optional<Location> duplicate = locationRepository.findByLocationCode(request.getLocationCode());

		if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
			throw new RuntimeException("Storage location " + request.getLocationCode() + " already exists!");
		}

		location.get().setLocationCode(request.getLocationCode());
		locationRepository.save(location.get());

		return ResponseEntity.ok("Lagerplatz ID " + id + " wurde erfolgreich aktualisiert.");
	}

	/**
	 * Deletes a storage location.
	 * Guard: a location that still has StockBatches must NOT be deleted.
	 *
	 * @param id ID of the storage location
	 * @return success message
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteLocation(@PathVariable Long id) {

		// Check whether the location exists at all
		Optional<Location> location = locationRepository.findById(id);

		if (location.isEmpty()) {
			throw new RuntimeException("Storage location with ID " + id + " does not exist!");
		}

		// Guard: no deletion while batches are still at this location
		long batchCount = stockBatchRepository.countByLocation(location.get());

		if (batchCount > 0) {
			throw new RuntimeException("Storage location ID " + id + " cannot be deleted because there are still "
					+ batchCount + " StockBatch(es) at it!");
		}

		locationRepository.delete(location.get());

		return ResponseEntity.ok("Lagerplatz ID " + id + " wurde erfolgreich geloescht.");
	}
}
