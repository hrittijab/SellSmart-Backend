package com.sellsmart.backend.model;

import lombok.Data;

@Data
public class SaleItem {
    private String itemId;
    private String id;
    private String name;
    private int quantitySold;
    private double sellPrice;
    private double buyPrice;
    private String addedByEmail;

    public SaleItem() {}
    public double getTotalEarned() {
        return sellPrice * quantitySold;
    }

    public double getTotalSpent() {
        return buyPrice * quantitySold;
    }
}
