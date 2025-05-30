package com.sellsmart.backend.model;



import lombok.Data;

@Data
public class InventoryItem {
    private String id;
    private String name;
    private double buyPrice;
    private double sellPrice;
    private int quantity;
    private String addedByEmail;
}

