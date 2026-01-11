package com.example.InventoryService.repository;

import com.example.InventoryService.entity.Inventory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Long> {

    Mono<Inventory> findByProductId(Long productId);
}
