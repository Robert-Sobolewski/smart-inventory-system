package com.sisie.core.config;

import java.sql.Statement;
import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class DatabaseConfig {

	private final DataSource dataSource;

	DatabaseConfig(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Diese Methode wird einmalig nach dem Start der Anwendung ausgefuehrt.
	 * Sie stellt sicher, dass SQLite Foreign Keys erwingt.
	 */
	@PostConstruct
	public void enableForeignKeys() {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA foreign_keys=ON;");
		} catch (Exception e) {
			System.err.println("Failed to enable SQLite Foreign Keys: " + e.getMessage());
		}
	}
}
