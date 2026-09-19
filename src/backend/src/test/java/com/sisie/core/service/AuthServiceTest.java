package com.sisie.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sisie.core.dto.AuthResponse;
import com.sisie.core.model.Role;
import com.sisie.core.model.User;
import com.sisie.core.repository.UserRepository;
import com.sisie.core.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider tokenProvider;

	@InjectMocks
	private AuthService authService;

	/** login function **/
	@Test
	@DisplayName("Should throw Exception when username is null")
	void login_ShouldThrowExceptionWhenUsernameIsNull() {
		// Assert
		var exception = assertThrows(IllegalArgumentException.class, () -> authService.login(null, "pass"));
		assertTrue(exception.getMessage().contains("Username and Password are required"));
	}

	@Test
	@DisplayName("Should throw Exception when useranem is empty")
	void login_ShouldThrowExceptionWhenUsernameIsEmpty() {
		// Assert
		var exception = assertThrows(IllegalArgumentException.class, () -> authService.login("  ", "pass"));
		assertTrue(exception.getMessage().contains("Username and Password are required"));
	}

	@Test
	@DisplayName("Should throw Exception when password is null")
	void login_ShouldThrowExceptionWhenPasswordIsNull() {
		var exception = assertThrows(IllegalArgumentException.class, () -> authService.login("user", null));
		assertTrue(exception.getMessage().contains("Username and Password are required"));

	}

	@Test
	@DisplayName("Should throw Exception when username not exist in DB")
	void login_ShouldThrowExceptionWhenUsernameNotExistInDB() {

		// Setup
		String username = "User-not-exist";

		when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

		// Act
		var exception = assertThrows(RuntimeException.class, () -> authService.login(username, "pass"));

		// Assert
		assertTrue(exception.getMessage().contains("Invalid credentials"));
	}

	@Test
	@DisplayName("Should throw Exception when password not match")
	void login_ShouldThrowExceptionWhenPasswordNotMatch() {

		// Setup
		String username = "username";
		String correctPassword = "Password";
		String incorrectPassword = "IncorectPassword";
		User mockUser = User.builder().username(username).password(correctPassword).build();

		when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

		// Act
		var exception = assertThrows(RuntimeException.class, () -> authService.login(username, incorrectPassword));

		// Assert
		assertTrue(exception.getMessage().contains("Invalid credentials"));

	}

	@Test
	@DisplayName("Should return correct Token")
	void login_ShouldReturnCorrectToken() {

		// Setup
		String username = "username";
		String password = "password";
		Role userRole = Role.ROLE_MANAGER;

		User mockUser = User.builder().username(username).password(password).role(userRole).build();
		when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
		when(passwordEncoder.matches(password, password)).thenReturn(true);

		// Act
		AuthResponse result = authService.login(username, password);

		// Assert
		assertAll("Verify returned results",
				() -> assertEquals(username, result.getUsername(), "Username should be the same."),
				() -> assertEquals(userRole.name(), result.getRole()));
	}

}
