package com.sisie.core.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	// Get data from application.properties
	@Value("${sisie.jwt.secret}")
	private String jwtSecret;

	@Value("${sisie.jwt.expiration}")
	private long jwtExpiration;

	/**
	 * Hilfsmethode, um den String aus der Config in einen sicheren SecretKey fuer
	 * HMAC-SHA zu verwandeln.
	 * 
	 * @return SecretKey
	 */
	private SecretKey getSigningKey() {
		byte[] keyBytes = this.jwtSecret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * Erstellen ein JWT Token
	 * 
	 * @param username
	 * @param role
	 * @return - ein Token in String format
	 */
	public String generateToken(String username, String role) {
		return Jwts.builder().subject(username).claim("role", role).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtExpiration)).signWith(getSigningKey()).compact();
	}

	public String getUsernameFromJWT(String token) {

		Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

		return claims.getSubject();
	}

	/**
	 * Validate JWT token
	 * @param authToken
	 * @return
	 * @throws IllegalArgumentException | {@link JwtException}
	 */
	public boolean validateToken(String authToken) {
		try {
			Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

}
