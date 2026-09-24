package com.sisie.core.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private HttpServletRequest request;

	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	@BeforeEach
	public void setUp() {
		jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
		// clear security context
		SecurityContextHolder.clearContext();
	}

	// doFilterInternal function
	@Test
	@DisplayName("Should authenticate user when valid Bearer token is provided")
	void doFilterInternal_ShouldExtractTokenSuccessfuly() throws ServletException, IOException {

		// Setup
		String token = "valid-jwt-token";
		String bearerToken = "Bearer " + token;
		String username = "Tom";

		when(request.getHeader("Authorization")).thenReturn(bearerToken);
		when(jwtTokenProvider.validateToken(token)).thenReturn(true);
		when(jwtTokenProvider.getUsernameFromJWT(token)).thenReturn(username);

		// Act
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Assert
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());

		// check if filter not crash
		verify(filterChain).doFilter(request, response);

	}

	@Test
	@DisplayName("Should not authenticate when header is missing")
	void doFilterInternal_ShouldNotAuthenticateWhenHeaderIsMissing() throws ServletException, IOException {

		// Setup
		when(request.getHeader("Authorization")).thenReturn("Wrong-format Token");

		// Act
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Assert
		// context should be empty
		assertNull(SecurityContextHolder.getContext().getAuthentication());

		verify(filterChain).doFilter(request, response);

	}

	@Test
	@DisplayName("Should NOT authenticate when header is missing")
	void doFilterInternal_NoHeader_NoAuth() throws ServletException, IOException {

		// Setup
		when(request.getHeader("Authorization")).thenReturn(null);

		// Act
		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		// Assert
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

}
