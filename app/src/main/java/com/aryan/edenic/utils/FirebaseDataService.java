package com.aryan.edenic.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.aryan.edenic.models.LeaderboardEntry;
import com.aryan.edenic.models.PortfolioItem;
import com.aryan.edenic.models.StockHolding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to handle all Firebase Realtime Database operations
 */
public class FirebaseDataService {
    private static final String TAG = "FirebaseDataService";

    private final FirebaseDatabase database;
    private final String userId;
    private final DatabaseReference userRef;
    private final DatabaseReference leaderboardRef;

    public interface LeaderboardListener {
        void onLeaderboardUpdated(List<LeaderboardEntry> topEntries, LeaderboardEntry currentUserEntry, int currentUserPosition);
    }

    public interface PortfolioListener {
        void onPortfolioUpdated(double portfolioValue, double dailyChangePercent, List<PortfolioItem> items);
    }

    public FirebaseDataService() {
        database = FirebaseDatabase.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            userId = user.getUid();
            userRef = database.getReference("users").child(userId);
            leaderboardRef = database.getReference("leaderboard");

            // Initialize user data if it doesn't exist
            initializeUserData(user);
        } else {
            userId = "";
            userRef = null;
            leaderboardRef = null;
            Log.e(TAG, "No user logged in!");
        }
    }

    /**
     * Initialize user data in Firebase if it doesn't exist
     */
    private void initializeUserData(FirebaseUser user) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // User doesn't exist in the database yet, create initial data
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", user.getDisplayName());
                    userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                    userData.put("portfolioValue", 10000.0); // Starting value
                    userData.put("dailyChangePercent", 0.0);
                    userData.put("lastUpdated", ServerValue.TIMESTAMP);

                    userRef.setValue(userData);

                    // Also add to leaderboard
                    LeaderboardEntry entry = new LeaderboardEntry(
                            user.getDisplayName(),
                            user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                            10000.0,
                            0.0
                    );
                    leaderboardRef.child(userId).setValue(entry);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error initializing user data", error.toException());
            }
        });
    }

    /**
     * Update portfolio data in Firebase
     */
    public void updatePortfolio(double portfolioValue, double dailyChangePercent) {
        if (userRef == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("portfolioValue", portfolioValue);
        updates.put("dailyChangePercent", dailyChangePercent);
        updates.put("lastUpdated", ServerValue.TIMESTAMP);

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Portfolio updated successfully");

                    // Also update leaderboard
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        LeaderboardEntry entry = new LeaderboardEntry(
                                user.getDisplayName(),
                                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                                portfolioValue,
                                dailyChangePercent
                        );
                        leaderboardRef.child(userId).setValue(entry);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error updating portfolio", e));
    }

    /**
     * Update a specific stock holding
     */
    public void updateStockHolding(String symbol, int quantity, double avgPrice) {
        if (userRef == null) return;

        DatabaseReference stockRef = userRef.child("stocks").child(symbol);

        if (quantity <= 0) {
            // Remove stock if quantity is zero or negative
            stockRef.removeValue();
        } else {
            StockHolding holding = new StockHolding(quantity, avgPrice);
            stockRef.setValue(holding);
        }
    }

    /**
     * Get current user's portfolio
     */
    public void getPortfolio(PortfolioListener listener) {
        if (userRef == null) {
            listener.onPortfolioUpdated(10000.0, 0.0, new ArrayList<>());
            return;
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double portfolioValue = snapshot.child("portfolioValue").exists() ?
                        snapshot.child("portfolioValue").getValue(Double.class) : 10000.0;
                double dailyChangePercent = snapshot.child("dailyChangePercent").exists() ?
                        snapshot.child("dailyChangePercent").getValue(Double.class) : 0.0;

                List<PortfolioItem> items = new ArrayList<>();

                // Get stock holdings
                if (snapshot.child("stocks").exists()) {
                    for (DataSnapshot stockSnapshot : snapshot.child("stocks").getChildren()) {
                        try {
                            String symbol = stockSnapshot.getKey();
                            StockHolding holding = stockSnapshot.getValue(StockHolding.class);

                            if (symbol != null && holding != null) {
                                PortfolioItem item = new PortfolioItem.Builder()
                                        .setSymbol(symbol)
                                        .setCompanyName(symbol) // You can fetch company name separately
                                        .setCurrentPrice(holding.getAvgPrice()) // Current price will be updated later
                                        .setQuantity(holding.getQty())
                                        .setInvestedAmount(holding.getAvgPrice() * holding.getQty())
                                        .build();

                                items.add(item);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing stock holding", e);
                        }
                    }
                }

                listener.onPortfolioUpdated(portfolioValue, dailyChangePercent, items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading portfolio", error.toException());
                listener.onPortfolioUpdated(10000.0, 0.0, new ArrayList<>());
            }
        });
    }

    /**
     * Listen to leaderboard updates
     */
    public void setupLeaderboardListener(LeaderboardListener listener) {
        if (leaderboardRef == null) return;

        // Use limitToLast to get the highest values first, since Firebase orders ascending by default
        leaderboardRef.orderByChild("portfolioValue").limitToLast(20).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LeaderboardEntry> allEntries = new ArrayList<>();
                LeaderboardEntry currentUserEntry = null;

                // 1. Collect all entries and find current user
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    try {
                        LeaderboardEntry entry = userSnapshot.getValue(LeaderboardEntry.class);
                        if (entry != null) {
                            entry.setUserId(userSnapshot.getKey());

                            // Check if this is the current user
                            if (userSnapshot.getKey().equals(userId)) {
                                currentUserEntry = entry;
                            }

                            allEntries.add(entry);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing leaderboard entry", e);
                    }
                }

                // Since Firebase returns in ascending order with limitToLast, we need to reverse
                Collections.reverse(allEntries);

                // Double-check sort by portfolio value (descending)
                allEntries.sort((o1, o2) -> {
                    double val1 = o1.getPortfolioValue();
                    double val2 = o2.getPortfolioValue();
                    Log.d(TAG, "Comparing: " + o1.getName() + "($" + val1 +
                            ") vs " + o2.getName() + "($" + val2 + ")");
                    return Double.compare(val2, val1);
                });

                // Find current user's position (if found)
                int currentUserPosition = -1;
                if (currentUserEntry != null) {
                    for (int i = 0; i < allEntries.size(); i++) {
                        if (allEntries.get(i).getUserId().equals(userId)) {
                            currentUserPosition = i + 1; // 1-based position
                            currentUserEntry.setPosition(currentUserPosition);
                            break;
                        }
                    }
                }

                // Get top 10 entries (or less if not enough)
                List<LeaderboardEntry> topEntries = new ArrayList<>();
                int topCount = Math.min(10, allEntries.size());
                for (int i = 0; i < topCount; i++) {
                    topEntries.add(allEntries.get(i));
                }

                // Deliver results
                listener.onLeaderboardUpdated(topEntries, currentUserEntry, currentUserPosition);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading leaderboard", error.toException());
            }
        });
    }
}