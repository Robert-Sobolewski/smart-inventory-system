package com.sisie.core.dto;

import lombok.Data;

@Data
public class ProductRequest {

	private String sku;
	private String name;
	private String category;
	private String description;
}
