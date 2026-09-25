package com.sisie.core.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sisie.core.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Web-Sicherheit configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Ermoeglicht @PreAuthorize auf den Controllern.
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	/**
	 * Provide BCrypt functionality
	 * 
	 * @return
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {

		// industriestandard fuer Passwort-Hashing
		return new BCryptPasswordEncoder();
	}

	/**
	 * CORS definitions
	 */
	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration config = new CorsConfiguration();

		// erlaube nur bestimmte Domains
		config.setAllowedOrigins(Arrays.asList("http://localhost:4200"));

		// erlaubte Methoden
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

		// headers permit
		config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

		// accept for all endpoints
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	/**
	 * JWT Konfigurieren und erstellt den Zugriff erlaubnis.
	 * 
	 * @param http - http request
	 * @return
	 * @throws Exception
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
				// CSRF deaktivieren ( JWT wird benutzt, deshalb braucht man Cookies/ Sessions
				// nicht)
				.csrf(csrf -> csrf.disable())

				// CORS aktivieren mit corsConfigurationSource
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// Fuegen den Filter vor dem Standard-Username/Password Filter ein.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

				// Die Zugriffsregeln definieren
				.authorizeHttpRequests(auth -> auth
						// Erlaube allen den Zugriff auf die Login-Endpunkte
						.requestMatchers("/api/auth/**").permitAll()
						// Alle andere Endpunkte muessen geschuetzt werden
						.anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Keine
				// Keine Sessions auf dem Server.
				);

		return http.build();
	}

}
