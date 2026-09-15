package com.sisie.core.dto;

import lombok.Data;

/**
 * DTO fuer den Warenausgang.
 * Trennt API von der internen Logik.
 */
@Data
public class IssueRequest {

	private String sku;
	private Integer qty;
}
