package com.sisie.core.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseConfigTest {
	
	@Mock
	private DataSource dataSource;
	
	@Mock
	private Connection connection;
	
	@Mock
	private Statement statement;
	
	private DatabaseConfig dbConfig;
	
	@BeforeEach
	void setUp() {
		
		// create instance with mock dataSource
		dbConfig = new DatabaseConfig(dataSource);
	}
	
	@Test
	@DisplayName("Should execute PRAGMA foreign_keys=ON successfuly")
	void enableForeignKeysSuccess() throws Exception {
		
		// Setup
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		
		// Act
		dbConfig.enableForeignKeys();
		
		// Assert
		verify(statement,times(1)).execute("PRAGMA foreign_keys=ON;");
	}
	
	@Test
	@DisplayName("Should handle exception when DB connection fail")
	void enableForeignKeysException() throws Exception{
		
		// Setup
		when(dataSource.getConnection()).thenThrow(new RuntimeException("DB connection failed!"));
		
		// Act
		assertDoesNotThrow(()->dbConfig.enableForeignKeys(),"Method shoud catch exception not crash app");
		
		// Assert
		verify(connection,never()).createStatement();
	}



}
