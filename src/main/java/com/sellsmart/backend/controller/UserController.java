package com.sellsmart.backend.controller;

import com.sellsmart.backend.model.User;
import com.sellsmart.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        try {
            String result = userService.register(user);
            if (result.contains("successfully")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(403).body(result); 
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            Map<String, String> result = userService.login(user);
            if (result.containsKey("token")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(403).body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/is-registered")
    public ResponseEntity<Boolean> isRegistered(@RequestParam String email) {
        try {
            return ResponseEntity.ok(userService.isRegistered(email));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(false);
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean authorized = userService.isAuthorized(email);
        boolean registered = false;

        if (authorized) {
            try {
                registered = userService.isRegistered(email);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error checking registration");
            }
        }

        return ResponseEntity.ok(Map.of(
            "authorized", authorized,
            "registered", registered
        ));
    }
} 
