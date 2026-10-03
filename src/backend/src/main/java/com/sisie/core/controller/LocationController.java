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

		// Ein location erstellen, Beispiel: "A-01-12" (Regal A, Ebene 1, Fach 12)
		Location location = Location.builder().locationCode("A-01-12").build();

		// Speichern in die Datenbank
		locationRepository.save(location);

		// Alle Lokationene auslesen , um zu pruefen ob es geklappt hat
		long count = locationRepository.count();
		return String.format("Datenbank-Test erfolgreich! Aktuelle Anzahl an Lokationen: %d", count);
	}

	/**
	 * Liefert alle Lagerplaetze als Liste zurueck.
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

		// Prufen ob es sonst existiert
		Optional<Location> exists = locationRepository.findByLocationCode(request.getLocationCode());

		if (exists.isPresent()) {
			throw new RuntimeException("Lagerplatz " + request.getLocationCode() + " existiert bereits!");
		}

		Location loc = Location.builder().locationCode(request.getLocationCode()).build();

		locationRepository.save(loc);

		return ResponseEntity.ok("Lagerplatz " + request.getLocationCode() + " wurde erfolgreich angelegt.");
	}

	/**
	 * Aktualisiert den Lagerplatz-Code eines bestehenden Lagerplatzes.
	 *
	 * @param id      ID des Lagerplatzes
	 * @param request neuer locationCode
	 * @return success message
	 */
	@PutMapping("/{id}")
	public ResponseEntity<String> updateLocation(@PathVariable Long id, @RequestBody LocationUpdateRequest request) {

		if (request.getLocationCode() == null || request.getLocationCode().isBlank()) {
			throw new RuntimeException("locationCode can't be null or empty");
		}

		// Pruefen ob der Lagerplatz ueberhaupt existiert
		Optional<Location> location = locationRepository.findById(id);

		if (location.isEmpty()) {
			throw new RuntimeException("Lagerplatz mit ID " + id + " existiert nicht!");
		}

		// Prufen ob der neue Code von einem ANDEREN Lagerplatz belegt ist
		Optional<Location> duplicate = locationRepository.findByLocationCode(request.getLocationCode());

		if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
			throw new RuntimeException("Lagerplatz " + request.getLocationCode() + " existiert bereits!");
		}

		location.get().setLocationCode(request.getLocationCode());
		locationRepository.save(location.get());

		return ResponseEntity.ok("Lagerplatz ID " + id + " wurde erfolgreich aktualisiert.");
	}

	/**
	 * Loescht einen Lagerplatz.
	 * Guard: Ein Lagerplatz, an dem noch StockBatches liegen, darf NICHT geloescht werden.
	 *
	 * @param id ID des Lagerplatzes
	 * @return success message
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteLocation(@PathVariable Long id) {

		// Pruefen ob der Lagerplatz ueberhaupt existiert
		Optional<Location> location = locationRepository.findById(id);

		if (location.isEmpty()) {
			throw new RuntimeException("Lagerplatz mit ID " + id + " existiert nicht!");
		}

		// Guard: keine Loeschung, solange noch Batches an diesem Ort liegen
		long batchCount = stockBatchRepository.countByLocation(location.get());

		if (batchCount > 0) {
			throw new RuntimeException("Lagerplatz ID " + id + " kann nicht geloescht werden, da dort noch "
					+ batchCount + " StockBatch(es) liegen!");
		}

		locationRepository.delete(location.get());

		return ResponseEntity.ok("Lagerplatz ID " + id + " wurde erfolgreich geloescht.");
	}
}
