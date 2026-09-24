package com.sisie.core.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sisie.core.model.Role;
import com.sisie.core.model.User;
import com.sisie.core.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
	
	@Mock
	private UserRepository userRepository;
	
	@Mock
	private PasswordEncoder passwordEncoder;
	
	private DataInitializer dataInitializer;
	
	@BeforeEach
	void setUp() {
		dataInitializer = new DataInitializer();
	}
	
	@Test
	@DisplayName("Should create admin user when no admin exist in DB")
	void initDatabase_ShouldCreateAdminUserWhenNotExistInDB() throws Exception{
		
		// Setup
		String username = "admin";
        String rawPassword = "Admin123!";
        String encodedPassword = "encoded_password_123";
               
        // admin not exist in DB
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        // encode password
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        
        // Act
        // create runner
        CommandLineRunner runner = dataInitializer.initDatabase(userRepository, passwordEncoder);
        runner.run();
        
        // Assert
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
		
	}
	
	@Test
	@DisplayName("Should not create admin user when admin already exist")
	void initDatabase_ShouldNotCreateAdminUserWhenExist() throws Exception{
		
		// Setup
		String username = "admin";
		User mockAdmin = User.builder().username(username).build();
		
		when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockAdmin));
		
		// Act
		CommandLineRunner runner = dataInitializer.initDatabase(userRepository, passwordEncoder);
		runner.run();
		
		// Assert
		verifyNoInteractions(passwordEncoder);
		verify(userRepository,never()).save(any(User.class));
	}

}
