package com.sisie.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SmartInventorySystemApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;
	
	@Test
	@DisplayName("Context should load successfully")
	void contextLoad() {
		
		// alle working fine or throw exception
		assertNotNull(applicationContext,"The application context should be not null");
	}
	
}
