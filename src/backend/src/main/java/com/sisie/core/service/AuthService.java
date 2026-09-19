package com.sisie.core.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sisie.core.dto.AuthResponse;
import com.sisie.core.model.User;
import com.sisie.core.repository.UserRepository;
import com.sisie.core.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider tokenProvider;

	/**
	 * Der Login-Prozess: Pruefen -> Validieren -> Token ausstellen.
	 * 
	 * @param username
	 * @param rawPassword
	 * @return - token
	 */
	public AuthResponse login(String username, String rawPassword) {

		// Input Validation - Fail Fast
		if (username == null || username.isBlank() || rawPassword == null) {
			throw new IllegalArgumentException("Username and Password are required");
		}

		// 1. Nutzer in der DB suchen
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Invalid credentials"));

		// 2. Wenn user existieren , pruefen Passwort
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new RuntimeException("Invalid credentials");
		}

		// 3. Wenn alles ok ist , Token generieren und zurueckgeben
		String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());

		return AuthResponse.builder().token(token).username(username).role(user.getRole().name()).build();
	}
}
