package com.sellsmart.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailyProfit {
    private String date;
    private double profit;
}
