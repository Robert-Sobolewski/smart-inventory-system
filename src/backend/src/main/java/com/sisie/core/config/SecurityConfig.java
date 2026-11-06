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
		config.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:8081"));

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
				// Disable CSRF (JWT is used, so cookies/sessions are not needed)
				.csrf(csrf -> csrf.disable())

				// Enable CORS with corsConfigurationSource
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// Insert the JWT filter before the standard username/password filter.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

				// Define the access rules
				.authorizeHttpRequests(auth -> auth
						// Allow everyone to access the login endpoints
						.requestMatchers("/api/auth/**").permitAll()
						// All other endpoints must be protected
						.anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No
				// no server-side sessions.
				);

		return http.build();
	}

}
