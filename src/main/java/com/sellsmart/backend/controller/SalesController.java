package com.sellsmart.backend.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sellsmart.backend.model.InventoryItem;
import com.sellsmart.backend.model.SaleItem;
import com.sellsmart.backend.model.DailyProfit;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "http://localhost:3000")
public class SalesController {

    @PostMapping("/add")
    public String addSales(@RequestParam String email, @RequestParam String date,
                           @RequestBody List<SaleItem> sales) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference inventoryRef = db.collection("users").document(email).collection("inventory");
        CollectionReference salesRef = db.collection("users").document(email)
                                         .collection("sales").document(date).collection("entries");

        for (SaleItem sale : sales) {
            ApiFuture<QuerySnapshot> future = inventoryRef.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            InventoryItem matchedItem = null;
            for (QueryDocumentSnapshot doc : documents) {
                InventoryItem invItem = doc.toObject(InventoryItem.class);
                if (invItem.getName().trim().equalsIgnoreCase(sale.getName().trim())) {
                    matchedItem = invItem;
                    break;
                }
            }

            if (matchedItem != null) {
                String itemId = matchedItem.getId();

                Query existingSaleQuery = salesRef.whereEqualTo("itemId", itemId);
                List<QueryDocumentSnapshot> existingDocs = existingSaleQuery.get().get().getDocuments();

                if (!existingDocs.isEmpty()) {
                    DocumentSnapshot existingDoc = existingDocs.get(0);
                    SaleItem existingSale = existingDoc.toObject(SaleItem.class);

                    int updatedQty = existingSale.getQuantitySold() + sale.getQuantitySold();
                    existingSale.setQuantitySold(updatedQty);
                    existingSale.setSellPrice(matchedItem.getSellPrice());
                    existingSale.setBuyPrice(matchedItem.getBuyPrice());
                    existingSale.setName(matchedItem.getName());

                    salesRef.document(existingDoc.getId()).set(existingSale).get();
                } else {
                    sale.setItemId(itemId);
                    sale.setBuyPrice(matchedItem.getBuyPrice());
                    sale.setSellPrice(matchedItem.getSellPrice());
                    sale.setName(matchedItem.getName());
                    sale.setAddedByEmail(email);
                    sale.setId(salesRef.document().getId());

                    salesRef.document(sale.getId()).set(sale).get();
                }

                int newQty = Math.max(0, matchedItem.getQuantity() - sale.getQuantitySold());
                matchedItem.setQuantity(newQty);
                inventoryRef.document(itemId).set(matchedItem).get();
            }
        }

        return "Sales recorded and inventory updated.";
    }

    @DeleteMapping("/delete/{saleId}")
    public String deleteSale(@RequestParam String email, @RequestParam String date,
                             @PathVariable String saleId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference saleDoc = db.collection("users").document(email)
                                      .collection("sales").document(date)
                                      .collection("entries").document(saleId);

        DocumentSnapshot saleSnap = saleDoc.get().get();
        if (!saleSnap.exists()) return "Sale not found";

        SaleItem sale = saleSnap.toObject(SaleItem.class);
        String itemId = sale.getItemId();
        int quantitySold = sale.getQuantitySold();

        DocumentReference invRef = db.collection("users").document(email).collection("inventory").document(itemId);
        DocumentSnapshot invSnap = invRef.get().get();
        if (invSnap.exists()) {
            InventoryItem item = invSnap.toObject(InventoryItem.class);
            item.setQuantity(item.getQuantity() + quantitySold);
            invRef.set(item).get();
        }

        saleDoc.delete().get();
        return "✅ Sale deleted and inventory restored.";
    }

    @GetMapping("/view")
    public List<SaleItem> viewSales(@RequestParam String email, @RequestParam String date) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference salesRef = db.collection("users").document(email)
                                         .collection("sales").document(date).collection("entries");

        ApiFuture<QuerySnapshot> future = salesRef.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<SaleItem> sales = new ArrayList<>();

        for (DocumentSnapshot doc : documents) {
            SaleItem item = doc.toObject(SaleItem.class);
            item.setId(doc.getId());
            sales.add(item);
        }

        return sales;
    }

    @GetMapping("/profit-summary")
    public List<DailyProfit> getMonthlyProfitSummary(@RequestParam String email, @RequestParam String month) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference salesRoot = db.collection("users").document(email).collection("sales");

        List<DailyProfit> summary = new ArrayList<>();

        Iterable<DocumentReference> salesDateRefs = salesRoot.listDocuments();

        for (DocumentReference dateRef : salesDateRefs) {
            String date = dateRef.getId();

            if (date.startsWith(month)) {
                CollectionReference entriesRef = dateRef.collection("entries");
                List<QueryDocumentSnapshot> entries = entriesRef.get().get().getDocuments();

                double earned = 0;
                double spent = 0;

                for (DocumentSnapshot sale : entries) {
                    Double sellPrice = sale.getDouble("sellPrice");
                    Double buyPrice = sale.getDouble("buyPrice");
                    Long quantity = sale.getLong("quantitySold");

                    if (sellPrice != null && buyPrice != null && quantity != null) {
                        earned += sellPrice * quantity;
                        spent += buyPrice * quantity;
                    }
                }

                double profit = earned - spent;
                summary.add(new DailyProfit(date, profit));
            }
        }

        return summary;
    }
    @PutMapping("/update/{saleId}")
    public String updateSale(@RequestParam String email, @RequestParam String date,
                            @PathVariable String saleId,
                            @RequestBody SaleItem updatedSale) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference saleDoc = db.collection("users").document(email)
                                    .collection("sales").document(date)
                                    .collection("entries").document(saleId);

        DocumentSnapshot saleSnap = saleDoc.get().get();
        if (!saleSnap.exists()) return "Sale not found.";

        SaleItem oldSale = saleSnap.toObject(SaleItem.class);
        String itemId = oldSale.getItemId();
        int oldQty = oldSale.getQuantitySold();
        int newQty = updatedSale.getQuantitySold();

        DocumentReference invRef = db.collection("users").document(email).collection("inventory").document(itemId);
        DocumentSnapshot invSnap = invRef.get().get();

        if (invSnap.exists()) {
            InventoryItem item = invSnap.toObject(InventoryItem.class);

            int adjustedQty = item.getQuantity() + oldQty - newQty;
            item.setQuantity(Math.max(0, adjustedQty)); // avoid negative quantity

            invRef.set(item).get();
        }

        oldSale.setQuantitySold(newQty);
        saleDoc.set(oldSale).get();

        return "✅ Sale updated and inventory adjusted.";
    }


    @GetMapping("/yearly-profit-summary")
    public List<DailyProfit> getYearlyProfitSummary(@RequestParam String email, @RequestParam String year) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference salesRoot = db.collection("users").document(email).collection("sales");

        List<DailyProfit> summary = new ArrayList<>();

        double[] monthlyProfits = new double[12];

        Iterable<DocumentReference> salesDateRefs = salesRoot.listDocuments();

        for (DocumentReference dateRef : salesDateRefs) {
            String date = dateRef.getId();

            if (date.startsWith(year)) {
                String[] parts = date.split("-");
                int monthIndex = Integer.parseInt(parts[1]) - 1;

                CollectionReference entriesRef = dateRef.collection("entries");
                List<QueryDocumentSnapshot> entries = entriesRef.get().get().getDocuments();

                double earned = 0;
                double spent = 0;

                for (DocumentSnapshot sale : entries) {
                    Double sellPrice = sale.getDouble("sellPrice");
                    Double buyPrice = sale.getDouble("buyPrice");
                    Long quantity = sale.getLong("quantitySold");

                    if (sellPrice != null && buyPrice != null && quantity != null) {
                        earned += sellPrice * quantity;
                        spent += buyPrice * quantity;
                    }
                }

                double profit = earned - spent;
                monthlyProfits[monthIndex] += profit;
            }
        }

        String[] monthNames = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };

        for (int i = 0; i < 12; i++) {
            summary.add(new DailyProfit(monthNames[i], monthlyProfits[i]));
        }

        return summary;
    }
    @GetMapping("/between")
    public List<SaleItem> getSalesBetweenDates(@RequestParam String email,
                                            @RequestParam String from,
                                            @RequestParam String to) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference salesRoot = db.collection("users").document(email).collection("sales");

        List<SaleItem> allSales = new ArrayList<>();
        Iterable<DocumentReference> salesDates = salesRoot.listDocuments();

        for (DocumentReference dateRef : salesDates) {
            String date = dateRef.getId();

            // If the date falls within the given range (inclusive)
            if (date.compareTo(from) >= 0 && date.compareTo(to) <= 0) {
                CollectionReference entries = dateRef.collection("entries");
                List<QueryDocumentSnapshot> docs = entries.get().get().getDocuments();

                for (DocumentSnapshot doc : docs) {
                    SaleItem item = doc.toObject(SaleItem.class);
                    item.setId(doc.getId());
                    allSales.add(item);
                }
            }
        }

        return allSales;
    }

} 
