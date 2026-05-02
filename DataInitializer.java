package com.maanmeal.config;

import com.maanmeal.model.*;
import com.maanmeal.repository.*;
import com.maanmeal.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // Already seeded
        }

        System.out.println("Seeding database with demo data...");

        // Admin
        User admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@maanmeal.com");
        admin.setRole("admin");
        admin.setPhone("9000000000");
        admin.setAddress("Maan-Meal HQ, Delhi");
        admin.setPasswordHash(PasswordUtil.hash("admin123"));
        admin.setApproved(true);
        userRepository.save(admin);
        System.out.println("[OK] Admin created");

        // Create farmers
        List<User> farmers = new ArrayList<>();

        User f1 = new User();
        f1.setName("Rajesh Kumar");
        f1.setEmail("rajesh@farm.com");
        f1.setRole("farmer");
        f1.setPhone("9876543210");
        f1.setAddress("Village Krishnapur, Pune, Maharashtra");
        f1.setPasswordHash(PasswordUtil.hash("farmer123"));
        f1.setApproved(true);
        FarmerProfile fp1 = new FarmerProfile();
        fp1.setUser(f1);
        fp1.setFarmName("Kumar Organic Farm");
        fp1.setFarmLocation("Pune, Maharashtra");
        fp1.setFarmSize("5 acres");
        fp1.setDescription("Organic vegetable farmer with 15 years experience");
        fp1.setIsVerified(true);
        fp1.setRating(4.5);
        fp1.setTotalReviews(23);
        f1.setFarmerProfile(fp1);
        userRepository.save(f1);
        farmers.add(f1);

        User f2 = new User();
        f2.setName("Priya Devi");
        f2.setEmail("priya@farm.com");
        f2.setRole("farmer");
        f2.setPhone("9876543211");
        f2.setAddress("Amritsar, Punjab");
        f2.setPasswordHash(PasswordUtil.hash("farmer123"));
        f2.setApproved(true);
        FarmerProfile fp2 = new FarmerProfile();
        fp2.setUser(f2);
        fp2.setFarmName("Devi Dairy & Grains");
        fp2.setFarmLocation("Amritsar, Punjab");
        fp2.setFarmSize("12 acres");
        fp2.setDescription("Specializing in wheat, rice, and fresh dairy products");
        fp2.setIsVerified(true);
        fp2.setRating(4.3);
        fp2.setTotalReviews(15);
        f2.setFarmerProfile(fp2);
        userRepository.save(f2);
        farmers.add(f2);

        User f3 = new User();
        f3.setName("Suresh Reddy");
        f3.setEmail("suresh@farm.com");
        f3.setRole("farmer");
        f3.setPhone("9876543212");
        f3.setAddress("Hyderabad, Telangana");
        f3.setPasswordHash(PasswordUtil.hash("farmer123"));
        f3.setApproved(true);
        FarmerProfile fp3 = new FarmerProfile();
        fp3.setUser(f3);
        fp3.setFarmName("Reddy Fresh Fruits");
        fp3.setFarmLocation("Hyderabad, Telangana");
        fp3.setFarmSize("8 acres");
        fp3.setDescription("Tropical fruit specialist - mangoes, bananas, papayas");
        fp3.setIsVerified(true);
        fp3.setRating(4.1);
        fp3.setTotalReviews(41);
        f3.setFarmerProfile(fp3);
        userRepository.save(f3);
        farmers.add(f3);

        System.out.println("[OK] Farmers created");

        // Create Products
        List<Product> products = new ArrayList<>();

        // f1 products
        products.add(createProd(f1, "Fresh Tomatoes", "Vegetables", 35.0, "kg", 200.0, true, 5, 4.5, 23));
        products.add(createProd(f1, "Spinach (Palak)", "Vegetables", 25.0, "bunch", 100.0, true, 3, 4.3, 15));
        products.add(createProd(f1, "Onions", "Vegetables", 28.0, "kg", 500.0, false, 30, 4.1, 41));
        products.add(createProd(f1, "Green Chillies", "Vegetables", 60.0, "kg", 50.0, true, 7, 4.7, 12));

        // f2 products
        products.add(createProd(f2, "Basmati Rice", "Grains", 85.0, "kg", 1000.0, false, 365, 4.8, 67));
        products.add(createProd(f2, "Wheat Flour (Atta)", "Grains", 42.0, "kg", 800.0, true, 90, 4.6, 34));
        products.add(createProd(f2, "Fresh Cow Milk", "Dairy", 55.0, "litre", 150.0, true, 2, 4.9, 89));
        products.add(createProd(f2, "Desi Ghee", "Dairy", 650.0, "500g", 60.0, true, 180, 4.9, 55));

        // f3 products
        products.add(createProd(f3, "Alphonso Mangoes", "Fruits", 320.0, "dozen", 80.0, false, 7, 4.9, 103));
        products.add(createProd(f3, "Bananas", "Fruits", 45.0, "dozen", 300.0, true, 5, 4.3, 28));
        products.add(createProd(f3, "Papaya", "Fruits", 40.0, "kg", 120.0, false, 7, 4.2, 19));
        products.add(createProd(f3, "Lemon", "Fruits", 80.0, "kg", 100.0, true, 14, 4.4, 22));

        System.out.println("[OK] Products created");

        // Consumers
        List<User> consumers = new ArrayList<>();
        User c1 = new User();
        c1.setName("Anita Sharma");
        c1.setEmail("anita@gmail.com");
        c1.setPhone("9123456789");
        c1.setAddress("Flat 301, Green Park, Mumbai");
        c1.setRole("consumer");
        c1.setPasswordHash(PasswordUtil.hash("consumer123"));
        c1.setApproved(true);
        userRepository.save(c1);
        consumers.add(c1);

        User c2 = new User();
        c2.setName("Mohan Patel");
        c2.setEmail("mohan@gmail.com");
        c2.setPhone("9123456790");
        c2.setAddress("House 12, Sector 5, Delhi");
        c2.setRole("consumer");
        c2.setPasswordHash(PasswordUtil.hash("consumer123"));
        c2.setApproved(true);
        userRepository.save(c2);
        consumers.add(c2);

        User c3 = new User();
        c3.setName("Kavita Nair");
        c3.setEmail("kavita@gmail.com");
        c3.setPhone("9123456791");
        c3.setAddress("45 MG Road, Bangalore");
        c3.setRole("consumer");
        c3.setPasswordHash(PasswordUtil.hash("consumer123"));
        c3.setApproved(true);
        userRepository.save(c3);
        consumers.add(c3);

        System.out.println("[OK] Consumers created");

        // Seed some sample orders
        Random random = new Random();
        for (int i = 0; i < consumers.size(); i++) {
            User consumer = consumers.get(i);
            Order order = new Order();
            order.setConsumer(consumer);
            order.setStatus("delivered");
            order.setDeliveryAddress(consumer.getAddress());
            order.setPaymentMethod("upi");
            order.setPaymentStatus("paid");
            order.setPaymentReference("MM-" + String.format("%06d", i + 1) + "-DEMO01");
            order.setEstimatedDelivery(LocalDateTime.now().minusHours(12));
            order.setDeliveredAt(LocalDateTime.now().minusHours(6));

            orderRepository.save(order);

            double total = 0.0;
            List<OrderItem> orderItems = new ArrayList<>();

            // Randomly select 3 products
            List<Product> selected = new ArrayList<>(products);
            for (int j = 0; j < 3; j++) {
                Product p = selected.remove(random.nextInt(selected.size()));
                double qty = 1.0 + random.nextInt(3);
                double subtotal = Math.round(p.getPricePerUnit() * qty * 100.0) / 100.0;
                total += subtotal;

                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(p);
                oi.setFarmer(p.getFarmer());
                oi.setQuantity(qty);
                oi.setUnitPrice(p.getPricePerUnit());
                oi.setSubtotal(subtotal);

                orderItemRepository.save(oi);
                orderItems.add(oi);
            }

            order.setTotalAmount(Math.round(total * 100.0) / 100.0);
            order.setItems(orderItems);
            orderRepository.save(order);

            com.maanmeal.model.Transaction txn = new com.maanmeal.model.Transaction();
            txn.setOrderId(order.getId());
            txn.setAmount(order.getTotalAmount());
            txn.setMethod(order.getPaymentMethod());
            txn.setStatus(order.getPaymentStatus());
            txn.setReferenceId(order.getPaymentReference());
            transactionRepository.save(txn);
        }

        System.out.println("[OK] Sample orders created");
        System.out.println("\n[DONE] Database seeded successfully!");
    }

    private Product createProd(User farmer, String name, String category, Double price, String unit, Double stock, boolean isOrganic, int expiry, Double rating, int totalReviews) {
        Product p = new Product();
        p.setFarmer(farmer);
        p.setName(name);
        p.setCategory(category);
        p.setPricePerUnit(price);
        p.setUnit(unit);
        p.setStockQty(stock);
        p.setIsOrganic(isOrganic);
        p.setExpiryDays(expiry);
        p.setRating(rating);
        p.setTotalReviews(totalReviews);
        p.setLocation(farmer.getFarmerProfile().getFarmLocation());
        p.setIsAvailable(true);
        p.setHarvestDate(LocalDate.now().minusDays(2));
        productRepository.save(p);
        return p;
    }
}
