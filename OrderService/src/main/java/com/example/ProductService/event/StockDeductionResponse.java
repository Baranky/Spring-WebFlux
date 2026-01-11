package com.example.ProductService.event;

public record StockDeductionResponse(
        Long orderId,
        Long productId,
        Integer quantity,
        Boolean success,
        String message
) {}
