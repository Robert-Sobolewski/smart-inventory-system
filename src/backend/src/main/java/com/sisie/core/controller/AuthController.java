package com.sisie.core.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.dto.AuthResponse;
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
	 * @param credentials
	 * @return
	 */
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> credentials){
		
		try {
			String username = credentials.get("username");
			String password = credentials.get("password");
			
			AuthResponse resp = authService.login(username, password);
			
			// Wir geben den Token in einem JSON-Obiekt zurueck.
			return ResponseEntity.ok(Map.of("token",resp.getToken()));
			
		} catch (RuntimeException e) {
			
			// Wenn Login fehlschlaegt, senden wir die Fehlermeldung zurueck
			return ResponseEntity.status(401).body(e.getMessage());
		}
	}
}
