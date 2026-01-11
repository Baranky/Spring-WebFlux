package com.example.ProductService.service;

import com.example.ProductService.client.InventoryClient;
import com.example.ProductService.dto.InventoryRequest;
import com.example.ProductService.dto.ProductRequest;
import com.example.ProductService.entity.Product;
import com.example.ProductService.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;

    public ProductService(ProductRepository productRepository, InventoryClient inventoryClient) {
        this.productRepository = productRepository;
        this.inventoryClient = inventoryClient;
    }

    public Flux<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Mono<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Mono<Product> createProduct(ProductRequest request) {
        Product newProduct = new Product();
        newProduct.setName(request.name());
        newProduct.setDescription(request.description());
        newProduct.setPrice(request.price());

        return productRepository.save(newProduct)
                .flatMap(savedProduct -> {
                    try {
                        InventoryRequest inventoryRequest = new InventoryRequest(savedProduct.getId(), request.stock());
                        return inventoryClient.createInventory(inventoryRequest)
                                .then(Mono.just(savedProduct))
                                .onErrorResume(e -> {
                                    log.error("Inventory oluşturulamadı, ürün siliniyor: productId={}", savedProduct.getId(), e);
                                    return productRepository.delete(savedProduct)
                                            .then(Mono.<Product>error(new RuntimeException("Ürün oluşturulamadı: " + e.getMessage())));
                                });
                    } catch (Exception e) {
                        log.error("Inventory request oluşturulamadı: productId={}", savedProduct.getId(), e);
                        return productRepository.delete(savedProduct)
                                .then(Mono.<Product>error(new RuntimeException("Ürün oluşturulamadı: " + e.getMessage())));
                    }
                });
    }

    public Mono<Product> updateProduct(Long id, ProductRequest request) {
        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    existingProduct.setName(request.name());
                    existingProduct.setDescription(request.description());
                    existingProduct.setPrice(request.price());
                    return productRepository.save(existingProduct);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Ürün bulunamadı: " + id)));
    }

    public Mono<Void> deleteProduct(Long id) {
        return productRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RuntimeException("Ürün bulunamadı: " + id));
                    }
                    return inventoryClient.deleteInventoryByProductId(id)
                            .onErrorResume(e -> {
                                log.warn("Inventory kaydı silinirken hata oluştu (devam ediliyor): productId={}", id, e);
                                return Mono.empty();
                            })
                            .then(productRepository.deleteById(id));
                });
    }
}
