package com.example.InventoryService.controller;

import com.example.InventoryService.dto.StockDeductionRequest;
import com.example.InventoryService.entity.Inventory;
import com.example.InventoryService.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/inventory")
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Inventory> createInventory(@RequestBody Inventory inventory) {
        return inventoryService.createInventory(inventory);
    }

    @DeleteMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteInventoryByProductId(@PathVariable("productId") Long productId) {
        return inventoryService.deleteInventoryByProductId(productId);
    }

    @PostMapping("/deduct")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> deductStock(@RequestBody StockDeductionRequest request) {
        return inventoryService.deductStock(request);
    }
}
