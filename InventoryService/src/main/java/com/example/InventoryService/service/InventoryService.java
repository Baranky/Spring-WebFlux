package com.example.InventoryService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.InventoryService.dto.StockDeductionRequest;
import com.example.InventoryService.entity.Inventory;
import com.example.InventoryService.repository.InventoryRepository;

import reactor.core.publisher.Mono;


@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Mono<Inventory> createInventory(Inventory inventory) {
        return inventoryRepository.findByProductId(inventory.getProductId())
                .flatMap(existing -> Mono.<Inventory>error(new RuntimeException("Bu ürün için zaten stok kaydı mevcut: " + inventory.getProductId())))
                .switchIfEmpty(inventoryRepository.save(inventory));
    }

    public Mono<Void> deleteInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .flatMap(inventoryRepository::delete)
                .then();
    }

    public Mono<Void> deductStock(StockDeductionRequest request) {
        return inventoryRepository.findByProductId(request.productId())
                .switchIfEmpty(Mono.error(new RuntimeException("Stok kaydı bulunamadı: productId=" + request.productId())))
                .flatMap(inv -> {
                    if (inv.getStock() < request.quantity()) {
                        return Mono.error(new RuntimeException(
                                String.format("Yetersiz stok: mevcut=%d, istenen=%d", inv.getStock(), request.quantity())
                        ));
                    }
                    inv.setStock(inv.getStock() - request.quantity());
                    return inventoryRepository.save(inv)
                            .doOnNext(saved -> log.info("Stok düşüldü: productId={}, yeni stok={}, düşülen miktar={}",
                                    saved.getProductId(), saved.getStock(), request.quantity()))
                            .then();
                });
    }
}
