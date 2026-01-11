package com.example.ProductService.controller;

import com.example.ProductService.dto.OrderRequest;
import com.example.ProductService.entity.Order;
import com.example.ProductService.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Flux<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public Mono<Order> getOrderById(@PathVariable("id") Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/customer/{email}")
    public Flux<Order> getOrdersByCustomerEmail(@PathVariable("email") String email) {
        return orderService.getOrdersByCustomerEmail(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> createOrder(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    @PutMapping("/{id}")
    public Mono<Order> updateOrder(@PathVariable("id") Long id, @RequestBody OrderRequest request) {
        return orderService.updateOrder(id, request);
    }

    @PatchMapping("/{id}/status")
    public Mono<Order> updateOrderStatus(@PathVariable("id") Long id) {
        return orderService.updateOrderStatus(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteOrder(@PathVariable("id") Long id) {
        return orderService.deleteOrder(id);
    }

    @PostMapping("/test-circuit")
    public Mono<String> testCircuitBreaker(@RequestParam("productId") Long productId) {
        log.info("Circuit breaker testi, productId={}", productId);
        return orderService.siparisVer(productId);
    }
}
