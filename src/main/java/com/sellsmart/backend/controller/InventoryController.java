package com.sellsmart.backend.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sellsmart.backend.model.InventoryItem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final String USERS_COLLECTION = "users";

    @PostMapping("/add")
    public String addItem(HttpServletRequest request, @RequestBody InventoryItem item) throws ExecutionException, InterruptedException {
        String email = (String) request.getAttribute("userEmail");
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference userInventory = db.collection(USERS_COLLECTION).document(email).collection("inventory");
        DocumentReference docRef = userInventory.document();
        item.setId(docRef.getId());
        item.setAddedByEmail(email);
        docRef.set(item).get();
        return "Item added successfully for " + email;
    }

    @PutMapping("/update/{id}")
    public String updateItem(HttpServletRequest request, @PathVariable String id, @RequestBody InventoryItem updatedItem) throws ExecutionException, InterruptedException {
        String email = (String) request.getAttribute("userEmail");
        Firestore db = FirestoreClient.getFirestore();
        updatedItem.setId(id);
        db.collection(USERS_COLLECTION).document(email).collection("inventory").document(id).set(updatedItem).get();
        return "Item updated successfully for " + email;
    }

    @DeleteMapping("/delete/{id}")
    public String deleteItem(HttpServletRequest request, @PathVariable String id) throws ExecutionException, InterruptedException {
        String email = (String) request.getAttribute("userEmail");
        Firestore db = FirestoreClient.getFirestore();
        db.collection(USERS_COLLECTION).document(email).collection("inventory").document(id).delete().get();
        return "Item deleted successfully for " + email;
    }

    @GetMapping("/all")
    public List<InventoryItem> getAllItems(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String email = (String) request.getAttribute("userEmail");
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection(USERS_COLLECTION).document(email).collection("inventory").get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        List<InventoryItem> items = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            InventoryItem item = doc.toObject(InventoryItem.class);
            items.add(item);
        }
        return items;
    }

    @GetMapping("/list")
    public List<String> getInventoryItems(HttpServletRequest request) throws ExecutionException, InterruptedException {
        String email = (String) request.getAttribute("userEmail");
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference inventoryRef = db.collection(USERS_COLLECTION).document(email).collection("inventory");
        List<QueryDocumentSnapshot> docs = inventoryRef.get().get().getDocuments();

        List<String> itemNames = new ArrayList<>();
        for (QueryDocumentSnapshot doc : docs) {
            String name = doc.getString("name");
            if (name != null) itemNames.add(name);
        }

        return itemNames;
    }

    @GetMapping("/get-by-name")
    public InventoryItem getInventoryItemByName(HttpServletRequest request, @RequestParam String name) throws ExecutionException, InterruptedException {
        String email = (String) request.getAttribute("userEmail");
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference inventoryRef = db.collection(USERS_COLLECTION).document(email).collection("inventory");
        Query query = inventoryRef.whereEqualTo("name", name);
        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        if (!docs.isEmpty()) {
            return docs.get(0).toObject(InventoryItem.class);
        } else {
            throw new RuntimeException("Inventory item not found for: " + name);
        }
    }
}
