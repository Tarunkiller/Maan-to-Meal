package com.maanmeal.controller;

import com.maanmeal.model.Notification;
import com.maanmeal.model.Product;
import com.maanmeal.model.Review;
import com.maanmeal.repository.NotificationRepository;
import com.maanmeal.repository.ProductRepository;
import com.maanmeal.repository.ReviewRepository;
import com.maanmeal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PublicController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private static final List<String> CATEGORIES = Arrays.asList(
            "Vegetables", "Fruits", "Grains", "Dairy", "Herbs", "Pulses", "Spices", "Organic", "Other"
    );

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int per_page,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double min_price,
            @RequestParam(required = false) Double max_price,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "created_at") String sort_by,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) Boolean is_organic) {

        List<Product> products = productRepository.findAllAvailableProducts();

        // Filters
        if (category != null) {
            products = products.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }
        if (min_price != null) {
            products = products.stream()
                    .filter(p -> p.getPricePerUnit() >= min_price)
                    .collect(Collectors.toList());
        }
        if (max_price != null) {
            products = products.stream()
                    .filter(p -> p.getPricePerUnit() <= max_price)
                    .collect(Collectors.toList());
        }
        if (location != null && !location.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getLocation() != null && p.getLocation().toLowerCase().contains(location.toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (search != null && !search.isEmpty()) {
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(search.toLowerCase())) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(search.toLowerCase())) ||
                            (p.getCategory() != null && p.getCategory().toLowerCase().contains(search.toLowerCase())))
                    .collect(Collectors.toList());
        }
        if (is_organic != null) {
            products = products.stream()
                    .filter(p -> p.getIsOrganic().equals(is_organic))
                    .collect(Collectors.toList());
        }

        // Sorting
        products.sort((p1, p2) -> {
            int cmp = 0;
            switch (sort_by) {
                case "price":
                    cmp = p1.getPricePerUnit().compareTo(p2.getPricePerUnit());
                    break;
                case "rating":
                    cmp = p1.getRating().compareTo(p2.getRating());
                    break;
                case "views":
                    cmp = p1.getViews().compareTo(p2.getViews());
                    break;
                default:
                    cmp = p1.getCreatedAt().compareTo(p2.getCreatedAt());
                    break;
            }
            return order.equalsIgnoreCase("desc") ? -cmp : cmp;
        });

        int total = products.size();
        int fromIndex = (page - 1) * per_page;
        int toIndex = Math.min(fromIndex + per_page, total);
        List<Product> paginated = fromIndex <= total && fromIndex >= 0 ? products.subList(fromIndex, toIndex) : new ArrayList<>();

        int pages = (int) Math.ceil((double) total / per_page);

        Map<String, Object> response = new HashMap<>();
        response.put("products", paginated.stream().map(Product::toDict).collect(Collectors.toList()));
        response.put("total", total);
        response.put("pages", pages);
        response.put("page", page);
        response.put("per_page", per_page);
        response.put("has_next", page < pages);
        response.put("has_prev", page > 1);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        Optional<Product> pOpt = productRepository.findById(id);
        if (pOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Product not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        Product p = pOpt.get();
        p.setViews(p.getViews() + 1);
        productRepository.save(p);

        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(id);

        Map<String, Object> data = p.toDict();
        data.put("reviews", reviews.stream().map(Review::toDict).collect(Collectors.toList()));
        return ResponseEntity.ok(data);
    }

    @GetMapping("/products/featured")
    public ResponseEntity<?> featuredProducts() {
        List<Product> products = productRepository.findAllAvailableProducts();
        products.sort((p1, p2) -> {
            int cmp = p2.getRating().compareTo(p1.getRating());
            if (cmp == 0) {
                cmp = p2.getViews().compareTo(p1.getViews());
            }
            return cmp;
        });
        List<Product> featured = products.stream().limit(8).collect(Collectors.toList());
        return ResponseEntity.ok(featured.stream().map(Product::toDict).collect(Collectors.toList()));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        Map<String, Object> response = new HashMap<>();
        response.put("categories", CATEGORIES);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        Long userId = jwtUtil.extractUserId(authHeader);
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifs.stream().limit(20).map(Notification::toDict).collect(Collectors.toList()));
        response.put("unread_count", unread);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @PathVariable Long id) {
        if (authHeader == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        Long userId = jwtUtil.extractUserId(authHeader);
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Optional<Notification> nOpt = notificationRepository.findById(id);
        if (nOpt.isEmpty() || !nOpt.get().getUser().getId().equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Notification not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        Notification n = nOpt.get();
        n.setIsRead(true);
        notificationRepository.save(n);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Marked as read");
        return ResponseEntity.ok(response);
    }
}
