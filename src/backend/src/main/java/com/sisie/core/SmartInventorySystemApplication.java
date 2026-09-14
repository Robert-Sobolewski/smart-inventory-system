package com.sisie.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartInventorySystemApplication {

	public static void main(String[] args) {
		// Dieser eine Befehl startet den eingebetteten Tomcat-Server 
        // und initialisiert den gesamten Spring-Kontext.
        SpringApplication.run(SmartInventorySystemApplication.class, args);
        
        System.out.println("====================================================");
        System.out.println("🚀 SIS-IE Backend ist gestartet!");
        System.out.println("👉 Teste die API unter: http://localhost:8080/api/products/test");
        System.out.println("====================================================");

	}

}
