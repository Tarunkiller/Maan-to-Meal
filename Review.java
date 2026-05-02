package com.maanmeal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consumer_id", nullable = false)
    private User consumer;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    private Long orderId;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String images; // JSON list or comma separated image URLs

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getConsumer() { return consumer; }
    public void setConsumer(User consumer) { this.consumer = consumer; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public User getFarmer() { return farmer; }
    public void setFarmer(User farmer) { this.farmer = farmer; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toDict() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", id);
        dict.put("consumer_id", consumer != null ? consumer.getId() : null);
        dict.put("product_id", product != null ? product.getId() : null);
        dict.put("farmer_id", farmer != null ? farmer.getId() : null);
        dict.put("order_id", orderId);
        dict.put("rating", rating);
        dict.put("comment", comment);
        dict.put("created_at", createdAt != null ? createdAt.toString() : null);
        dict.put("consumer_name", consumer != null ? consumer.getName() : null);
        dict.put("consumer_image", consumer != null ? consumer.getProfileImage() : null);
        return dict;
    }
}
