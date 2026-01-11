package com.example.InventoryService.dto;

public record StockDeductionRequest(
        Long productId,
        Integer quantity
) {}
