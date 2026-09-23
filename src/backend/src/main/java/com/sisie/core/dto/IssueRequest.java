package com.sisie.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO fuer den Warenausgang.
 * Trennt API von der internen Logik.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueRequest {

	private String sku;
	private Integer qty;
}
