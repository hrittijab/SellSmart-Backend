package com.sellsmart.backend.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sellsmart.backend.model.DamagedItem;
import com.sellsmart.backend.model.InventoryItem;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/damages")
@CrossOrigin(origins = "https://sellsmart2025.netlify.app")
public class DamagedGoodsController {

    @PostMapping("/report")
    public String reportDamagedGoods(@RequestParam String email, @RequestParam String date,
                                     @RequestBody List<DamagedItem> damagedItems) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference inventoryRef = db.collection("users").document(email).collection("inventory");
        CollectionReference damageRef = db.collection("users").document(email)
                                          .collection("damages").document(date).collection("entries");

        for (DamagedItem item : damagedItems) {
            ApiFuture<QuerySnapshot> future = inventoryRef.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            InventoryItem matchedItem = null;
            for (QueryDocumentSnapshot doc : documents) {
                InventoryItem invItem = doc.toObject(InventoryItem.class);
                if (invItem.getName().trim().equalsIgnoreCase(item.getName().trim())) {
                    matchedItem = invItem;
                    break;
                }
            }

            if (matchedItem != null) {
                String itemId = matchedItem.getId();

                Query existingDamageQuery = damageRef.whereEqualTo("itemId", itemId);
                List<QueryDocumentSnapshot> existingDocs = existingDamageQuery.get().get().getDocuments();

                if (!existingDocs.isEmpty()) {
                    DocumentSnapshot existingDoc = existingDocs.get(0);
                    DamagedItem existingDamage = existingDoc.toObject(DamagedItem.class);

                    int updatedQty = existingDamage.getQuantityDamaged() + item.getQuantityDamaged();
                    existingDamage.setQuantityDamaged(updatedQty);
                    existingDamage.setBuyPrice(matchedItem.getBuyPrice());
                    existingDamage.setName(matchedItem.getName());

                    damageRef.document(existingDoc.getId()).set(existingDamage).get();
                    System.out.println("Updated existing damaged entry for item: " + matchedItem.getName());

                } else {
                    item.setItemId(itemId);
                    item.setBuyPrice(matchedItem.getBuyPrice());
                    item.setName(matchedItem.getName()); 
                    item.setAddedByEmail(email);
                    item.setId(damageRef.document().getId());

                    damageRef.document(item.getId()).set(item).get();
                    System.out.println("➕ Created new damage entry for item: " + matchedItem.getName());
                }

                int newQty = Math.max(0, matchedItem.getQuantity() - item.getQuantityDamaged());
                matchedItem.setQuantity(newQty);
                inventoryRef.document(itemId).set(matchedItem).get();
                System.out.println("Inventory updated for damaged item: " + matchedItem.getName());
            } else {
                System.out.println("No matching inventory item found for: " + item.getName());
            }
        }

        return "Damaged goods recorded and inventory updated.";
    }

    @GetMapping("/view")
    public List<DamagedItem> getDamagedGoods(@RequestParam String email, @RequestParam String date) throws Exception {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference damageRef = db.collection("users").document(email)
                                          .collection("damages").document(date)
                                          .collection("entries");

        ApiFuture<QuerySnapshot> future = damageRef.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<DamagedItem> damagedItems = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            damagedItems.add(doc.toObject(DamagedItem.class));
        }

        return damagedItems;
    }
    @PutMapping("/update/{damageId}")
    public String updateDamagedItem(@RequestParam String email, @RequestParam String date,
                                    @PathVariable String damageId,
                                    @RequestBody DamagedItem updatedItem) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference damageDoc = db.collection("users").document(email)
                                        .collection("damages").document(date)
                                        .collection("entries").document(damageId);

        DocumentSnapshot damageSnap = damageDoc.get().get();
        if (!damageSnap.exists()) return "Damage entry not found.";

        DamagedItem oldItem = damageSnap.toObject(DamagedItem.class);
        String itemId = oldItem.getItemId();

        DocumentReference invRef = db.collection("users").document(email).collection("inventory").document(itemId);
        DocumentSnapshot invSnap = invRef.get().get();

        if (invSnap.exists()) {
            InventoryItem item = invSnap.toObject(InventoryItem.class);

            int adjustedQty = item.getQuantity() + oldItem.getQuantityDamaged() - updatedItem.getQuantityDamaged();
            item.setQuantity(Math.max(0, adjustedQty));

            invRef.set(item).get();
        }

        oldItem.setQuantityDamaged(updatedItem.getQuantityDamaged());
        damageDoc.set(oldItem).get();

        return "✅ Damaged item updated and inventory adjusted.";
    }
    @DeleteMapping("/delete/{damageId}")
    public String deleteDamagedItem(@RequestParam String email, @RequestParam String date,
                                    @PathVariable String damageId) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference damageDoc = db.collection("users").document(email)
                                        .collection("damages").document(date)
                                        .collection("entries").document(damageId);

        DocumentSnapshot damageSnap = damageDoc.get().get();
        if (!damageSnap.exists()) return "Damage entry not found.";

        DamagedItem damage = damageSnap.toObject(DamagedItem.class);
        String itemId = damage.getItemId();
        int qty = damage.getQuantityDamaged();

        DocumentReference invRef = db.collection("users").document(email).collection("inventory").document(itemId);
        DocumentSnapshot invSnap = invRef.get().get();
        if (invSnap.exists()) {
            InventoryItem item = invSnap.toObject(InventoryItem.class);
            item.setQuantity(item.getQuantity() + qty); 
            invRef.set(item).get();
        }

        damageDoc.delete().get();
        return "✅ Damaged entry deleted and inventory restored.";
    }
    @GetMapping("/between")
    public List<DamagedItem> getDamagedBetweenDates(@RequestParam String email,
                                                    @RequestParam String from,
                                                    @RequestParam String to) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference damagesRoot = db.collection("users").document(email).collection("damages");

        List<DamagedItem> result = new ArrayList<>();
        Iterable<DocumentReference> damageDateDocs = damagesRoot.listDocuments();

        for (DocumentReference dateDoc : damageDateDocs) {
            String date = dateDoc.getId();

            if (date.compareTo(from) >= 0 && date.compareTo(to) <= 0) {
                CollectionReference entries = dateDoc.collection("entries");
                List<QueryDocumentSnapshot> entriesDocs = entries.get().get().getDocuments();

                for (DocumentSnapshot entryDoc : entriesDocs) {
                    DamagedItem item = entryDoc.toObject(DamagedItem.class);
                    item.setId(entryDoc.getId());
                    result.add(item);
                }
            }
        }

        return result;
    }

}
