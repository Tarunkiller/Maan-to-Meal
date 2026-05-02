package com.maanmeal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "consumer"; // farmer, consumer, admin

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_approved")
    private Boolean approved = true;

    @Column(name = "is_active")
    private Boolean active = true;

    private String profileImage = "/frontend/static/uploads/default_avatar.png";

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private FarmerProfile farmerProfile;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public FarmerProfile getFarmerProfile() { return farmerProfile; }
    public void setFarmerProfile(FarmerProfile farmerProfile) { this.farmerProfile = farmerProfile; }

    public Map<String, Object> toDict() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("id", id);
        dict.put("name", name);
        dict.put("email", email);
        dict.put("role", role);
        dict.put("phone", phone);
        dict.put("address", address);
        dict.put("is_approved", approved);
        dict.put("is_active", active);
        dict.put("profile_image", profileImage);
        dict.put("created_at", createdAt != null ? createdAt.toString() : null);
        if (farmerProfile != null) {
            dict.put("farmer_profile", farmerProfile.toDict());
        } else {
            dict.put("farmer_profile", null);
        }
        return dict;
    }
}
