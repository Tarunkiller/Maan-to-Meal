package com.maanmeal.controller;

import com.maanmeal.model.*;
import com.maanmeal.repository.*;
import com.maanmeal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getAuthenticatedUserId(String authHeader) {
        if (authHeader == null) return null;
        return jwtUtil.extractUserId(authHeader);
    }

    private User getAuthenticatedAdmin(String authHeader) {
        Long userId = getAuthenticatedUserId(authHeader);
        if (userId == null) return null;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getRole().equals("admin")) return null;
        return userOpt.get();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        long totalUsers = userRepository.count();
        long totalFarmers = userRepository.findByRole("farmer").size();
        long totalConsumers = userRepository.findByRole("consumer").size();
        long pendingFarmers = userRepository.findByRoleAndApproved("farmer", false).size();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        double totalRevenue = orderRepository.findAll().stream()
                .filter(o -> o.getStatus().equals("delivered"))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_users", totalUsers);
        stats.put("total_farmers", totalFarmers);
        stats.put("total_consumers", totalConsumers);
        stats.put("pending_farmers", pendingFarmers);
        stats.put("total_products", totalProducts);
        stats.put("total_orders", totalOrders);
        stats.put("total_revenue", Math.round(totalRevenue * 100.0) / 100.0);

        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats);
        response.put("monthly_revenue", new ArrayList<>());
        response.put("category_sales", new ArrayList<>());
        response.put("recent_orders", orderRepository.findAll().stream().limit(10).map(o -> o.toDict(false)).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/farmers/pending")
    public ResponseEntity<?> getPendingFarmers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<User> pending = userRepository.findByRoleAndApproved("farmer", false);

        Map<String, Object> response = new HashMap<>();
        response.put("farmers", pending.stream().map(User::toDict).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/farmers")
    public ResponseEntity<?> getAllFarmers(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(required = false) String search) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<User> farmers = userRepository.findByRole("farmer");
        if (search != null && !search.isEmpty()) {
            farmers = farmers.stream()
                    .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(search.toLowerCase())) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())))
                    .collect(Collectors.toList());
        }

        int perPage = 20;
        int total = farmers.size();
        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, total);
        List<User> paginated = fromIndex <= total && fromIndex >= 0 ? farmers.subList(fromIndex, toIndex) : new ArrayList<>();

        int pages = (int) Math.ceil((double) total / perPage);

        Map<String, Object> response = new HashMap<>();
        response.put("farmers", paginated.stream().map(User::toDict).collect(Collectors.toList()));
        response.put("total", total);
        response.put("pages", pages);
        response.put("page", page);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/farmers/{id}/approve")
    public ResponseEntity<?> approveFarmer(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @PathVariable Long id) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<User> uOpt = userRepository.findById(id);
        if (uOpt.isEmpty() || !uOpt.get().getRole().equals("farmer")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User farmer = uOpt.get();
        farmer.setApproved(true);
        if (farmer.getFarmerProfile() != null) {
            farmer.getFarmerProfile().setIsVerified(true);
        }
        userRepository.save(farmer);

        Notification n = new Notification();
        n.setUser(farmer);
        n.setTitle("Account Approved! 🎉");
        n.setMessage("Congratulations! Your farmer account has been approved. You can now add products.");
        n.setType("approval");
        n.setLink("/frontend/farmer/dashboard.html");
        notificationRepository.save(n);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Farmer approved");
        response.put("farmer", farmer.toDict());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/farmers/{id}/reject")
    public ResponseEntity<?> rejectFarmer(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                          @PathVariable Long id) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<User> uOpt = userRepository.findById(id);
        if (uOpt.isEmpty() || !uOpt.get().getRole().equals("farmer")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User farmer = uOpt.get();
        farmer.setApproved(false);
        farmer.setActive(false);
        userRepository.save(farmer);

        Notification n = new Notification();
        n.setUser(farmer);
        n.setTitle("Account Not Approved");
        n.setMessage("Unfortunately your farmer registration was not approved. Contact support for more info.");
        n.setType("approval");
        notificationRepository.save(n);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Farmer rejected");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) String search) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<User> users = userRepository.findAll();
        if (role != null && !role.isEmpty()) {
            users = users.stream().filter(u -> u.getRole().equalsIgnoreCase(role)).collect(Collectors.toList());
        }
        if (search != null && !search.isEmpty()) {
            users = users.stream()
                    .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(search.toLowerCase())) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())))
                    .collect(Collectors.toList());
        }

        users.sort((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()));

        int perPage = 20;
        int total = users.size();
        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, total);
        List<User> paginated = fromIndex <= total && fromIndex >= 0 ? users.subList(fromIndex, toIndex) : new ArrayList<>();

        int pages = (int) Math.ceil((double) total / perPage);

        Map<String, Object> response = new HashMap<>();
        response.put("users", paginated.stream().map(User::toDict).collect(Collectors.toList()));
        response.put("total", total);
        response.put("pages", pages);
        response.put("page", page);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<?> toggleUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                        @PathVariable Long id) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<User> uOpt = userRepository.findById(id);
        if (uOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = uOpt.get();
        if (user.getRole().equals("admin")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Cannot deactivate admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        user.setActive(!user.getActive());
        userRepository.save(user);

        String status = user.getActive() ? "activated" : "deactivated";
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User " + status);
        response.put("user", user.toDict());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(required = false) String status) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Order> orders = orderRepository.findAll();
        if (status != null && !status.isEmpty()) {
            orders = orders.stream().filter(o -> o.getStatus().equalsIgnoreCase(status)).collect(Collectors.toList());
        }

        orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));

        int perPage = 20;
        int total = orders.size();
        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, total);
        List<Order> paginated = fromIndex <= total && fromIndex >= 0 ? orders.subList(fromIndex, toIndex) : new ArrayList<>();

        int pages = (int) Math.ceil((double) total / perPage);

        Map<String, Object> response = new HashMap<>();
        response.put("orders", paginated.stream().map(o -> o.toDict(false)).collect(Collectors.toList()));
        response.put("total", total);
        response.put("pages", pages);
        response.put("page", page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestParam(defaultValue = "1") int page) {
        User admin = getAuthenticatedAdmin(authHeader);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<com.maanmeal.model.Transaction> transactions = transactionRepository.findAll();
        transactions.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));

        int perPage = 20;
        int total = transactions.size();
        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, total);
        List<com.maanmeal.model.Transaction> paginated = fromIndex <= total && fromIndex >= 0 ? transactions.subList(fromIndex, toIndex) : new ArrayList<>();

        int pages = (int) Math.ceil((double) total / perPage);

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", paginated.stream().map(com.maanmeal.model.Transaction::toDict).collect(Collectors.toList()));
        response.put("total", total);
        response.put("pages", pages);

        return ResponseEntity.ok(response);
    }
}
