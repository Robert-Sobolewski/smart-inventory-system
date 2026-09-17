package com.sisie.core.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.dto.LocationCreateRequest;
import com.sisie.core.model.Location;
import com.sisie.core.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@PreAuthorize("hasRole('ROLE_MANAGER')")
@RequiredArgsConstructor
public class LocationController {

	private final LocationRepository locationRepository;

	
	@GetMapping("/test")
	@PreAuthorize("hasAnyRole('ROLE_WORKER','ROLE_MANAGER','ROLE_ADMIN')")
	public String testDatabase() {

		// Ein location erstellen, Beispiel: "A-01-12" (Regal A, Ebene 1, Fach 12)
		Location location = Location.builder().locationCode("A-01-12").build();

		// Speichern in die Datenbank
		locationRepository.save(location);

		// Alle Lokationene auslesen , um zu pruefen ob es geklappt hat
		long count = locationRepository.count();
		return String.format("Datenbank-Test erfolgreich! Aktuelle Anzahl an Lokationen: %d", count);
	}

	@PostMapping("/create")
	public ResponseEntity<String> createLocation(@RequestBody LocationCreateRequest request) {

		// Prufen ob es sonst existiert
		Optional<Location> exists = locationRepository.findByLocationCode(request.getLoc());

		if (exists.isPresent()) {
			throw new RuntimeException("Lagerplatz " + request.getLoc() + " existiert bereits!");
		}

		Location loc = Location.builder().locationCode(request.getLoc()).build();

		locationRepository.save(loc);

		return ResponseEntity.ok("Lagerplatz " + request.getLoc() + " wurde erfolgreich angelegt.");
	}
}
