package com.sellsmart.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sellsmart.backend.model.User;
import com.sellsmart.backend.util.EmailLoader;
import com.sellsmart.backend.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final Set<String> authorizedEmails = EmailLoader.loadEmails("/etc/secrets/authorized-emails.txt");

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

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

        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        user.setEmail(email);

        ApiFuture<WriteResult> result = docRef.set(user);
        return "Registered successfully at: " + result.get().getUpdateTime();
    }

    public Map<String, String> login(User user) throws ExecutionException, InterruptedException {
        String email = user.getEmail().trim().toLowerCase();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snapshot = db.collection("users").document(email).get().get();

        Map<String, String> response = new HashMap<>();

        if (!snapshot.exists()) {
            response.put("error", "Not registered");
            return response;
        }

        User storedUser = snapshot.toObject(User.class);
        if (storedUser == null) {
            response.put("error", "Login error");
            return response;
        }

        if (!passwordEncoder.matches(user.getPassword(), storedUser.getPassword())) {
            response.put("error", "Incorrect password");
            return response;
        }

        String token = jwtUtil.generateToken(email);
        response.put("token", token);
        return response;
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
