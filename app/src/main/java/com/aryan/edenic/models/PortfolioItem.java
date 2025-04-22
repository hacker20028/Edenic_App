package com.aryan.edenic.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PortfolioItem implements Serializable {
    private final String symbol;
    private final String companyName;
    private double currentPrice;
    private double avgPurchasePrice;
    private int quantity;
    private double investedAmount;
    private long lastUpdated;

    public PortfolioItem(String symbol, String companyName, double currentPrice,
                         int quantity, double investedAmount) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.avgPurchasePrice = investedAmount / quantity;
        this.quantity = quantity;
        this.investedAmount = investedAmount;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Core Methods
    public void addShares(int additionalQuantity, double additionalInvestment) {
        this.avgPurchasePrice = ((this.investedAmount + additionalInvestment) /
                (this.quantity + additionalQuantity));
        this.quantity += additionalQuantity;
        this.investedAmount += additionalInvestment;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void removeShares(int quantityToSell) {
        if (quantityToSell > quantity) {
            throw new IllegalArgumentException("Cannot sell more shares than owned");
        }
        this.quantity -= quantityToSell;
        this.investedAmount = avgPurchasePrice * quantity;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void updatePrice(double newPrice) {
        this.currentPrice = newPrice;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public double getCurrentPrice() { return currentPrice; }
    public double getAvgPurchasePrice() { return avgPurchasePrice; }
    public int getQuantity() { return quantity; }
    public double getInvestedAmount() { return investedAmount; }
    public long getLastUpdated() { return lastUpdated; }

    // Calculated Properties
    public double getCurrentValue() {
        return currentPrice * quantity;
    }

    public double getProfitLoss() {
        return getCurrentValue() - investedAmount;
    }

    public double getProfitLossPercentage() {
        return (getProfitLoss() / investedAmount) * 100;
    }

    // Formatting Helpers
    public String getFormattedCurrentValue() {
        return formatCurrency(getCurrentValue());
    }

    public String getFormattedProfitLoss() {
        return formatCurrency(getProfitLoss());
    }

    public String getFormattedProfitLossPercentage() {
        return String.format(Locale.US, "%.2f%%", getProfitLossPercentage());
    }

    private String formatCurrency(double value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }

    // For debugging
    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US,
                "%s (%s): %d shares @ $%.2f (Avg: $%.2f) → Val: %s (%s)",
                symbol, companyName, quantity, currentPrice, avgPurchasePrice,
                getFormattedCurrentValue(), getFormattedProfitLossPercentage());
    }

    // For Firestore integration (optional)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", symbol);
        map.put("companyName", companyName);
        map.put("currentPrice", currentPrice);
        map.put("avgPurchasePrice", avgPurchasePrice);
        map.put("quantity", quantity);
        map.put("investedAmount", investedAmount);
        map.put("lastUpdated", lastUpdated);
        return map;
    }

    public static class Builder {
        private String symbol;
        private String companyName;
        private double currentPrice;
        private int quantity;
        private double investedAmount;

        public Builder setSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder setCompanyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder setCurrentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder setInvestedAmount(double investedAmount) {
            this.investedAmount = investedAmount;
            return this;
        }

        public PortfolioItem build() {
            return new PortfolioItem(symbol, companyName, currentPrice, quantity, investedAmount);
        }
    }
}