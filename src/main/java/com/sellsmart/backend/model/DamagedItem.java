package com.sellsmart.backend.model;

import lombok.Data;

@Data
public class DamagedItem {
    private String id;
    private String name;
    private String itemId;
    private double buyPrice;
    private int quantityDamaged;
    private String addedByEmail;


}
