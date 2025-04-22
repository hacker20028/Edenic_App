package com.aryan.edenic.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.aryan.edenic.R;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Stock implements Serializable {
    private final String symbol;
    private final String name;
    private double price;
    private final int logoRes;
    private double previousClose;
    private double dayHigh;
    private double dayLow;
    private double yearHigh;
    private double yearLow;
    private long marketCap;
    private double volume;
    private double peRatio;
    private double dividendYield;
    private long lastUpdated;
    private double changePercent;  // New field for Google Sheets data

    // Constructor
    public Stock(String symbol, String name, double price, int logoRes) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.previousClose = price;
        this.logoRes = logoRes;
        this.lastUpdated = System.currentTimeMillis();
        this.changePercent = 0;  // Initialize to 0
    }

    // Getters
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getLogoRes() { return logoRes; }
    public double getPreviousClose() { return previousClose; }
    public double getDayHigh() { return dayHigh; }
    public double getDayLow() { return dayLow; }
    public double getYearHigh() { return yearHigh; }
    public double getYearLow() { return yearLow; }
    public long getMarketCap() { return marketCap; }
    public double getVolume() { return volume; }
    public double getPeRatio() { return peRatio; }
    public double getDividendYield() { return dividendYield; }
    public long getLastUpdated() { return lastUpdated; }

    // Getter for changePercent with fallback to calculation
    public double getChangePercent() {
        // If we have a pre-fetched value from Google Sheets, use it
        if (changePercent != 0) {
            return changePercent;
        }

        // Otherwise, fall back to calculating it from previous close if available
        if (previousClose > 0) {
            return ((price - previousClose) / previousClose) * 100;
        }

        // Default to 0 if we have no data
        return 0;
    }

    // Setters
    public void setPrice(double price) {
        this.price = price;
        this.lastUpdated = System.currentTimeMillis();
    }
    public void setPreviousClose(double previousClose) { this.previousClose = previousClose; }
    public void setDayHigh(double dayHigh) { this.dayHigh = dayHigh; }
    public void setDayLow(double dayLow) { this.dayLow = dayLow; }
    public void setYearHigh(double yearHigh) { this.yearHigh = yearHigh; }
    public void setYearLow(double yearLow) { this.yearLow = yearLow; }
    public void setMarketCap(long marketCap) { this.marketCap = marketCap; }
    public void setVolume(double volume) { this.volume = volume; }
    public void setPeRatio(double peRatio) { this.peRatio = peRatio; }
    public void setDividendYield(double dividendYield) { this.dividendYield = dividendYield; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }

    // Formatted getters
    public String getFormattedPrice() {
        return NumberFormat.getCurrencyInstance(Locale.US).format(price);
    }

    public String getFormattedMarketCap() {
        if (marketCap >= 1_000_000_000_000L) {
            return String.format(Locale.US, "$%.2fT", marketCap / 1_000_000_000_000.0);
        } else if (marketCap >= 1_000_000_000L) {
            return String.format(Locale.US, "$%.2fB", marketCap / 1_000_000_000.0);
        } else if (marketCap >= 1_000_000L) {
            return String.format(Locale.US, "$%.2fM", marketCap / 1_000_000.0);
        }
        return NumberFormat.getCurrencyInstance(Locale.US).format(marketCap);
    }

    public String getFormattedVolume() {
        if (volume >= 1_000_000_000) {
            return String.format(Locale.US, "%.2fB", volume / 1_000_000_000.0);
        } else if (volume >= 1_000_000) {
            return String.format(Locale.US, "%.2fM", volume / 1_000_000.0);
        }
        return NumberFormat.getNumberInstance(Locale.US).format(volume);
    }

    public String getFormattedChange() {
        // Use the Google Sheets change percent if available
        if (changePercent != 0) {
            String changeSign = changePercent > 0 ? "+" : "";
            return String.format(Locale.US, "%s%.2f%%", changeSign, changePercent);
        }

        // Handle case where previousClose is zero or not set
        if (previousClose <= 0) {
            return "0.00 (0.00%)"; // Or "N/A" if you prefer
        }

        double change = price - previousClose;
        double percentChange = (change / previousClose) * 100;

        String changeSign = change > 0 ? "+" : "";
        return String.format(Locale.US, "%s%.2f (%s%.2f%%)",
                changeSign, change, changeSign, percentChange);
    }

    public int getChangeColor(Context context) {
        // Use Google Sheets change percent if available
        double percentToCheck = changePercent != 0 ? changePercent :
                (previousClose > 0 ? ((price - previousClose) / previousClose) * 100 : 0);

        if (percentToCheck > 0) {
            return ContextCompat.getColor(context, R.color.green);
        } else if (percentToCheck < 0) {
            return ContextCompat.getColor(context, R.color.red);
        } else {
            return ContextCompat.getColor(context, R.color.white);
        }
    }

    public boolean isUp() {
        // Use Google Sheets change percent if available
        if (changePercent != 0) {
            return changePercent > 0;
        }
        return price > previousClose;
    }

    // For Firestore serialization
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", symbol);
        map.put("name", name);
        map.put("price", price);
        map.put("logoRes", logoRes);
        map.put("previousClose", previousClose);
        map.put("dayHigh", dayHigh);
        map.put("dayLow", dayLow);
        map.put("yearHigh", yearHigh);
        map.put("yearLow", yearLow);
        map.put("marketCap", marketCap);
        map.put("volume", volume);
        map.put("peRatio", peRatio);
        map.put("dividendYield", dividendYield);
        map.put("lastUpdated", lastUpdated);
        map.put("changePercent", changePercent);
        return map;
    }

    // Builder pattern
    public static class Builder {
        private String symbol;
        private String name;
        private double price;
        private int logoRes;
        private double previousClose;
        private double dayHigh;
        private double dayLow;
        private double yearHigh;
        private double yearLow;
        private long marketCap;
        private double volume;
        private double peRatio;
        private double dividendYield;
        private double changePercent;

        public Builder setSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setPrice(double price) {
            this.price = price;
            return this;
        }

        public Builder setLogoRes(int logoRes) {
            this.logoRes = logoRes;
            return this;
        }

        public Builder setPreviousClose(double previousClose) {
            this.previousClose = previousClose;
            return this;
        }

        public Builder setChangePercent(double changePercent) {
            this.changePercent = changePercent;
            return this;
        }

        // ... other setters for each field

        public Stock build() {
            Stock stock = new Stock(symbol, name, price, logoRes);
            stock.setPreviousClose(previousClose);
            stock.setDayHigh(dayHigh);
            stock.setDayLow(dayLow);
            stock.setYearHigh(yearHigh);
            stock.setYearLow(yearLow);
            stock.setMarketCap(marketCap);
            stock.setVolume(volume);
            stock.setPeRatio(peRatio);
            stock.setDividendYield(dividendYield);
            stock.setChangePercent(changePercent);
            return stock;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US,
                "%s (%s): $%.2f | Change: %s | Vol: %s | Mkt Cap: %s",
                symbol, name, price, getFormattedChange(),
                getFormattedVolume(), getFormattedMarketCap());
    }
}