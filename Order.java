package com.maanmeal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consumer_id", nullable = false)
    private User consumer;

    private String status = "pending";
    private Double totalAmount;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress;

    private String paymentMethod = "cod";
    private String paymentStatus = "pending";
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String deliveryNotes;

    private LocalDateTime estimatedDelivery;
    private LocalDateTime deliveredAt;
    private String qrCode;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getConsumer() { return consumer; }
    public void setConsumer(User consumer) { this.consumer = consumer; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }

    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public Map<String, Object> toDict() { return toDict(true); }

    public Map<String, Object> toDict(boolean includeItems) {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", id);
        dict.put("consumer_id", consumer != null ? consumer.getId() : null);
        dict.put("status", status);
        dict.put("total_amount", totalAmount);
        dict.put("delivery_address", deliveryAddress);
        dict.put("payment_method", paymentMethod);
        dict.put("payment_status", paymentStatus);
        dict.put("payment_reference", paymentReference);
        dict.put("delivery_notes", deliveryNotes);
        dict.put("estimated_delivery", estimatedDelivery != null ? estimatedDelivery.toString() : null);
        dict.put("delivered_at", deliveredAt != null ? deliveredAt.toString() : null);
        dict.put("qr_code", qrCode);
        dict.put("created_at", createdAt != null ? createdAt.toString() : null);
        dict.put("consumer_name", consumer != null ? consumer.getName() : null);
        dict.put("consumer_email", consumer != null ? consumer.getEmail() : null);
        dict.put("consumer_phone", consumer != null ? consumer.getPhone() : null);

        if (includeItems && items != null) {
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (OrderItem i : items) {
                itemsList.add(i.toDict());
            }
            dict.put("items", itemsList);
        }
        return dict;
    }
}
