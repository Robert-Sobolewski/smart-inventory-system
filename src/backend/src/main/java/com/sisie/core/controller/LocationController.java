package com.sisie.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.model.Location;
import com.sisie.core.repository.LocationRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

	private final LocationRepository locationRepository;
	
	@GetMapping("/test")
	public String testDatabase() {
		
		// Ein location erstellen,  Beispiel: "A-01-12" (Regal A, Ebene 1, Fach 12)
		Location location = Location.builder()
				.locationCode("A-01-12")
				.build();
		
		// Speichern in die Datenbank
		locationRepository.save(location);
		
		// Alle Lokationene auslesen , um zu pruefen ob es geklappt hat
		long count = locationRepository.count();
		return String.format("Datenbank-Test erfolgreich! Aktuelle Anzahl an Lokationen: %d", count);
	}
}
