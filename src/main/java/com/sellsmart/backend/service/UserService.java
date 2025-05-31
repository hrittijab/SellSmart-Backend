package com.sellsmart.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sellsmart.backend.model.User;
import com.sellsmart.backend.util.EmailLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // ✅ Load from Render secret file path
    private final Set<String> authorizedEmails = EmailLoader.loadEmails("/etc/secrets/authorized-emails.txt");

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String register(User user) throws ExecutionException, InterruptedException {
        String email = user.getEmail().trim().toLowerCase();

        logger.info("🔍 Attempting to register email: {}", email);
        logger.info("✅ Authorized list: {}", authorizedEmails);

        if (!authorizedEmails.contains(email)) {
            logger.warn("❌ Registration failed: unauthorized email {}", email);
            return "Email not authorized";
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("users").document(email);

        DocumentSnapshot snapshot = docRef.get().get();
        if (snapshot.exists()) {
            logger.info("ℹ️ User already registered: {}", email);
            return "Already registered";
        }

        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        user.setEmail(email);

        ApiFuture<WriteResult> result = docRef.set(user);
        logger.info("✅ User registered successfully: {}", email);
        return "Registered successfully at: " + result.get().getUpdateTime();
    }

    public String login(User user) throws ExecutionException, InterruptedException {
        String email = user.getEmail().trim().toLowerCase();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snapshot = db.collection("users").document(email).get().get();

        if (!snapshot.exists()) {
            logger.warn("❌ Login failed: user not found for email {}", email);
            return "Not registered";
        }

        User storedUser = snapshot.toObject(User.class);
        if (storedUser == null) {
            logger.error("❌ Login error: stored user is null for email {}", email);
            return "Login error";
        }

        if (!passwordEncoder.matches(user.getPassword(), storedUser.getPassword())) {
            logger.warn("❌ Login failed: incorrect password for {}", email);
            return "Incorrect password";
        }

        logger.info("✅ Login successful for {}", email);
        return "Login successful";
    }

    public boolean isRegistered(String emailRaw) throws ExecutionException, InterruptedException {
        String email = emailRaw.trim().toLowerCase();
        Firestore db = FirestoreClient.getFirestore();
        boolean exists = db.collection("users").document(email).get().get().exists();
        logger.info("📌 Checked registration status for {}: {}", email, exists);
        return exists;
    }

    public boolean isAuthorized(String emailRaw) {
        String email = emailRaw.trim().toLowerCase();
        boolean authorized = authorizedEmails.contains(email);
        logger.info("📌 Checked authorization for {}: {}", email, authorized);
        return authorized;
    }
}
