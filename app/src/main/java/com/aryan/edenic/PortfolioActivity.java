package com.aryan.edenic;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.adapters.PortfolioAdapter;
import com.aryan.edenic.models.PortfolioItem;
import com.aryan.edenic.yahoo_finance.YahooFinanceClient;
import com.aryan.edenic.yahoo_finance.YahooResponse;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PortfolioActivity extends AppCompatActivity {
    private static final String TAG = "PortfolioActivity";

    // UI Components
    private TextView totalValue;
    private TextView overallGain;
    private TextView overallGainPercent;
    private TextView investedValue;
    private TextView todayGain;
    private TextView todayGainPercent;
    private EditText searchStocks;
    private RecyclerView stocksList;
    private TextView emptyPortfolio;
    private BottomNavigationView bottomNav;
    private ImageView userProfile;
    private TextView userBalance;

    // Data
    private List<PortfolioItem> portfolioItems = new ArrayList<>();
    private PortfolioAdapter adapter;
    private String userId;

    // Portfolio statistics
    private double totalPortfolioValue = 0;
    private double totalInvestedValue = 0;
    private double totalProfitLoss = 0;
    private double profitLossPercent = 0;
    private double availableBalance = 10000;  // Default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        // Get user ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            Log.d(TAG, "User ID: " + userId);
        } else {
            // If no user is logged in, go back to login screen
            startActivity(new Intent(this, Auth_Activity.class));
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();
        setupListeners();

        // Initialize portfolio list
        setupPortfolioList();

        // Load profile photo
        loadProfilePhoto();

        // Load portfolio data
        loadPortfolioData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_portfolio);
        portfolioItems.clear(); // Clear existing items first
        // Refresh data when activity is resumed
        loadPortfolioData();
    }

    private void initializeViews() {
        // Top bar views
        userBalance = findViewById(R.id.user_balance);
        userProfile = findViewById(R.id.user_profile);

        // Portfolio summary views
        totalValue = findViewById(R.id.total_value);
        overallGain = findViewById(R.id.overall_gain);
       // overallGainPercent = findViewById(R.id.overall_gain_percent);
        investedValue = findViewById(R.id.invested_value);
        todayGain = findViewById(R.id.today_gain);
        todayGainPercent = findViewById(R.id.today_gain_percent);

        // Search and list views
        searchStocks = findViewById(R.id.search_stocks);
        stocksList = findViewById(R.id.stocks_list);
        emptyPortfolio = findViewById(R.id.empty_portfolio);

        // Navigation
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void loadProfilePhoto() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(userProfile);
        }
    }

    private void setupListeners() {
        // Search text changed listener
        searchStocks.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (adapter != null) {
                    adapter.getFilter().filter(s.toString());
                }
            }
        });

        // Add profile click listener
        userProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        // Bottom navigation
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_portfolio);
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_discovery) {
                startActivity(new Intent(this, Discover.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (item.getItemId() == R.id.nav_chat) {
                startActivity(new Intent(this, ChatListActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    private void setupPortfolioList() {
        adapter = new PortfolioAdapter(this, portfolioItems);
        stocksList.setLayoutManager(new LinearLayoutManager(this));
        stocksList.setAdapter(adapter);
    }

    private void loadPortfolioData() {
        if (userId == null) {
            Log.e(TAG, "UserId is null, cannot load portfolio data");
            return;
        }

        Log.d(TAG, "Starting to load portfolio data for user: " + userId);

        // Use the new portfolioItems structure in Realtime Database
        FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("portfolioItems")  // New path
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Portfolio items data retrieved: exists = " + snapshot.exists() +
                                ", count = " + snapshot.getChildrenCount());

                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            portfolioItems.clear();
                            totalInvestedValue = 0;

                            // Process stocks in new format
                            for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                                try {
                                    String symbol = stockSnapshot.child("symbol").getValue(String.class);
                                    String companyName = stockSnapshot.child("companyName").getValue(String.class);
                                    double currentPrice = stockSnapshot.child("currentPrice").getValue(Double.class);
                                    int quantity = stockSnapshot.child("quantity").getValue(Integer.class);
                                    double investedAmount = stockSnapshot.child("investedAmount").getValue(Double.class);

                                    Log.d(TAG, String.format("Loading stock: %s, %d shares @ $%.2f",
                                            symbol, quantity, currentPrice));

                                    // Create portfolio item
                                    PortfolioItem item = new PortfolioItem(
                                            symbol,
                                            companyName,
                                            currentPrice,
                                            quantity,
                                            investedAmount
                                    );

                                    portfolioItems.add(item);
                                    totalInvestedValue += investedAmount;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing stock: " + e.getMessage(), e);
                                }
                            }

                            Log.d(TAG, "Loaded " + portfolioItems.size() + " stocks in new format");

                            // Calculate portfolio value
                            totalPortfolioValue = portfolioItems.stream()
                                    .mapToDouble(PortfolioItem::getCurrentValue)
                                    .sum();

                            // Calculate profit/loss
                            totalProfitLoss = totalPortfolioValue - totalInvestedValue;
                            profitLossPercent = totalInvestedValue > 0 ?
                                    (totalProfitLoss / totalInvestedValue) * 100 : 0;

                            // Update UI
                            updatePortfolioSummary();

                            // Calculate available balance
                            availableBalance = 10000 - totalInvestedValue;
                            updateBalanceUI();

                            // Update adapter with a new copy of the list
                            Log.d(TAG, "Updating adapter with " + portfolioItems.size() + " items");
                            adapter.updateItems(new ArrayList<>(portfolioItems));

                            // Show/hide empty view
                            boolean isEmpty = portfolioItems.isEmpty();
                            Log.d(TAG, "Portfolio empty? " + isEmpty);
                            emptyPortfolio.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                            stocksList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                            // Update with latest prices
                            refreshStockPrices();
                        } else {
                            // Fallback to the old structure if new one doesn't exist
                            loadLegacyPortfolioData();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading portfolio: " + error.getMessage());
                        // Try legacy format as fallback
                        loadLegacyPortfolioData();
                    }
                });
    }

    // Legacy loader for backward compatibility
    private void loadLegacyPortfolioData() {
        Log.d(TAG, "Falling back to legacy portfolio data format");

        FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("stocks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Legacy stocks data retrieved: exists = " + snapshot.exists());

                        if (snapshot.exists()) {
                            portfolioItems.clear();
                            totalInvestedValue = 0;

                            // Process stocks in legacy format
                            for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                                try {
                                    String symbol = stockSnapshot.getKey();
                                    double avgPrice = stockSnapshot.child("avgPrice").getValue(Double.class);
                                    int quantity = stockSnapshot.child("qty").getValue(Integer.class);

                                    double investedAmount = avgPrice * quantity;

                                    Log.d(TAG, String.format("Loading legacy stock: %s, %d shares @ $%.2f",
                                            symbol, quantity, avgPrice));

                                    // Create portfolio item
                                    PortfolioItem item = new PortfolioItem(
                                            symbol,
                                            getFullName(symbol),
                                            avgPrice,  // Current price same as avg initially
                                            quantity,
                                            investedAmount
                                    );

                                    portfolioItems.add(item);
                                    totalInvestedValue += investedAmount;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing legacy stock: " + e.getMessage(), e);
                                }
                            }

                            // Continue with the same logic as before...
                            Log.d(TAG, "Loaded " + portfolioItems.size() + " stocks in legacy format");

                            // Update adapter with portfolio items
                            adapter.updateItems(new ArrayList<>(portfolioItems));

                            // Rest of the UI updates...
                            // [Same code as in loadPortfolioData]
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading legacy portfolio: " + error.getMessage());
                        emptyPortfolio.setVisibility(View.VISIBLE);
                        stocksList.setVisibility(View.GONE);
                    }
                });
    }

    private void updateBalanceUI() {
        userBalance.setText(String.format("$%,.2f", availableBalance));
    }

    private void refreshStockPrices() {
        // Update prices for all stocks in portfolio
        for (PortfolioItem item : portfolioItems) {
            YahooFinanceClient.getInstance().getStockData(item.getSymbol(), "1d", "1d")
                    .enqueue(new Callback<YahooResponse>() {
                        @Override
                        public void onResponse(Call<YahooResponse> call, Response<YahooResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                YahooResponse yahooResponse = response.body();

                                if (yahooResponse.chart != null &&
                                        yahooResponse.chart.result != null &&
                                        !yahooResponse.chart.result.isEmpty() &&
                                        yahooResponse.chart.result.get(0) != null &&
                                        yahooResponse.chart.result.get(0).meta != null) {

                                    double price = yahooResponse.chart.result.get(0).meta.regularMarketPrice;
                                    Log.d(TAG, "Updated price for " + item.getSymbol() + ": $" + price);

                                    // Update price
                                    item.updatePrice(price);

                                    // Recalculate portfolio statistics
                                    recalculatePortfolioStats();

                                    // Update in database
                                    updateStockInDatabase(item);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<YahooResponse> call, Throwable t) {
                            Log.e(TAG, "Error fetching price for " + item.getSymbol() + ": " + t.getMessage(), t);
                        }
                    });
        }
    }

    private void updateStockInDatabase(PortfolioItem item) {
        if (userId == null) return;

        // Only update the current price, not the average price
        FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("portfolioValue")
                .setValue(totalPortfolioValue)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating portfolio value", e));
    }

    private void recalculatePortfolioStats() {
        totalPortfolioValue = 0;

        for (PortfolioItem item : portfolioItems) {
            totalPortfolioValue += item.getCurrentValue();
        }

        totalProfitLoss = totalPortfolioValue - totalInvestedValue;
        profitLossPercent = totalInvestedValue > 0 ? (totalProfitLoss / totalInvestedValue) * 100 : 0;

        // Update UI
        runOnUiThread(() -> {
            updatePortfolioSummary();

            // Update the adapter to reflect the new prices/values
            adapter.notifyDataSetChanged();

            // Update leaderboard
            updateLeaderboardEntry();
        });
    }

    private void updatePortfolioSummary() {
        // Format values with proper currency format
        totalValue.setText(String.format(Locale.US, "$%,.2f", totalPortfolioValue));
        investedValue.setText(String.format(Locale.US, "$%,.2f", totalInvestedValue));

        // Format profit/loss
        boolean isProfit = totalProfitLoss >= 0;

        // Get the icons from your layout
        ImageView gainLossIcon = findViewById(R.id.gain_loss_icon);
        ImageView todayIcon = findViewById(R.id.today_gain_icon);

        // Set icon color based on profit/loss if icons exist
        if (gainLossIcon != null) {
            gainLossIcon.setImageResource(isProfit ? R.drawable.ic_trending_up : R.drawable.ic_trending_down);
            gainLossIcon.setColorFilter(getResources().getColor(isProfit ? R.color.green : R.color.red));
        }

        if (todayIcon != null) {
            todayIcon.setImageResource(isProfit ? R.drawable.ic_trending_up : R.drawable.ic_trending_down);
            todayIcon.setColorFilter(getResources().getColor(isProfit ? R.color.green : R.color.red));
        }

        // Format text for gain/loss
        String profitLossSign = isProfit ? "+" : "-";
        int textColor = getResources().getColor(isProfit ? R.color.green : R.color.red);

        // Set overall gain/loss with value and percentage combined
        overallGain.setText(String.format(Locale.US, "%s$%.2f (%s%.2f%%)",
                profitLossSign, Math.abs(totalProfitLoss), profitLossSign, Math.abs(profitLossPercent)));
        overallGain.setTextColor(textColor);

        // Set today's gain/loss with value and percentage
        todayGain.setText(String.format(Locale.US, "%s$%.2f", profitLossSign, Math.abs(totalProfitLoss)));
        todayGain.setTextColor(textColor);

        // Set today's percentage if the view exists
        if (todayGainPercent != null) {
            todayGainPercent.setText(String.format(Locale.US, "(%s%.2f%%)", profitLossSign, Math.abs(profitLossPercent)));
            todayGainPercent.setTextColor(textColor);
        }
    }

    private void updateLeaderboardEntry() {
        if (userId == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Create leaderboard entry
        Map<String, Object> entryData = new HashMap<>();
        entryData.put("name", user.getDisplayName());
        entryData.put("displayName", user.getDisplayName());
        entryData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        entryData.put("portfolioValue", totalPortfolioValue);
        entryData.put("dailyChangePercent", profitLossPercent);
        entryData.put("position", -1);

        // Update in Firebase Realtime Database
        FirebaseDatabase.getInstance().getReference("leaderboard")
                .child(userId)
                .setValue(entryData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Leaderboard entry updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating leaderboard", e));
    }

    // Helper method to get company name from symbol
    private String getFullName(String symbol) {
        Map<String, String> companyNames = new HashMap<>();
        companyNames.put("AAPL", "Apple Inc.");
        companyNames.put("MSFT", "Microsoft Corporation");
        companyNames.put("GOOGL", "Alphabet Inc.");
        companyNames.put("AMZN", "Amazon.com Inc.");
        companyNames.put("META", "Meta Platforms Inc.");
        companyNames.put("TSLA", "Tesla Inc.");
        companyNames.put("NVDA", "NVIDIA Corporation");

        return companyNames.getOrDefault(symbol, symbol + " Inc.");
    }
}