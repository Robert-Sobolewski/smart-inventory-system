package com.sisie.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sisie.core.dto.IssueRequest;
import com.sisie.core.dto.ReceiveRequest;
import com.sisie.core.service.InventoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {
	
	private final InventoryService inventoryService;
	
	/**
	 * Wareneingang - receive products 
	 * @param sku - product id
	 * @param loc - location id
	 * @param qty - quantity
	 * @return
	 */
	@PostMapping("/receive")
	public String receive(@RequestBody ReceiveRequest request) {
		
		inventoryService.receiveGoods(request.getSku(), request.getLoc(), request.getQty());
		return "Wareneingang erfolgreich gebucht!";
	}
	
	/**
	 * Warenausgang (FIFO)
	 * @param sku - product id
	 * @param qty - quantity
	 * @return
	 */
	@PostMapping("/issue")
	public String issue(@RequestBody IssueRequest request) {
		
		inventoryService.issueGoods(request.getSku(), request.getQty());
		return "Warenausgang (FIFO) erfolgreich durchgefuert!";
	}
	
	/**
	 * Gesamtbestand fuer product
	 * @param sku - product id
	 * @return
	 */
	@GetMapping("/stock/{sku}")
	public String getStock(@PathVariable String sku) {
		
		return "Gesamtbestand fuer "+sku+" : "+inventoryService.getTotalStock(sku) +" Stueck.";
	}

}
