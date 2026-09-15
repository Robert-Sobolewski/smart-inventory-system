package com.sisie.core.dto;

import lombok.Data;

/**
 * DTO fuer den Wareneingang.
 * Trennt die API-Schnittstelle von der internen Logik.
 */
@Data
public class ReceiveRequest {

	private String sku;
	private String loc;
	private Integer qty;
}
