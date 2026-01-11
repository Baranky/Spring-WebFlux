package com.example.InventoryService.event;

public record StockDeductionRequest(
        Long orderId,
        Long productId,
        Integer quantity
) {}
