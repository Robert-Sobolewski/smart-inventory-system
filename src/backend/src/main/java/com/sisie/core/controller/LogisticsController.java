package com.sisie.core.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.dto.IssueRequest;
import com.sisie.core.dto.ReceiveRequest;
import com.sisie.core.service.InventoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/logistics")
@PreAuthorize("hasRole('MANAGER')") // Default security: Only Managers can access logistics by default
@RequiredArgsConstructor
public class LogisticsController {

	private final InventoryService inventoryService;

	/**
	 * Wareneingang - receive products
	 * 
	 * @param sku - product id
	 * @param loc - location id
	 * @param qty - quantity
	 * @return
	 */
	@PostMapping("/receive")
	/**
	 * Wareneingang - receive products into the warehouse
	 * 
	 * @param request DTO containing SKU, Location and Quantity
	 * @return ResponseEntity with 201 Created on success
	 */
	public ResponseEntity<String> receive(@RequestBody ReceiveRequest request) {

		inventoryService.receiveGoods(request.getSku(), request.getLoc(), request.getQty());

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(String.format("Wareneingang fuer SKU %s erfolgreich gebucht!", request.getSku()));

	}

	/**
	 * Warenausgang (FIFO) - issue products from warehouse
	 * 
	 * @param request
	 * @return
	 */
	@PostMapping("/issue")
	public ResponseEntity<String> issue(@RequestBody IssueRequest request) {

		inventoryService.issueGoods(request.getSku(), request.getQty());

		return ResponseEntity
				.ok(String.format("Warenausgang (FIFO) fuer SKU: %s erfolgreich durchgefuert!", request.getSku()));

	}

	/**
	 * Gesamtbestand fuer product
	 * 
	 * @param sku - product id
	 * @return
	 */
    @PreAuthorize("hasAnyRole('WORKER','MANAGER','ADMIN')") // changet to hasAnyAuthority
	@GetMapping("/stock/{sku}")
	public ResponseEntity<String> getStock(@PathVariable String sku) {
		
		Integer stock = inventoryService.getTotalStock(sku);
		
		return ResponseEntity.ok(String.format("Gesamtbestand fuer %s : %d Stueck", sku,stock));
	}

}
