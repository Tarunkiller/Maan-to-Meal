package com.maanmeal.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double pricePerUnit;

    private String unit = "kg";
    private Double stockQty = 0.0;
    private String imageUrl = "/frontend/static/uploads/default_product.jpg";
    private String qualityGrade = "A";
    private String location;
    private Boolean isAvailable = true;
    private Boolean isOrganic = false;
    private LocalDate harvestDate;
    private Integer expiryDays = 7;
    private Integer views = 0;
    private Double rating = 0.0;
    private Integer totalReviews = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getFarmer() { return farmer; }
    public void setFarmer(User farmer) { this.farmer = farmer; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(Double pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getStockQty() { return stockQty; }
    public void setStockQty(Double stockQty) { this.stockQty = stockQty; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean available) { isAvailable = available; }

    public Boolean getIsOrganic() { return isOrganic; }
    public void setIsOrganic(Boolean organic) { isOrganic = organic; }

    public LocalDate getHarvestDate() { return harvestDate; }
    public void setHarvestDate(LocalDate harvestDate) { this.harvestDate = harvestDate; }

    public Integer getExpiryDays() { return expiryDays; }
    public void setExpiryDays(Integer expiryDays) { this.expiryDays = expiryDays; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toDict() { return toDict(true); }

    public Map<String, Object> toDict(boolean includeFarmer) {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", id);
        dict.put("farmer_id", farmer != null ? farmer.getId() : null);
        dict.put("name", name);
        dict.put("category", category);
        dict.put("description", description);
        dict.put("price_per_unit", pricePerUnit);
        dict.put("unit", unit);
        dict.put("stock_qty", stockQty);
        dict.put("image_url", imageUrl);
        dict.put("quality_grade", qualityGrade);
        dict.put("location", location);
        dict.put("is_available", isAvailable);
        dict.put("is_organic", isOrganic);
        dict.put("harvest_date", harvestDate != null ? harvestDate.toString() : null);
        dict.put("expiry_days", expiryDays);
        dict.put("views", views);
        dict.put("rating", rating != null ? Math.round(rating * 10.0) / 10.0 : 0.0);
        dict.put("total_reviews", totalReviews);
        dict.put("created_at", createdAt != null ? createdAt.toString() : null);

        if (includeFarmer && farmer != null) {
            dict.put("farmer_name", farmer.getName());
            if (farmer.getFarmerProfile() != null) {
                dict.put("farm_name", farmer.getFarmerProfile().getFarmName());
                dict.put("farm_location", farmer.getFarmerProfile().getFarmLocation());
            } else {
                dict.put("farm_name", "");
                dict.put("farm_location", "");
            }
        }
        return dict;
    }
}
