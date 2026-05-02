package com.maanmeal.controller;

import com.maanmeal.model.*;
import com.maanmeal.repository.*;
import com.maanmeal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consumer")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ConsumerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getAuthenticatedUserId(String authHeader) {
        if (authHeader == null) return null;
        return jwtUtil.extractUserId(authHeader);
    }

    private User getAuthenticatedConsumer(String authHeader) {
        Long userId = getAuthenticatedUserId(authHeader);
        if (userId == null) return null;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getRole().equals("consumer")) return null;
        return userOpt.get();
    }

    @GetMapping("/cart")
    public ResponseEntity<?> getCart(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<CartItem> items = cartItemRepository.findByConsumerId(consumer.getId());
        double total = items.stream().mapToDouble(i -> i.getProduct().getPricePerUnit() * i.getQuantity()).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("items", items.stream().map(CartItem::toDict).collect(Collectors.toList()));
        response.put("total", Math.round(total * 100.0) / 100.0);
        response.put("count", items.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cart")
    public ResponseEntity<?> addToCart(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestBody Map<String, Object> data) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Long productId = Long.valueOf(String.valueOf(data.get("product_id")));
        double quantity = Double.parseDouble(String.valueOf(data.getOrDefault("quantity", "1")));

        Optional<Product> pOpt = productRepository.findById(productId);
        if (pOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Product product = pOpt.get();

        if (Boolean.FALSE.equals(product.getIsAvailable()) || product.getStockQty() < quantity) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Product unavailable or insufficient stock");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Optional<CartItem> existingOpt = cartItemRepository.findByConsumerIdAndProductId(consumer.getId(), productId);
        if (existingOpt.isPresent()) {
            CartItem existing = existingOpt.get();
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setConsumer(consumer);
            item.setProduct(product);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Added to cart");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/cart/{id}")
    public ResponseEntity<?> updateCart(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Object> data) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<CartItem> itemOpt = cartItemRepository.findById(id);
        if (itemOpt.isEmpty() || !itemOpt.get().getConsumer().getId().equals(consumer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        CartItem item = itemOpt.get();
        double qty = Double.parseDouble(String.valueOf(data.getOrDefault("quantity", "1")));

        if (qty <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(qty);
            cartItemRepository.save(item);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cart updated");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cart/{id}")
    public ResponseEntity<?> removeCartItem(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @PathVariable Long id) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<CartItem> itemOpt = cartItemRepository.findById(id);
        if (itemOpt.isEmpty() || !itemOpt.get().getConsumer().getId().equals(consumer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        cartItemRepository.delete(itemOpt.get());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Removed from cart");
        return ResponseEntity.ok(response);
    }

    @Transactional
    @DeleteMapping("/cart/clear")
    public ResponseEntity<?> clearCart(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        cartItemRepository.deleteByConsumerId(consumer.getId());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cart cleared");
        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping("/orders")
    public ResponseEntity<?> placeOrder(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                        @RequestBody Map<String, Object> data) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<CartItem> cartItems = cartItemRepository.findByConsumerId(consumer.getId());
        if (cartItems.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Cart is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String deliveryAddress = (String) data.get("delivery_address");
        if (deliveryAddress == null || deliveryAddress.isEmpty()) {
            deliveryAddress = consumer.getAddress();
        }
        if (deliveryAddress == null || deliveryAddress.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Delivery address required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        double total = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();
        Set<Long> farmerIds = new HashSet<>();

        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            if (p == null || Boolean.FALSE.equals(p.getIsAvailable())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Product is unavailable");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            if (p.getStockQty() < ci.getQuantity()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Insufficient stock for " + p.getName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            double subtotal = p.getPricePerUnit() * ci.getQuantity();
            total += subtotal;

            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setFarmer(p.getFarmer());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(p.getPricePerUnit());
            oi.setSubtotal(subtotal);

            orderItems.add(oi);
            p.setStockQty(p.getStockQty() - ci.getQuantity());
            productRepository.save(p);
            farmerIds.add(p.getFarmer().getId());
        }

        String paymentMethod = (String) data.getOrDefault("payment_method", "cod");

        Order order = new Order();
        order.setConsumer(consumer);
        order.setTotalAmount(Math.round(total * 100.0) / 100.0);
        order.setDeliveryAddress(deliveryAddress);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentMethod.equals("cod") ? "pending" : "paid");
        order.setDeliveryNotes((String) data.getOrDefault("delivery_notes", ""));
        order.setEstimatedDelivery(LocalDateTime.now().plusHours(24));
        order.setStatus("confirmed");

        orderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
            orderItemRepository.save(oi);
        }
        order.setItems(orderItems);

        String ref = "MM-" + String.format("%06d", order.getId()) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        order.setPaymentReference(ref);
        orderRepository.save(order);

        com.maanmeal.model.Transaction txn = new com.maanmeal.model.Transaction();
        txn.setOrderId(order.getId());
        txn.setAmount(total);
        txn.setMethod(paymentMethod);
        txn.setStatus(paymentMethod.equals("cod") ? "pending" : "paid");
        txn.setReferenceId(ref);
        transactionRepository.save(txn);

        cartItemRepository.deleteByConsumerId(consumer.getId());

        // Notify consumer
        Notification cn = new Notification();
        cn.setUser(consumer);
        cn.setTitle("Order Placed!");
        cn.setMessage("Your order #" + order.getId() + " has been confirmed. Expected delivery within 24 hours.");
        cn.setType("order");
        cn.setLink("/frontend/consumer/orders.html?order=" + order.getId());
        notificationRepository.save(cn);

        // Notify farmers
        for (Long fid : farmerIds) {
            Optional<User> fOpt = userRepository.findById(fid);
            if (fOpt.isPresent()) {
                Notification fn = new Notification();
                fn.setUser(fOpt.get());
                fn.setTitle("New Order Received!");
                fn.setMessage("You have a new order #" + order.getId() + ". Please confirm and process it.");
                fn.setType("order");
                fn.setLink("/frontend/farmer/orders.html?order=" + order.getId());
                notificationRepository.save(fn);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order placed successfully");
        response.put("order", order.toDict());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(required = false) String status) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByConsumerIdAndStatusOrderByCreatedAtDesc(consumer.getId(), status);
        } else {
            orders = orderRepository.findByConsumerIdOrderByCreatedAtDesc(consumer.getId());
        }

        int perPage = 10;
        int total = orders.size();
        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, total);
        List<Order> paginated = fromIndex <= total && fromIndex >= 0 ? orders.subList(fromIndex, toIndex) : new ArrayList<>();

        int pages = (int) Math.ceil((double) total / perPage);

        Map<String, Object> response = new HashMap<>();
        response.put("orders", paginated.stream().map(Order::toDict).collect(Collectors.toList()));
        response.put("total", total);
        response.put("pages", pages);
        response.put("page", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @PathVariable Long id) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Order> oOpt = orderRepository.findById(id);
        if (oOpt.isEmpty() || !oOpt.get().getConsumer().getId().equals(consumer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(oOpt.get().toDict());
    }

    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @PathVariable Long id) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Order> oOpt = orderRepository.findById(id);
        if (oOpt.isEmpty() || !oOpt.get().getConsumer().getId().equals(consumer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Order order = oOpt.get();
        if (!order.getStatus().equals("pending") && !order.getStatus().equals("confirmed")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Cannot cancel order in current status");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        order.setStatus("cancelled");
        for (OrderItem oi : order.getItems()) {
            if (oi.getProduct() != null) {
                Product p = oi.getProduct();
                p.setStockQty(p.getStockQty() + oi.getQuantity());
                productRepository.save(p);
            }
        }

        orderRepository.save(order);

        Notification n = new Notification();
        n.setUser(consumer);
        n.setTitle("Order Cancelled");
        n.setMessage("Order #" + order.getId() + " has been cancelled.");
        n.setType("order");
        notificationRepository.save(n);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order cancelled");
        response.put("order", order.toDict());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> submitReview(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                          @RequestBody Map<String, Object> data) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Long productId = Long.valueOf(String.valueOf(data.get("product_id")));
        Optional<Product> pOpt = productRepository.findById(productId);
        if (pOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Product product = pOpt.get();

        // Must have ordered it and been delivered
        List<Order> orders = orderRepository.findByConsumerIdAndStatusOrderByCreatedAtDesc(consumer.getId(), "delivered");
        boolean purchased = false;
        Long orderId = null;
        for (Order o : orders) {
            for (OrderItem oi : o.getItems()) {
                if (oi.getProduct().getId().equals(productId)) {
                    purchased = true;
                    orderId = o.getId();
                    break;
                }
            }
            if (purchased) break;
        }

        if (!purchased) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "You can only review products you have purchased");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        Optional<Review> existingOpt = reviewRepository.findByConsumerIdAndProductId(consumer.getId(), productId);
        if (existingOpt.isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "You have already reviewed this product");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        int rating = Integer.parseInt(String.valueOf(data.getOrDefault("rating", "5")));
        if (rating < 1 || rating > 5) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Rating must be between 1 and 5");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Review r = new Review();
        r.setConsumer(consumer);
        r.setProduct(product);
        r.setFarmer(product.getFarmer());
        r.setOrderId(orderId);
        r.setRating(rating);
        r.setComment((String) data.getOrDefault("comment", ""));

        reviewRepository.save(r);

        // Update product rating
        List<Review> pReviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        double prodAvg = pReviews.stream().mapToInt(Review::getRating).average().orElse(rating);
        product.setRating(Math.round(prodAvg * 10.0) / 10.0);
        product.setTotalReviews(pReviews.size());
        productRepository.save(product);

        // Update farmer rating
        List<Review> fReviews = reviewRepository.findByFarmerId(product.getFarmer().getId());
        double fAvg = fReviews.stream().mapToInt(Review::getRating).average().orElse(rating);
        if (product.getFarmer().getFarmerProfile() != null) {
            FarmerProfile profile = product.getFarmer().getFarmerProfile();
            profile.setRating(Math.round(fAvg * 10.0) / 10.0);
            profile.setTotalReviews(fReviews.size());
            userRepository.save(product.getFarmer());
        }

        Notification n = new Notification();
        n.setUser(product.getFarmer());
        n.setTitle("New Review Received");
        n.setMessage("You received a " + rating + "-star review for " + product.getName());
        n.setType("review");
        notificationRepository.save(n);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Review submitted");
        response.put("review", r.toDict());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> consumerDashboard(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User consumer = getAuthenticatedConsumer(authHeader);
        if (consumer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Order> orders = orderRepository.findByConsumerIdOrderByCreatedAtDesc(consumer.getId());
        long delivered = orders.stream().filter(o -> o.getStatus().equals("delivered")).count();
        double spent = orders.stream().filter(o -> o.getStatus().equals("delivered")).mapToDouble(Order::getTotalAmount).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("total_orders", orders.size());
        response.put("delivered_orders", delivered);
        response.put("total_spent", Math.round(spent * 100.0) / 100.0);
        response.put("recent_orders", orders.stream().limit(5).map(Order::toDict).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }
}
