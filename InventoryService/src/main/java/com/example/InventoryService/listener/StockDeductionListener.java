package com.example.InventoryService.listener;

import com.example.InventoryService.event.StockDeductionRequest;
import com.example.InventoryService.event.StockDeductionResponse;
import com.example.InventoryService.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StockDeductionListener {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, StockDeductionResponse> responseKafkaTemplate;

    public StockDeductionListener(InventoryService inventoryService,
                                  KafkaTemplate<String, StockDeductionResponse> responseKafkaTemplate) {
        this.inventoryService = inventoryService;
        this.responseKafkaTemplate = responseKafkaTemplate;
    }

    @KafkaListener(topics = "stock-deduction-request", groupId = "inventory-service-group",
            containerFactory = "stockDeductionRequestKafkaListenerContainerFactory")
    public void handleStockDeductionRequest(StockDeductionRequest request) {
        log.info("Stok düşürme isteği alındı: orderId={}, productId={}, quantity={}",
                request.orderId(), request.productId(), request.quantity());

        try {
            inventoryService.deductStock(
                    new com.example.InventoryService.dto.StockDeductionRequest(request.productId(), request.quantity())
            ).subscribe(
                    () -> {
                        StockDeductionResponse response = new StockDeductionResponse(
                                request.orderId(),
                                request.productId(),
                                request.quantity(),
                                true,
                                "Stok başarıyla düşürüldü"
                        );
                        responseKafkaTemplate.send("stock-deduction-response", response);
                        log.info("Stok düşürme başarılı, response gönderildi: orderId={}, productId={}, quantity={}",
                                request.orderId(), request.productId(), request.quantity());
                    },
                    error -> {
                        StockDeductionResponse response = new StockDeductionResponse(
                                request.orderId(),
                                request.productId(),
                                request.quantity(),
                                false,
                                error.getMessage()
                        );
                        responseKafkaTemplate.send("stock-deduction-response", response);
                        log.error("Stok düşürme başarısız, response gönderildi: orderId={}, productId={}, quantity={}, error={}",
                                request.orderId(), request.productId(), request.quantity(), error.getMessage());
                    }
            );
        } catch (Exception e) {
            StockDeductionResponse response = new StockDeductionResponse(
                    request.orderId(),
                    request.productId(),
                    request.quantity(),
                    false,
                    e.getMessage()
            );
            responseKafkaTemplate.send("stock-deduction-response", response);
            log.error("Stok düşürme işlenirken hata oluştu: orderId={}, productId={}, quantity={}, error={}",
                    request.orderId(), request.productId(), request.quantity(), e.getMessage(), e);
        }
    }
}
