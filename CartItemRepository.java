package com.maanmeal.repository;

import com.maanmeal.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByConsumerId(Long consumerId);
    Optional<CartItem> findByConsumerIdAndProductId(Long consumerId, Long productId);
    void deleteByConsumerId(Long consumerId);
}
