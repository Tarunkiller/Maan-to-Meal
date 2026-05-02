package com.maanmeal.controller;

import com.maanmeal.model.*;
import com.maanmeal.repository.*;
import com.maanmeal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/farmer")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FarmerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getAuthenticatedUserId(String authHeader) {
        if (authHeader == null) return null;
        return jwtUtil.extractUserId(authHeader);
    }

    private User getAuthenticatedFarmer(String authHeader) {
        Long userId = getAuthenticatedUserId(authHeader);
        if (userId == null) return null;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getRole().equals("farmer")) return null;
        return userOpt.get();
    }

    @GetMapping("/products")
    public ResponseEntity<?> getMyProducts(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int per_page) {
        User farmer = getAuthenticatedFarmer(authHeader);
        if (farmer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Product> products = productRepository.findByFarmerId(farmer.getId());
        products.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

        int total = products.size();
        int fromIndex = (page - 1) * per_page;
        int toIndex = Math.min(fromIndex + per_page, total);
        List<Product> paginated = fromIndex <= total && fromIndex >= 0 ? products.subList(fromIndex, toIndex) : new ArrayList<>();

        int pages = (int) Math.ceil((double) total / per_page);

        Map<String, Object> response = new HashMap<>();
        response.put("products", paginated.stream().map(p -> p.toDict(false)).collect(Collectors.toList()));
        response.put("total", total);
        response.put("pages", pages);
        response.put("page", page);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                        @RequestBody Map<String, Object> data) {
        User farmer = getAuthenticatedFarmer(authHeader);
        if (farmer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String name = (String) data.get("name");
        String category = (String) data.get("category");
        String pStr = String.valueOf(data.get("price_per_unit"));
        String qStr = String.valueOf(data.get("stock_qty"));
        String unit = (String) data.get("unit");

        if (name == null || category == null || pStr.equals("null") || qStr.equals("null") || unit == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Name, category, price_per_unit, unit, stock_qty are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Product p = new Product();
        p.setFarmer(farmer);
        p.setName(name);
        p.setCategory(category);
        p.setDescription((String) data.getOrDefault("description", ""));
        p.setPricePerUnit(Double.parseDouble(pStr));
        p.setUnit(unit);
        p.setStockQty(Double.parseDouble(qStr));
        p.setImageUrl((String) data.getOrDefault("image_url", "/frontend/static/uploads/default_product.jpg"));
        p.setQualityGrade((String) data.getOrDefault("quality_grade", "A"));
        p.setLocation(farmer.getFarmerProfile() != null ? farmer.getFarmerProfile().getFarmLocation() : "");
        p.setIsOrganic(Boolean.parseBoolean(String.valueOf(data.getOrDefault("is_organic", "false"))));
        p.setExpiryDays(Integer.parseInt(String.valueOf(data.getOrDefault("expiry_days", "7"))));
        if (data.containsKey("harvest_date") && data.get("harvest_date") != null) {
            p.setHarvestDate(LocalDate.parse(String.valueOf(data.get("harvest_date"))));
        }

        productRepository.save(p);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product added successfully");
        response.put("product", p.toDict());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @PathVariable Long id,
                                           @RequestBody Map<String, Object> data) {
        User farmer = getAuthenticatedFarmer(authHeader);
        if (farmer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isEmpty() || !pOpt.get().getFarmer().getId().equals(farmer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Product p = pOpt.get();
        if (data.containsKey("name")) p.setName((String) data.get("name"));
        if (data.containsKey("category")) p.setCategory((String) data.get("category"));
        if (data.containsKey("description")) p.setDescription((String) data.get("description"));
        if (data.containsKey("price_per_unit")) p.setPricePerUnit(Double.parseDouble(String.valueOf(data.get("price_per_unit"))));
        if (data.containsKey("unit")) p.setUnit((String) data.get("unit"));
        if (data.containsKey("stock_qty")) p.setStockQty(Double.parseDouble(String.valueOf(data.get("stock_qty"))));
        if (data.containsKey("is_available")) p.setIsAvailable(Boolean.parseBoolean(String.valueOf(data.get("is_available"))));
        if (data.containsKey("is_organic")) p.setIsOrganic(Boolean.parseBoolean(String.valueOf(data.get("is_organic"))));
        if (data.containsKey("quality_grade")) p.setQualityGrade((String) data.get("quality_grade"));
        if (data.containsKey("expiry_days")) p.setExpiryDays(Integer.parseInt(String.valueOf(data.get("expiry_days"))));
        if (data.containsKey("harvest_date") && data.get("harvest_date") != null) {
            p.setHarvestDate(LocalDate.parse(String.valueOf(data.get("harvest_date"))));
        }

        productRepository.save(p);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product updated");
        response.put("product", p.toDict());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @PathVariable Long id) {
        User farmer = getAuthenticatedFarmer(authHeader);
        if (farmer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isEmpty() || !pOpt.get().getFarmer().getId().equals(farmer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        productRepository.delete(pOpt.get());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Product deleted");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(required = false) String status) {
        User farmer = getAuthenticatedFarmer(authHeader);
        if (farmer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<OrderItem> items = orderItemRepository.findByFarmerId(farmer.getId());
        Set<Order> distinctOrders = items.stream().map(OrderItem::getOrder).collect(Collectors.toSet());

        List<Order> farmerOrders = new ArrayList<>(distinctOrders);
        if (status != null && !status.isEmpty()) {
            farmerOrders = farmerOrders.stream().filter(o -> o.getStatus().equals(status)).collect(Collectors.toList());
        }

        farmerOrders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));

        int perPage = 20;
        int total = farmerOrders.size();
        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, total);
        List<Order> paginated = fromIndex <= total && fromIndex >= 0 ? farmerOrders.subList(fromIndex, toIndex) : new ArrayList<>();

        List<Map<String, Object>> mappedOrders = new ArrayList<>();
        for (Order o : paginated) {
            Map<String, Object> oMap = o.toDict(false);
            List<Map<String, Object>> farmerItems = items.stream()
                    .filter(oi -> oi.getOrder().getId().equals(o.getId()))
                    .map(OrderItem::toDict)
                    .collect(Collectors.toList());
            oMap.put("items", farmerItems);
            mappedOrders.add(oMap);
        }

        int pages = (int) Math.ceil((double) total / perPage);

        Map<String, Object> response = new HashMap<>();
        response.put("orders", mappedOrders);
        response.put("total", total);
        response.put("pages", pages);
        response.put("page", page);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                               @PathVariable Long id,
                                               @RequestBody Map<String, String> data) {
        User farmer = getAuthenticatedFarmer(authHeader);
        if (farmer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<OrderItem> items = orderItemRepository.findByFarmerId(farmer.getId());
        boolean hasItems = items.stream().anyMatch(oi -> oi.getOrder().getId().equals(id));
        if (!hasItems) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Order not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        String newStatus = data.get("status");
        List<String> validStatuses = Arrays.asList("confirmed", "processing", "shipped", "out_for_delivery", "delivered");
        if (newStatus == null || !validStatuses.contains(newStatus)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid status");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Optional<Order> oOpt = orderRepository.findById(id);
        if (oOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Order order = oOpt.get();
        order.setStatus(newStatus);
        if (newStatus.equals("delivered")) {
            order.setDeliveredAt(LocalDateTime.now());
            order.setPaymentStatus("paid");

            if (farmer.getFarmerProfile() != null) {
                FarmerProfile profile = farmer.getFarmerProfile();
                double farmerTotal = items.stream()
                        .filter(oi -> oi.getOrder().getId().equals(order.getId()))
                        .mapToDouble(OrderItem::getSubtotal)
                        .sum();
                profile.setTotalSales(profile.getTotalSales() + farmerTotal);
                userRepository.save(farmer);
            }
        }

        orderRepository.save(order);

        // Notify consumer
        Notification n = new Notification();
        n.setUser(order.getConsumer());
        n.setTitle("Order #" + order.getId() + " Update");
        n.setMessage("Your order status has been updated to: " + newStatus.toUpperCase());
        n.setType("order");
        n.setLink("/frontend/consumer/orders.html?order=" + order.getId());
        notificationRepository.save(n);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order status updated");
        response.put("order", order.toDict());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/earnings")
    public ResponseEntity<?> getEarnings(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User farmer = getAuthenticatedFarmer(authHeader);
        if (farmer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<OrderItem> items = orderItemRepository.findByFarmerId(farmer.getId());
        double totalEarnings = items.stream()
                .filter(oi -> oi.getOrder().getStatus().equals("delivered"))
                .mapToDouble(OrderItem::getSubtotal)
                .sum();

        long activeOrders = items.stream()
                .filter(oi -> !oi.getOrder().getStatus().equals("delivered") && !oi.getOrder().getStatus().equals("cancelled"))
                .map(OrderItem::getOrder)
                .distinct()
                .count();

        long totalProducts = productRepository.findByFarmerId(farmer.getId()).size();
        long totalOrders = items.stream().map(OrderItem::getOrder).distinct().count();

        Map<String, Object> response = new HashMap<>();
        response.put("total_earnings", Math.round(totalEarnings * 100.0) / 100.0);
        response.put("active_orders", activeOrders);
        response.put("total_products", totalProducts);
        response.put("total_orders", totalOrders);
        response.put("farmer_rating", farmer.getFarmerProfile() != null ? farmer.getFarmerProfile().getRating() : 0.0);
        response.put("monthly_earnings", new ArrayList<>()); // Optional
        response.put("product_stats", new ArrayList<>()); // Optional

        return ResponseEntity.ok(response);
    }
}
