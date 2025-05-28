package com.sellsmart.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sellsmart.backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final Set<String> authorizedEmails = Set.of(
            "client1@example.com", "client2@example.com", "hrittija2001@gmail.com"
    );

    @Autowired
    private PasswordEncoder passwordEncoder; // Injected encoder

    public String register(User user) throws ExecutionException, InterruptedException {
        String email = user.getEmail().trim().toLowerCase();

        if (!authorizedEmails.contains(email)) {
            return "Email not authorized";
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("users").document(email);

        DocumentSnapshot snapshot = docRef.get().get();
        if (snapshot.exists()) {
            return "Already registered";
        }

        // 🔐 Hash the password
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        user.setEmail(email); // Save normalized email

        ApiFuture<WriteResult> result = docRef.set(user);
        return "Registered successfully at: " + result.get().getUpdateTime();
    }

    public String login(User user) throws ExecutionException, InterruptedException {
        String email = user.getEmail().trim().toLowerCase();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snapshot = db.collection("users").document(email).get().get();

        if (!snapshot.exists()) return "Not registered";

        User storedUser = snapshot.toObject(User.class);
        if (storedUser == null) return "Login error";

        // ✅ Check hashed password
        if (!passwordEncoder.matches(user.getPassword(), storedUser.getPassword())) {
            return "Incorrect password";
        }

        return "Login successful";
    }

    public boolean isRegistered(String emailRaw) throws ExecutionException, InterruptedException {
        String email = emailRaw.trim().toLowerCase();
        Firestore db = FirestoreClient.getFirestore();
        return db.collection("users").document(email).get().get().exists();
    }

    public boolean isAuthorized(String emailRaw) {
        String email = emailRaw.trim().toLowerCase();
        return authorizedEmails.contains(email);
    }
}
