package com.maanmeal.controller;

import com.maanmeal.model.FarmerProfile;
import com.maanmeal.model.User;
import com.maanmeal.repository.UserRepository;
import com.maanmeal.security.JwtUtil;
import com.maanmeal.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> data) {
        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");
        String role = data.get("role");

        if (name == null || email == null || password == null || role == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Name, email, password, and role are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (!role.equals("farmer") && !role.equals("consumer")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid role");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (userRepository.findByEmail(email.toLowerCase().trim()).isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email already registered");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.toLowerCase().trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(role);
        user.setPhone(data.getOrDefault("phone", ""));
        user.setAddress(data.getOrDefault("address", ""));
        user.setApproved(role.equals("consumer"));

        if (role.equals("farmer")) {
            String farmName = data.get("farm_name");
            String farmLocation = data.get("farm_location");
            if (farmName == null || farmLocation == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "farm_name and farm_location are required for farmers");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            FarmerProfile profile = new FarmerProfile();
            profile.setUser(user);
            profile.setFarmName(farmName.trim());
            profile.setFarmLocation(farmLocation.trim());
            profile.setFarmSize(data.getOrDefault("farm_size", ""));
            profile.setDescription(data.getOrDefault("farm_description", ""));
            user.setFarmerProfile(profile);
        }

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        String msg = role.equals("farmer") ? "Registration successful. Awaiting admin approval." : "Registration successful!";

        Map<String, Object> response = new HashMap<>();
        response.put("message", msg);
        response.put("access_token", token);
        response.put("refresh_token", refreshToken);
        response.put("user", user.toDict());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String password = data.get("password");

        if (email == null || password == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email and password required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        if (userOpt.isEmpty() || !PasswordUtil.check(password, userOpt.get().getPasswordHash())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        User user = userOpt.get();
        if (Boolean.FALSE.equals(user.getActive())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Account is deactivated. Contact support.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        String token = jwtUtil.generateToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("access_token", token);
        response.put("refresh_token", refreshToken);
        response.put("user", user.toDict());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Missing Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        Long userId = jwtUtil.extractUserId(authHeader);
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        String newToken = jwtUtil.generateToken(userId);
        Map<String, String> response = new HashMap<>();
        response.put("access_token", newToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("user", userOpt.get().toDict());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @RequestBody Map<String, String> data) {
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
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        User user = userOpt.get();
        if (data.containsKey("name")) user.setName(data.get("name"));
        if (data.containsKey("phone")) user.setPhone(data.get("phone"));
        if (data.containsKey("address")) user.setAddress(data.get("address"));
        if (data.containsKey("password") && data.containsKey("new_password")) {
            if (!PasswordUtil.check(data.get("password"), user.getPasswordHash())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Current password incorrect");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            user.setPasswordHash(PasswordUtil.hash(data.get("new_password")));
        }

        if (user.getRole().equals("farmer") && user.getFarmerProfile() != null) {
            FarmerProfile profile = user.getFarmerProfile();
            if (data.containsKey("farm_name")) profile.setFarmName(data.get("farm_name"));
            if (data.containsKey("farm_location")) profile.setFarmLocation(data.get("farm_location"));
            if (data.containsKey("farm_size")) profile.setFarmSize(data.get("farm_size"));
            if (data.containsKey("farm_description")) profile.setDescription(data.get("farm_description"));
        }

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated");
        response.put("user", user.toDict());
        return ResponseEntity.ok(response);
    }
}
