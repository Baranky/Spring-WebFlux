package com.example.ProductService.service;

import com.example.ProductService.client.ProductClient;
import com.example.ProductService.dto.OrderRequest;
import com.example.ProductService.entity.Order;
import com.example.ProductService.enums.OrderStatus;
import com.example.ProductService.event.StockDeductionRequest;
import com.example.ProductService.repository.OrderRepository;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, StockDeductionRequest> stockDeductionRequestKafkaTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public OrderService(OrderRepository orderRepository,
            ProductClient productClient,
            KafkaTemplate<String, StockDeductionRequest> stockDeductionRequestKafkaTemplate,
            CircuitBreaker circuitBreaker,
            Retry retry) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.stockDeductionRequestKafkaTemplate = stockDeductionRequestKafkaTemplate;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Flux<Order> getOrdersByCustomerEmail(String email) {
        return orderRepository.findByCustomerEmail(email);
    }

    public Mono<Order> createOrder(OrderRequest request) {
        // Sipariş oluşturulur (PENDING status ile)
        Order newOrder = new Order();
        newOrder.setProductId(request.productId());
        newOrder.setQuantity(request.quantity());
        newOrder.setTotalPrice(request.totalPrice());
        newOrder.setCustomerName(request.customerName());
        newOrder.setCustomerEmail(request.customerEmail());
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setOrderDate(LocalDateTime.now());

        return orderRepository.save(newOrder)
                .doOnSuccess(order -> {
                    log.info("Sipariş oluşturuldu (PENDING): orderId={}, productId={}, quantity={}",
                            order.getId(), order.getProductId(), order.getQuantity());

                    // Kafka'ya stok düşürme event'i gönderilir
                    StockDeductionRequest stockRequest = new StockDeductionRequest(
                            order.getId(),
                            request.productId(),
                            request.quantity()
                    );
                    stockDeductionRequestKafkaTemplate.send("stock-deduction-request", stockRequest);
                    log.info("Stok düşürme eventi gönderildi: orderId={}, productId={}, quantity={}",
                            order.getId(), request.productId(), request.quantity());
                })
                .doOnError(error -> log.error("Sipariş oluşturulamadı: productId={}, quantity={}, error={}",
                request.productId(), request.quantity(), error.getMessage()));
    }

    public Mono<Order> updateOrder(Long id, OrderRequest request) {
        return orderRepository.findById(id)
                .flatMap(existingOrder -> {
                    existingOrder.setProductId(request.productId());
                    existingOrder.setQuantity(request.quantity());
                    existingOrder.setTotalPrice(request.totalPrice());
                    existingOrder.setCustomerName(request.customerName());
                    existingOrder.setCustomerEmail(request.customerEmail());
                    return orderRepository.save(existingOrder);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Sipariş bulunamadı: " + id)));
    }

    public Mono<Order> updateOrderStatus(Long id) {
        return orderRepository.findById(id)
                .flatMap(orderRepository::save)
                .switchIfEmpty(Mono.error(new RuntimeException("Sipariş bulunamadı: " + id)));
    }

    public Mono<String> siparisVer(Long productId) {
        return productClient.getProductById(productId)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .map(product -> "Sipariş oluşturuldu, ürün onaylandı.")
                .onErrorReturn("Circuit breaker açık veya hata oluştu, fallback çalıştı.");
    }

    public Mono<Void> deleteOrder(Long id) {
        return orderRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RuntimeException("Sipariş bulunamadı: " + id));
                    }
                    return orderRepository.deleteById(id);
                });
    }
}
