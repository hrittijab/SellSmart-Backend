package com.sellsmart.backend.controller;

import com.sellsmart.backend.model.User;
import com.sellsmart.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


@CrossOrigin(origins = "http://localhost:3000") // Allow frontend to connect
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
                return ResponseEntity.status(403).body(result); // unauthorized email, already registered, etc.
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        try {
            String result = userService.login(user);
            if (result.equals("Login successful")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(403).body(result); // Not registered or wrong password
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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