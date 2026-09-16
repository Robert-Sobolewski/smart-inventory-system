package com.sisie.core.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
	public String login(String username, String rawPassword) {

		// 1. Nutzer in der DB suchen
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Nutzer nicht gefunden: " + username));

		// 2. Wenn user existieren , pruefen Passwort
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new RuntimeException("Falsches Passwort!");
		}

		// 3. Wenn alles ok ist , Token generieren und zurueckgeben
		return tokenProvider.generateToken(user.getUsername(), user.getRole().name());
	}
}
