package com.sisie.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO fuer create location
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationCreateRequest {
	
	private String locationCode;
}
