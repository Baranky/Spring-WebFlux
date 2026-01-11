package com.example.ProductService.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.example.ProductService.entity.Product;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {
}
