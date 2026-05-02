package com.maanmeal.repository;

import com.maanmeal.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByFarmerId(Long farmerId);
    
    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND p.farmer.approved = true AND p.farmer.active = true")
    List<Product> findAllAvailableProducts();
}
