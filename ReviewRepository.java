package com.maanmeal.repository;

import com.maanmeal.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
    Optional<Review> findByConsumerIdAndProductId(Long consumerId, Long productId);
    List<Review> findByFarmerId(Long farmerId);
}
