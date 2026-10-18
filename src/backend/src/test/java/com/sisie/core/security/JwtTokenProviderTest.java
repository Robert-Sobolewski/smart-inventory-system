package com.sisie.core.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;
	private final String secret = "mySecretKeyForTestingPurposesOnly1234567890123456";
	private final long expiration = 3600000L; // 1 hour

	@BeforeEach
	void setUp() throws Exception {
		jwtTokenProvider = new JwtTokenProvider();

		// inject into private field @Value
		ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
		ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", expiration);
	}

	// getSigningKey function

	@Test
	@DisplayName("Should generate a valid token and extract correct username")
	void getKey_ShouldGenerateValidTokenAndExtractCorrectUsername() throws Exception {

		// Setup
		String username = "Tom";
		String role = "ROLE_ADMIN";

		// Act
		String token = jwtTokenProvider.generateToken(username, role);
		String extractUsername = jwtTokenProvider.getUsernameFromJWT(token);

		// Assert
		assertNotNull(token);
		assertEquals(username, extractUsername);

	}

	@Test
	@DisplayName("Should return true for a valid token")
	void validateToken_Success() {

		// Setup
		String token = jwtTokenProvider.generateToken("Tom", "ROLE_USER");

		// Act & Assert
		assertTrue(jwtTokenProvider.validateToken(token));
	}

	@Test
	@DisplayName("Should return false for a tampered or invalid token")
	void validateToken_Invalid() {

		// Setup
		String invalidToken = "this.is.not.a.real.jwt.token";

		// Act & Assert
		assertFalse(jwtTokenProvider.validateToken(invalidToken));
	}

	@Test
	@DisplayName("Should return false when token is expired")
	void validateToken_Expired() {

		// Setup - token expired with negative value
		ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", -1000L);
		String token = jwtTokenProvider.generateToken("Tom", "ROLE_USER");

		// Act & Assert
		assertFalse(jwtTokenProvider.validateToken(token));
	}
	
	@Test
	@DisplayName("Should return correct role from valid JWT")
	void getRoleFromJWT_Success() {
		
		// Setup token with role MANAGER
		SecretKey testkey= Keys.hmacShaKeyFor(secret.getBytes());
		String token = Jwts.builder()
				.setSubject("user123")
				.claim("role", "ROLE_MANAGER")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis()+3600000))
				.signWith(testkey)
				.compact();
		
		//act
		String role = jwtTokenProvider.getRoleFromJWT(token);
		
		// assert
		assertEquals("ROLE_MANAGER", role,"Role in token should match ");
	}

}
