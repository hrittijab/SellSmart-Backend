package com.sellsmart.backend.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class EmailLoader {
    public static Set<String> loadEmails(String filePath) {
        Set<String> emails = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                emails.add(line.trim().toLowerCase());
            }
        } catch (Exception e) {
            System.err.println("Could not load authorized emails: " + e.getMessage());
        }
        return emails;
    }
}
