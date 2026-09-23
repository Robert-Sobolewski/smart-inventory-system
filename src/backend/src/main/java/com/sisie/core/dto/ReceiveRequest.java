package com.sisie.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO fuer den Wareneingang.
 * Trennt die API-Schnittstelle von der internen Logik.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiveRequest {

	private String sku;
	private String loc;
	private Integer qty;
}
