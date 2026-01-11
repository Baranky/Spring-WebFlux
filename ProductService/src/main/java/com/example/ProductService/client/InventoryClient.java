package com.example.ProductService.client;

import com.example.ProductService.dto.InventoryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    public Mono<Void> createInventory(InventoryRequest request) {
        return webClient.post()
                .uri("/api/inventory")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Inventory oluşturulurken hata: {}", error.getMessage()))
                .onErrorMap(error -> new RuntimeException("Inventory servisine erişilemedi: " + error.getMessage()));
    }

    public Mono<Void> deleteInventoryByProductId(Long productId) {
        return webClient.delete()
                .uri("/api/inventory/product/{productId}", productId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> log.error("Inventory silinirken hata: productId={}, error={}", productId, error.getMessage()))
                .onErrorMap(error -> new RuntimeException("Inventory servisine erişilemedi: " + error.getMessage()));
    }
}
