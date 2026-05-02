package com.maanmeal.repository;

import com.maanmeal.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByConsumerIdOrderByCreatedAtDesc(Long consumerId);
    List<Order> findByConsumerIdAndStatusOrderByCreatedAtDesc(Long consumerId, String status);
}
