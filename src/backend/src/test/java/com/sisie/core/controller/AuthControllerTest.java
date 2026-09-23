package com.sisie.core.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisie.core.dto.AuthResponse;
import com.sisie.core.dto.LoginRequest;
import com.sisie.core.model.Role;
import com.sisie.core.model.User;
import com.sisie.core.security.JwtTokenProvider;
import com.sisie.core.service.AuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	// -- login api/auth/login

	@Test
	@DisplayName("Should return unauthorized when username not exist")
	void login_ShouldReturnUnauthorizedWhenUsernameNotExist() throws Exception {

		// Setup
		String notExistUsername = "user-123";
		String password = "whatever";

		// build login request
		LoginRequest loginRequest = LoginRequest.builder().username(notExistUsername).password(password).build();

		when(authService.login(anyString(), anyString())).thenThrow(new RuntimeException("Invalid credentials"));

		// Act & Assert
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isUnauthorized())
				.andExpect(content().string("Invalid credentials"));

	}

	@Test
	@DisplayName("Should return token when user successfully login")
	void login_ShouldReturnTokenWhenUserSuccessfullyLogin() throws Exception {

		// Setup
		String username = "Tom";
		String password = "Tom-Password";
		String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
		String expectedRole = Role.ROLE_WORKER.name();

		// build login request
		LoginRequest loginRequest = LoginRequest.builder().username(username).password(password).build();

		// build service response
		AuthResponse authResponse = AuthResponse.builder().token(expectedToken).username(username).role(expectedRole)
				.build();

		when(authService.login(username, password)).thenReturn(authResponse);

		// Act & Assert
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value(expectedToken)).andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.role").value(expectedRole));

		verify(authService, times(1)).login(username, password);
	}

	@Test
	@DisplayName("Should return bad request when username is null")
	void login_ShouldReturnBadRequestWhenUsernameIsNull() throws Exception {

		// Setup
		String username = null;
		String password = "password";

		LoginRequest loginRequest = LoginRequest.builder().username(username).password(password).build();

		when(authService.login(anyString(), anyString()))
				.thenThrow(new RuntimeException("Username and password are required"));

		// Act & Assert
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
				.andExpect(content().string("Username and password are required"));
	}
	
	@Test
	@DisplayName("Should return bad request when password is null")
	void login_ShouldReturnBadRequestWhenPasswordIsNull() throws Exception {

		// Setup
		String username = "username";
		String password = null;

		LoginRequest loginRequest = LoginRequest.builder().username(username).password(password).build();

		when(authService.login(anyString(), anyString()))
				.thenThrow(new RuntimeException("Username and password are required"));

		// Act & Assert
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isBadRequest())
				.andExpect(content().string("Username and password are required"));
	}

	@Test
	@DisplayName("Should return unauthorized when AuthResponse is null")
	void login_ShouldReturnUnauthorizedWhenAuthResponseIsNull() throws Exception {

		// Setup
		String mockUser = "user";
		String mockPassword = "password";

		LoginRequest loginRequest = LoginRequest.builder().username(mockUser).password(mockPassword).build();

		when(authService.login(anyString(), anyString())).thenReturn(null);

		// Act & Assert
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isUnauthorized())
				.andExpect(content().string("Invalid username or password"));
	}

}
