package com.example.ProductService.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class InventoryClient {

    private final WebClient webClient;
    private static final String SERVICE_NAME = "inventory-service";

    public InventoryClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://" + SERVICE_NAME)
                .build();
    }

    public Mono<Void> deductStock(Long productId, Integer quantity) {
        return webClient.post()
                .uri("/api/inventory/deduct")
                .bodyValue(Map.of("productId", productId, "quantity", quantity))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Stok düşürülürken hata: productId={}, quantity={}, error={}",
                productId, quantity, error.getMessage()))
                .onErrorMap(error -> new RuntimeException("Stok düşürülemedi: " + error.getMessage()));
    }
}
