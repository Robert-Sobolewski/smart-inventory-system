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
	 * The login process: check -> validate -> issue token.
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

		// 1. Look up the user in the database
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Invalid credentials"));

		// 2. If the user exists, check the password
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new RuntimeException("Invalid credentials");
		}

		// 3. If everything is OK, generate and return the token
		String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());

		return AuthResponse.builder().token(token).username(username).role(user.getRole().name()).build();
	}
}
