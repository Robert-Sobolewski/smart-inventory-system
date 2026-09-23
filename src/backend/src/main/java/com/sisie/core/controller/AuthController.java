package com.sisie.core.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.dto.AuthResponse;
import com.sisie.core.dto.LoginRequest;
import com.sisie.core.service.AuthService;

import lombok.RequiredArgsConstructor;

/**
 * Erstellen Authorization endpoint
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/**
	 * Login endpoint
	 * 
	 * @param credentials
	 * @return
	 */
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {

		try {

			// validation
			if (request.getUsername() == null || request.getPassword() == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username and password are required");
			}

			AuthResponse resp = authService.login(request.getUsername(), request.getPassword());
			if (resp == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
			}

			// Wir geben den Token in einem JSON-Obiekt zurueck.
			return ResponseEntity.ok(resp);

		} catch (RuntimeException e) {

			// Wenn Login fehlschlaegt, senden wir die Fehlermeldung zurueck
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
		}
	}
}
