package com.sellsmart.backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sellsmart.backend.model.User;
import com.sellsmart.backend.util.EmailLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    // ✅ Load from Render secret file path
    private final Set<String> authorizedEmails = EmailLoader.loadEmails("/etc/secrets/authorized-emails.txt");

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String register(User user) throws ExecutionException, InterruptedException {
        String email = user.getEmail().trim().toLowerCase();

        System.out.println("🔍 Attempting to register email: " + email);
        System.out.println("✅ Authorized list: " + authorizedEmails);

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

    public String login(User user) throws ExecutionException, InterruptedException {
        String email = user.getEmail().trim().toLowerCase();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snapshot = db.collection("users").document(email).get().get();

        if (!snapshot.exists()) return "Not registered";

        User storedUser = snapshot.toObject(User.class);
        if (storedUser == null) return "Login error";

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
