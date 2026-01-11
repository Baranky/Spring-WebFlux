package com.example.ProductService.client;

import com.example.ProductService.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ProductClient {

    private final WebClient webClient;
    private static final String SERVICE_NAME = "product-service";

    public ProductClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://" + SERVICE_NAME)
                .build();
    }

    public Mono<ProductDto> getProductById(Long id) {
        return webClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .bodyToMono(ProductDto.class)
                .doOnError(error -> log.error("Product servisine erişilemedi: productId={}, error={}", id, error.getMessage()))
                .onErrorMap(error -> new RuntimeException("Product servisine erişilemedi: " + error.getMessage()));
    }
}
