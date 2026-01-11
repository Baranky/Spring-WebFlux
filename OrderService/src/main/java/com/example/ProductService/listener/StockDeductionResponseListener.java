package com.example.ProductService.listener;

import com.example.ProductService.entity.Order;
import com.example.ProductService.enums.OrderStatus;
import com.example.ProductService.event.StockDeductionResponse;
import com.example.ProductService.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class StockDeductionResponseListener {

    private final OrderRepository orderRepository;

    public StockDeductionResponseListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "stock-deduction-response", groupId = "order-service-group",
            containerFactory = "stockDeductionResponseKafkaListenerContainerFactory")
    public void handleStockDeductionResponse(StockDeductionResponse response) {
        log.info("Stok düşürme response alındı: orderId={}, productId={}, quantity={}, success={}, message={}",
                response.orderId(), response.productId(), response.quantity(), response.success(), response.message());

        // Sipariş status'ünü güncelle
        orderRepository.findById(response.orderId())
                .flatMap(order -> {
                    if (response.success()) {
                        order.setStatus(OrderStatus.CONFIRMED);
                        log.info("Sipariş onaylandı: orderId={}", response.orderId());
                    } else {
                        order.setStatus(OrderStatus.CANCELLED);
                        log.warn("Sipariş iptal edildi (stok düşürülemedi): orderId={}, reason={}",
                                response.orderId(), response.message());
                    }
                    return orderRepository.save(order);
                })
                .doOnError(error -> log.error("Sipariş güncellenirken hata oluştu: orderId={}, error={}",
                        response.orderId(), error.getMessage(), error))
                .subscribe();
    }
}
