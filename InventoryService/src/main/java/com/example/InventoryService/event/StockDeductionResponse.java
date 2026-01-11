package com.example.InventoryService.event;

public record StockDeductionResponse(
        Long orderId,
        Long productId,
        Integer quantity,
        Boolean success,
        String message
) {}
