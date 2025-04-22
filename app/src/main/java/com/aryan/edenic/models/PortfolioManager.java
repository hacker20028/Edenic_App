package com.aryan.edenic.models;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aryan.edenic.yahoo_finance.YahooFinanceClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortfolioManager {
    private static final String TAG = "PortfolioManager";
    private static final long UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private final FirebaseDatabase database;
    private final String userId;
    private Handler updateHandler;
    private volatile boolean isUpdating = false;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public PortfolioManager(String userId) {
        this.userId = userId;
        this.database = FirebaseDatabase.getInstance();
    }

    public void startAutoUpdates() {
        if (updateHandler != null) {
            // Already started
            return;
        }

        updateHandler = new Handler(Looper.getMainLooper());
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
        Log.d(TAG, "Auto updates started");
    }

    public void stopAutoUpdates() {
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateRunnable);
            updateHandler = null;
            Log.d(TAG, "Auto updates stopped");
        }
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isUpdating) {
                updatePortfolio();
            }

            // Schedule next update regardless of whether this one completes
            if (updateHandler != null) {
                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

    public void updatePortfolio() {
        if (isUpdating) {
            Log.d(TAG, "Update already in progress, skipping");
            return;
        }

        isUpdating = true;
        Log.d(TAG, "Starting portfolio update");

        database.getReference("users").child(userId).child("stocks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Create a final copy of holdings to use in lambda
                        final Map<String, StockHolding> holdings = new HashMap<>();

                        // Collect all holdings
                        for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                            try {
                                StockHolding holding = stockSnapshot.getValue(StockHolding.class);
                                if (holding != null) {
                                    holdings.put(stockSnapshot.getKey(), holding);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing stock holding: " + e.getMessage());
                            }
                        }

                        // Process holdings on background thread with the final copy
                        executorService.execute(() -> {
                            try {
                                fetchLatestPricesAndUpdate(holdings);
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating portfolio: " + e.getMessage(), e);
                            } finally {
                                isUpdating = false;
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading stocks", error.toException());
                        isUpdating = false;
                    }
                });
    }

    private void fetchLatestPricesAndUpdate(final Map<String, StockHolding> holdings) {
        if (holdings.isEmpty()) {
            Log.d(TAG, "No holdings to update");
            return;
        }

        // Use final variables for all values that will be used in the lambda
        final double[] portfolioValues = calculatePortfolioValues(holdings);
        final double newPortfolioValue = portfolioValues[0];
        final double previousPortfolioValue = portfolioValues[1];

        // Skip update if we couldn't get any prices
        if (newPortfolioValue <= 0 || previousPortfolioValue <= 0) {
            Log.w(TAG, "Invalid portfolio values, skipping update");
            return;
        }

        // Calculate change
        final double dailyChangePercent = ((newPortfolioValue - previousPortfolioValue) / previousPortfolioValue) * 100;

        // Update Firebase on main thread with final variables
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Update user portfolio value
                DatabaseReference userRef = database.getReference("users").child(userId);
                userRef.child("portfolioValue").setValue(newPortfolioValue);
                userRef.child("dailyChangePercent").setValue(dailyChangePercent);

                // Update leaderboard
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    String name = auth.getCurrentUser().getDisplayName();
                    String photoUrl = "";
                    if (auth.getCurrentUser().getPhotoUrl() != null) {
                        photoUrl = auth.getCurrentUser().getPhotoUrl().toString();
                    }

                    LeaderboardEntry entry = new LeaderboardEntry(
                            name,
                            photoUrl,
                            newPortfolioValue,
                            dailyChangePercent
                    );

                    database.getReference("leaderboard").child(userId).setValue(entry);
                    Log.d(TAG, "Leaderboard updated: " + newPortfolioValue);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating Firebase: " + e.getMessage(), e);
            }
        });
    }

    // Helper method to calculate portfolio values
    private double[] calculatePortfolioValues(Map<String, StockHolding> holdings) {
        double newPortfolioValue = 0;
        double previousPortfolioValue = 0;

        for (Map.Entry<String, StockHolding> entry : holdings.entrySet()) {
            try {
                String symbol = entry.getKey();
                StockHolding holding = entry.getValue();

                if (holding.getQty() <= 0) {
                    continue; // Skip empty holdings
                }

                // Get current price
                double currentPrice = 0;
                try {
                    currentPrice = YahooFinanceClient.getCurrentPrice(symbol);

                    // Skip if price fetch failed
                    if (currentPrice <= 0) {
                        Log.w(TAG, "Failed to get valid price for " + symbol);
                        continue;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error fetching price for " + symbol, e);
                    continue; // Skip this stock but continue with others
                }

                // Add to totals
                newPortfolioValue += currentPrice * holding.getQty();
                previousPortfolioValue += holding.getAvgPrice() * holding.getQty();

                Log.d(TAG, "Updated " + symbol + " price: " + currentPrice);
            } catch (Exception e) {
                Log.e(TAG, "Error processing holding: " + e.getMessage(), e);
                // Continue with next holding
            }
        }

        return new double[] { newPortfolioValue, previousPortfolioValue };
    }
}