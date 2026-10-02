package com.sisie.core.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sisie.core.model.Role;
import com.sisie.core.model.User;
import com.sisie.core.repository.UserRepository;

@Configuration
public class DataInitializer {

	@Bean
	public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {

		return args -> {
			// Pruefen, ob bereits ein Admin existiert, damit wir nicht bei jedem Start
			// einen neuen anlegen
			if (userRepository.findByUsername("admin").isEmpty()) {

				// Erstellung eines Admin-Nutzers mit verschluesseltem Passwort!
				User admin = User.builder().username("admin").password(passwordEncoder.encode("Admin123!"))
						.role(Role.ROLE_ADMIN).build();
				userRepository.save(admin);

			} else {
				System.out.println("Admin already exist in database.");
			}

                    // Add user with role manager
                    if (userRepository.findByUsername("manager").isEmpty()) {

                        // Erstellung eines Admin-Nutzers mit verschluesseltem Passwort!
                        User manager = User.builder().username("manager").password(passwordEncoder.encode("Manager123!"))
                                .role(Role.ROLE_MANAGER).build();
                        userRepository.save(manager);

                    } else {
                        System.out.println("Manager already exist in database.");
                    }

                    // Add user with role worker
                    if (userRepository.findByUsername("worker").isEmpty()) {

                        // Erstellung eines Admin-Nutzers mit verschluesseltem Passwort!
                        User worker = User.builder().username("worker").password(passwordEncoder.encode("Worker123!"))
                                .role(Role.ROLE_WORKER).build();
                        userRepository.save(worker);

                    } else {
                        System.out.println("Worker already exist in database.");
                    }
		};
	}
}
