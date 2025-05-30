package com.sellsmart.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Component
public class FirebaseInitializer {

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount =
                        new FileInputStream("src/main/resources/firebase-service-account.json");

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println(" Firebase initialized successfully.");
            } else {
                System.out.println(" Firebase already initialized. Skipping...");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(" Firebase initialization failed.");
        }
    }
}
