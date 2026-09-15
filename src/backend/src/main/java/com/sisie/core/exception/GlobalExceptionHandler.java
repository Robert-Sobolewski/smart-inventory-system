package com.sisie.core.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Globaler Fehler-Handler fuer die gesamte Anwendung.
 * Verhindert genrische 500er Fehler und gibt praezise Informationen zurueck.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e){
		
		Map<String, Object> body = new HashMap<>();
		body.put("timestamp", java.time.LocalDateTime.now());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Business Logic Error");
		body.put("message", e.getMessage());
		
		return new ResponseEntity<>(body,HttpStatus.BAD_REQUEST);
	}
}
