package com.aryan.edenic.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Model class for a stock transaction (buy or sell)
 */
public class Transaction {
    public enum Type {
        BUY, SELL
    }

    private String id;
    private String symbol;
    private String companyName;
    private Type type;
    private int quantity;
    private double price;
    private double totalValue;
    private Date timestamp;
    private double profitLoss; // Only for SELL transactions
    private double profitLossPercentage; // Only for SELL transactions

    // Required no-arg constructor for Firebase
    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = new Date();
    }

    // Constructor for BUY transactions
    public Transaction(String symbol, String companyName, int quantity, double price) {
        this();
        this.symbol = symbol;
        this.companyName = companyName;
        this.type = Type.BUY;
        this.quantity = quantity;
        this.price = price;
        this.totalValue = price * quantity;
    }

    // Constructor for SELL transactions
    public Transaction(String symbol, String companyName, int quantity, double price, double buyPrice) {
        this(symbol, companyName, quantity, price);
        this.type = Type.SELL;
        this.profitLoss = (price - buyPrice) * quantity;
        this.profitLossPercentage = buyPrice > 0 ? ((price - buyPrice) / buyPrice) * 100 : 0;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Type getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public double getProfitLossPercentage() {
        return profitLossPercentage;
    }

    // Setters for Firebase
    public void setId(String id) {
        this.id = id;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setProfitLoss(double profitLoss) {
        this.profitLoss = profitLoss;
    }

    public void setProfitLossPercentage(double profitLossPercentage) {
        this.profitLossPercentage = profitLossPercentage;
    }

    // Method to convert to a Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("symbol", symbol);
        map.put("companyName", companyName);
        map.put("type", type.toString());
        map.put("quantity", quantity);
        map.put("price", price);
        map.put("totalValue", totalValue);
        map.put("timestamp", timestamp.getTime());

        // Only include profit/loss for SELL transactions
        if (type == Type.SELL) {
            map.put("profitLoss", profitLoss);
            map.put("profitLossPercentage", profitLossPercentage);
        }

        return map;
    }

    // Factory method to create from Firebase data
    public static Transaction fromMap(Map<String, Object> map) {
        Transaction transaction = new Transaction();

        transaction.id = (String) map.get("id");
        transaction.symbol = (String) map.get("symbol");
        transaction.companyName = (String) map.get("companyName");
        transaction.type = Type.valueOf((String) map.get("type"));

        // Handle numeric types safely
        transaction.quantity = getIntValue(map.get("quantity"));
        transaction.price = getDoubleValue(map.get("price"));
        transaction.totalValue = getDoubleValue(map.get("totalValue"));

        // Handle timestamp
        Object timestampObj = map.get("timestamp");
        if (timestampObj instanceof Long) {
            transaction.timestamp = new Date((Long) timestampObj);
        } else {
            transaction.timestamp = new Date();
        }

        // Get profit/loss for SELL transactions
        if (transaction.type == Type.SELL) {
            transaction.profitLoss = map.containsKey("profitLoss") ?
                    getDoubleValue(map.get("profitLoss")) : 0;
            transaction.profitLossPercentage = map.containsKey("profitLossPercentage") ?
                    getDoubleValue(map.get("profitLossPercentage")) : 0;
        }

        return transaction;
    }

    // Helper method to safely convert to int
    private static int getIntValue(Object value) {
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return 0;
    }

    // Helper method to safely convert to double
    private static double getDoubleValue(Object value) {
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        return 0;
    }
}