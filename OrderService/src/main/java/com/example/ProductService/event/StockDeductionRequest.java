package com.example.ProductService.event;

public record StockDeductionRequest(
        Long productId,
        Integer quantity
) {}
