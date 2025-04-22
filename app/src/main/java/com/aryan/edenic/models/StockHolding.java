package com.aryan.edenic.models;

public class StockHolding {
    private int qty;
    private double avgPrice;

    public StockHolding() {}

    public StockHolding(int qty, double avgPrice) {
        this.qty = qty;
        this.avgPrice = avgPrice;
    }

    // Getters and setters
    public int getQty() { return qty; }
    public double getAvgPrice() { return avgPrice; }
}
