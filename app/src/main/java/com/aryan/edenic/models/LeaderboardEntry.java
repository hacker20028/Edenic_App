package com.aryan.edenic.models;

import java.util.Locale;

public class LeaderboardEntry {
    private String name;
    private String photoUrl;
    private double portfolioValue;
    private double dailyChangePercent;
    private int position = -1; // Only used for current user outside top 10
    private String userId;

    // Default constructor for Firebase
    public LeaderboardEntry() {}

    public LeaderboardEntry(String name, String photoUrl, double portfolioValue, double dailyChangePercent) {
        this.name = name;
        this.photoUrl = photoUrl;
        this.portfolioValue = portfolioValue;
        this.dailyChangePercent = dailyChangePercent;
    }

    public String getDisplayName() {
        if (position > 0) {
            return String.format(Locale.US, "#%d. %s", position, name);
        }
        return name;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public double getPortfolioValue() { return portfolioValue; }
    public void setPortfolioValue(double portfolioValue) { this.portfolioValue = portfolioValue; }
    public double getDailyChangePercent() { return dailyChangePercent; }
    public void setDailyChangePercent(double dailyChangePercent) { this.dailyChangePercent = dailyChangePercent; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
