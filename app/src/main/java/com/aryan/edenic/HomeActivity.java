package com.aryan.edenic;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aryan.edenic.adapters.LeaderboardAdapter;
import com.aryan.edenic.adapters.StockAdapter;
import com.aryan.edenic.models.ChatContact;
import com.aryan.edenic.models.ChatMessage;
import com.aryan.edenic.models.LeaderboardEntry;
import com.aryan.edenic.models.PortfolioItem;
import com.aryan.edenic.models.PortfolioManager;
import com.aryan.edenic.models.Stock;
import com.aryan.edenic.models.Transaction;
import com.aryan.edenic.utils.FirebaseDataService;
import com.aryan.edenic.utils.NotificationManager;
import com.aryan.edenic.utils.StockChangeClient;
import com.aryan.edenic.utils.StockLogoLoader;
import com.aryan.edenic.yahoo_finance.YahooFinanceClient;
import com.aryan.edenic.yahoo_finance.YahooResponse;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements StockAdapter.StockShareListener {
    private static final String TAG = "HomeActivity";
    private static final long PRICE_REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private static long lastPriceRefreshTime = 0;
    private FirebaseDataService firebaseDataService;

    // UI Components
    private RecyclerView stocksRecyclerView;
    private TextView portfolioValue, portfolioChange, userBalance;
    private View portfolioCard;
    private BottomNavigationView bottomNav;
    private ImageView userProfile;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView leaderboardRecycler;
    private TextView seeAllLabel;

    // Data
    private List<Stock> stocks = new ArrayList<>();
    private List<PortfolioItem> portfolioItems = new ArrayList<>();
    private StockAdapter adapter;
    private LeaderboardAdapter leaderboardAdapter;
    private PortfolioManager portfolioManager;

    // User portfolio state
    private double availableBalance = 10000;
    private double portfolioTotalValue = 0;

    // Constants
    private static final Map<String, Integer> STOCK_LOGOS = new HashMap<String, Integer>() {{
        put("AAPL", R.drawable.default_img_holder);
        put("MSFT", R.drawable.default_img_holder);
        put("GOOGL", R.drawable.default_img_holder);
        put("AMZN", R.drawable.default_img_holder);
        put("META", R.drawable.default_img_holder);
        put("TSLA", R.drawable.default_img_holder);
        put("NVDA", R.drawable.default_img_holder);
        put("JPM", R.drawable.default_img_holder);
        put("V", R.drawable.default_img_holder);
        put("WMT", R.drawable.default_img_holder);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views and data
        initializeViews();
        setupRecyclerViews();
        setupBottomNavigation();
        setupSwipeRefresh();
        setupListeners();

        // Initialize Firebase Data Service
        firebaseDataService = new FirebaseDataService();

        // Initialize portfolio manager
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        portfolioManager = new PortfolioManager(userId);

        // Load data
        loadUserData();
        setupStockData();
        setupLeaderboard();

    }

    private void setupListeners() {
        // Add profile click listener
        userProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        seeAllLabel.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, Discover.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_home);
        portfolioManager.startAutoUpdates();
        refreshData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        portfolioManager.stopAutoUpdates();
    }

    private void initializeViews() {
        portfolioValue = findViewById(R.id.portfolio_value);
        portfolioChange = findViewById(R.id.portfolio_change);
        portfolioCard = findViewById(R.id.portfolio_card);
        stocksRecyclerView = findViewById(R.id.stocks_recycler);
        bottomNav = findViewById(R.id.bottom_nav);
        userBalance = findViewById(R.id.user_balance);
        userProfile = findViewById(R.id.user_profile);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        leaderboardRecycler = findViewById(R.id.leaderboard_recycler);
        seeAllLabel = findViewById(R.id.see_all_label);

    }

    private void setupRecyclerViews() {
        // Set up stocks recycler view with sharing listener
        adapter = new StockAdapter(stocks, this::showTradeDialog, this);
        stocksRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        stocksRecyclerView.setAdapter(adapter);

        // Set up leaderboard recycler view
        leaderboardAdapter = new LeaderboardAdapter(this);
        leaderboardRecycler.setLayoutManager(new LinearLayoutManager(this));
        leaderboardRecycler.setAdapter(leaderboardAdapter);
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        //bottomNav.setItemBackgroundResource(R.drawable.nav_item_background);
        bottomNav.setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_discovery) {
                startActivity(new Intent(this, Discover.class));
                return true;
            } else if (item.getItemId() == R.id.nav_portfolio) {
                startActivity(new Intent(this, PortfolioActivity.class));
                return true;
            } else if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (item.getItemId() == R.id.nav_chat) {
                startActivity(new Intent(this, ChatListActivity.class));
                return true;
            }
            return true;
        });

    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.google_blue,
                R.color.profit_green,
                R.color.google_yellow,
                R.color.loss_red
        );
    }

    private void refreshData() {
        swipeRefreshLayout.setRefreshing(true);

        // Loading portfolio data should check isInitialLoad
        boolean isInitialLoad = portfolioItems.isEmpty();
        loadPortfolioFromDatabase(isInitialLoad);
    }

    private void loadUserData() {
        // Load user profile
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(userProfile);
        }

        // Load portfolio data
        loadPortfolioFromDatabase(true);
    }

    private void setupStockData() {
        // Initial stock data setup
        stocks.add(new Stock("AAPL", "Apple Inc.", 0, STOCK_LOGOS.get("AAPL")));
        stocks.add(new Stock("MSFT", "Microsoft Corp.", 0, STOCK_LOGOS.get("MSFT")));
        stocks.add(new Stock("GOOGL", "Alphabet Inc.", 0, STOCK_LOGOS.get("GOOGL")));
        stocks.add(new Stock("AMZN", "Amazon.com Inc.", 0, STOCK_LOGOS.get("AMZN")));
        stocks.add(new Stock("META", "Meta Platforms", 0, STOCK_LOGOS.get("META")));
        stocks.add(new Stock("TSLA", "Tesla Inc.", 0, STOCK_LOGOS.get("TSLA")));
        stocks.add(new Stock("NVDA", "NVIDIA Corp.", 0, STOCK_LOGOS.get("NVDA")));
        stocks.add(new Stock("JPM", "JPMorgan Chase", 0, STOCK_LOGOS.get("JPM")));
        stocks.add(new Stock("V", "Visa Inc.", 0, STOCK_LOGOS.get("V")));
        stocks.add(new Stock("WMT", "Walmart Inc.", 0, STOCK_LOGOS.get("WMT")));
        adapter.notifyDataSetChanged();

        // Fetch initial prices
        fetchInitialPrices();
    }

    private void setupLeaderboard() {
        // Listen for leaderboard updates
        setupLeaderboardListener();
    }

    private void fetchInitialPrices() {
        long currentTime = System.currentTimeMillis();

        // Only fetch prices if it's been more than PRICE_REFRESH_INTERVAL since last refresh
        if (currentTime - lastPriceRefreshTime > PRICE_REFRESH_INTERVAL) {
            // First fetch percentage changes from Google Sheets
            StockChangeClient.fetchChangePercentages(new StockChangeClient.StockChangeCallback() {
                @Override
                public void onSuccess(Map<String, Double> changePercentMap) {
                    // Store the percentage changes for each stock
                    for (Stock stock : stocks) {
                        Double changePercent = changePercentMap.get(stock.getSymbol());
                        if (changePercent != null) {
                            stock.setChangePercent(changePercent); // Assume you've added this field
                            Log.d(TAG, "Set change % for " + stock.getSymbol() + ": " + changePercent);
                        }
                    }

                    // Then continue with your normal price fetching
                    for (Stock stock : stocks) {
                        fetchStockPrice(stock);
                    }

                    // Also update portfolio item prices
                    for (PortfolioItem item : portfolioItems) {
                        // Find corresponding stock in your stocks list
                        for (Stock stock : stocks) {
                            if (stock.getSymbol().equals(item.getSymbol())) {
                                // First check if price is already loaded
                                if (stock.getPrice() > 0) {
                                    item.updatePrice(stock.getPrice());
                                } else {
                                    // If not, fetch price directly for this portfolio item
                                    fetchPriceForPortfolioItem(item);
                                }
                                break;
                            }
                        }
                    }
                    lastPriceRefreshTime = currentTime;
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to fetch change percentages from Google Sheets", e);

                    // Fall back to just fetching prices with Yahoo Finance
                    for (Stock stock : stocks) {
                        fetchStockPrice(stock);
                    }

                    // Rest of your existing code for portfolio items
                    for (PortfolioItem item : portfolioItems) {
                        for (Stock stock : stocks) {
                            if (stock.getSymbol().equals(item.getSymbol())) {
                                if (stock.getPrice() > 0) {
                                    item.updatePrice(stock.getPrice());
                                } else {
                                    fetchPriceForPortfolioItem(item);
                                }
                                break;
                            }
                        }
                    }
                    lastPriceRefreshTime = currentTime;
                }
            });
        } else {
            // Just update the UI with current data
            updatePortfolioValue();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    // Method to fetch price specifically for portfolio items
    private void fetchPriceForPortfolioItem(PortfolioItem item) {
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

                                runOnUiThread(() -> {
                                    // Update the portfolio item price
                                    item.updatePrice(price);
                                    Log.d(TAG, "Updated price for portfolio item " + item.getSymbol() + ": $" + price);

                                    // Recalculate portfolio value and update UI
                                    updatePortfolioValue();
                                    swipeRefreshLayout.setRefreshing(false);
                                });
                            } else {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        } else {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<YahooResponse> call, Throwable t) {
                        Log.e(TAG, "Error fetching price for " + item.getSymbol(), t);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void fetchStockPrice(Stock stock) {
        YahooFinanceClient.getInstance().getStockData(stock.getSymbol(), "1d", "1d")
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

                                runOnUiThread(() -> {
                                    stock.setPrice(price);
                                    // We don't need previous close or to calculate change percent here
                                    // as we're using the pre-fetched value from Google Sheets
                                    adapter.notifyDataSetChanged();
                                    updatePortfolioValue();
                                    swipeRefreshLayout.setRefreshing(false);
                                });
                            } else {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        } else {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onFailure(Call<YahooResponse> call, Throwable t) {
                        Log.e(TAG, "Error fetching price for " + stock.getSymbol(), t);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void showTradeDialog(Stock stock) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_trade_stock, null);

        // Initialize views
        ImageView stockLogo = dialogView.findViewById(R.id.dialog_stock_logo);
        TextView stockName = dialogView.findViewById(R.id.dialog_stock_name);
        TextView stockPrice = dialogView.findViewById(R.id.dialog_stock_price);
        TextView currentHoldings = dialogView.findViewById(R.id.dialog_current_holdings);
        RadioGroup tradeTypeGroup = dialogView.findViewById(R.id.trade_type_group);
        EditText quantityInput = dialogView.findViewById(R.id.quantity_input);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // Set stock data
        StockLogoLoader.loadStockLogo(this, stock.getSymbol(), stockLogo);
        stockName.setText(stock.getSymbol());
        stockPrice.setText(String.format("$%.2f", stock.getPrice()));

        // Check current holdings
        int ownedShares = getOwnedShares(stock.getSymbol());
        currentHoldings.setText(String.format("You own: %d shares", ownedShares));
        tradeTypeGroup.findViewById(R.id.radio_sell).setEnabled(ownedShares > 0);

        // Real-time validation
        quantityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                validateInput(quantityInput, btnConfirm, stock,
                        tradeTypeGroup.getCheckedRadioButtonId() == R.id.radio_buy,
                        ownedShares);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        tradeTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isBuy = checkedId == R.id.radio_buy;
            btnConfirm.setText(isBuy ? "Buy" : "Sell");
            btnConfirm.setBackgroundResource(isBuy ?
                    R.drawable.btn_gradient_buy : R.drawable.btn_gradient_sell);
            quantityInput.setHint(isBuy ? "Quantity to buy" : "Quantity to sell");
            validateInput(quantityInput, btnConfirm, stock, isBuy, ownedShares);
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            String quantityStr = quantityInput.getText().toString().trim();
            executeTrade(
                    stock,
                    quantityStr,
                    tradeTypeGroup.getCheckedRadioButtonId() == R.id.radio_buy,
                    dialog
            );
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void validateInput(EditText input, Button confirmBtn, Stock stock,
                               boolean isBuy, int ownedShares) {
        // Get the input text
        String inputText = input.getText().toString().trim();

        // Check if input is empty
        if (inputText.isEmpty()) {
            confirmBtn.setEnabled(false);
            input.setError("Please enter a quantity");
            return;
        }

        try {
            int quantity = Integer.parseInt(inputText);

            // Validate quantity is greater than zero
            if (quantity <= 0) {
                confirmBtn.setEnabled(false);
                input.setError("Quantity must be greater than 0");
                return;
            }

            if (isBuy) {
                double totalCost = quantity * stock.getPrice();
                boolean canAfford = totalCost <= availableBalance;
                confirmBtn.setEnabled(canAfford);
                input.setError(canAfford ? null : "Exceeds balance");
            } else {
                boolean canSell = quantity <= ownedShares;
                confirmBtn.setEnabled(canSell);
                input.setError(canSell ? null : "Not enough shares");
            }
        } catch (NumberFormatException e) {
            confirmBtn.setEnabled(false);
            input.setError("Invalid number format");
        }
    }

    private void executeTrade(Stock stock, String quantityStr, boolean isBuy, AlertDialog dialog) {
        // Validate input again as a safety check
        if (quantityStr == null || quantityStr.isEmpty()) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);

            // Check for valid quantity
            if (quantity <= 0) {
                Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = quantity * stock.getPrice();

            if (isBuy) {
                executeBuyOrder(amount, stock, quantity);
            } else {
                executeSellOrder(amount, stock, quantity);
            }

            dialog.dismiss();
            Toast.makeText(this,
                    String.format("%s %d shares of %s",
                            isBuy ? "Bought" : "Sold",
                            quantity,
                            stock.getSymbol()),
                    Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity format", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeBuyOrder(double amount, Stock stock, int quantity) {
        if (amount <= availableBalance) {
            availableBalance -= amount;
            addToPortfolio(stock, quantity, amount);
            updateUI();

            // Record the transaction
            recordTransaction(stock, Transaction.Type.BUY, quantity, stock.getPrice());

            // Save to Firebase
            savePortfolioToDatabase();

            showTransactionNotification(
                    "Purchase Complete",
                    "You bought " + quantity + " shares of " + stock.getSymbol() + " at $" + stock.getPrice()
            );

            //Notification for the Stock
            NotificationManager.subscribeToStockUpdates(stock.getSymbol());
        }
    }

    private void executeSellOrder(double amount, Stock stock, int quantity) {
        // Get the avg purchase price for profit/loss calculation
        double avgPurchasePrice = 0;
        for (PortfolioItem item : portfolioItems) {
            if (item.getSymbol().equals(stock.getSymbol())) {
                avgPurchasePrice = item.getAvgPurchasePrice();
                break;
            }
        }

        availableBalance += amount;
        removeFromPortfolio(stock, quantity);
        updateUI();

        // Record the transaction with profit/loss info
        recordTransaction(stock, Transaction.Type.SELL, quantity, stock.getPrice(), avgPurchasePrice);

        showTransactionNotification(
                "Sale Complete",
                "You sold " + quantity + " shares of " + stock.getSymbol() + " at $" + stock.getPrice()
        );

        NotificationManager.unsubscribeFromStockUpdates(stock.getSymbol());

        // Save to Firebase
        savePortfolioToDatabase();
    }

    // Method to show the transaction notification
    private void showTransactionNotification(String title, String message) {
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "transactions")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Show notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(generateNotificationId(), builder.build());
            }
        } else {
            notificationManager.notify(generateNotificationId(), builder.build());
        }
    }

    // Generate unique IDs for notifications
    private int generateNotificationId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    private void recordTransaction(Stock stock, Transaction.Type type, int quantity, double price) {
        recordTransaction(stock, type, quantity, price, 0);
    }

    private void recordTransaction(Stock stock, Transaction.Type type, int quantity, double price, double avgPurchasePrice) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        // Create transaction object
        Transaction transaction;
        if (type == Transaction.Type.BUY) {
            transaction = new Transaction(stock.getSymbol(), stock.getName(), quantity, price);
        } else {
            transaction = new Transaction(stock.getSymbol(), stock.getName(), quantity, price, avgPurchasePrice);
        }

        // Save to Firebase
        FirebaseDatabase.getInstance().getReference("transactions")
                .child(userId)
                .child(transaction.getId())
                .setValue(transaction.toMap())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, type + " transaction recorded for " + stock.getSymbol()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to record transaction", e));
    }

    private void addToPortfolio(Stock stock, int quantity, double investedAmount) {
        for (PortfolioItem item : portfolioItems) {
            if (item.getSymbol().equals(stock.getSymbol())) {
                item.addShares(quantity, investedAmount);
                updatePortfolioValue();
                return;
            }
        }

        portfolioItems.add(new PortfolioItem(
                stock.getSymbol(),
                stock.getName(),
                stock.getPrice(),
                quantity,
                investedAmount
        ));
        updatePortfolioValue();
    }

    private void removeFromPortfolio(Stock stock, int quantity) {
        Log.d(TAG, "Attempting to remove " + quantity + " shares of " + stock.getSymbol());

        PortfolioItem itemToRemove = null;
        for (PortfolioItem item : portfolioItems) {
            if (item.getSymbol().equals(stock.getSymbol())) {
                item.removeShares(quantity);
                Log.d(TAG, "After removal, remaining shares: " + item.getQuantity());

                if (item.getQuantity() <= 0) {
                    itemToRemove = item; // Mark for removal outside loop
                }
                break;
            }

            if (getOwnedShares(stock.getSymbol()) <= 0) {
                NotificationManager.unsubscribeFromStockUpdates(stock.getSymbol());
            }
        }

        // Remove outside the loop to avoid ConcurrentModificationException
        if (itemToRemove != null) {
            // Store symbol in a final variable for use in lambda
            final String symbolToRemove = itemToRemove.getSymbol();

            Log.d(TAG, "Completely removing stock: " + symbolToRemove);
            portfolioItems.remove(itemToRemove);

            // IMPORTANT: Directly remove from the database as well
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

                // Remove from both data structures
                userRef.child("stocks").child(symbolToRemove).removeValue()
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "Successfully removed " + symbolToRemove + " from stocks"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to remove " + symbolToRemove + " from stocks: " + e.getMessage()));

                userRef.child("portfolioItems").child(symbolToRemove).removeValue()
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "Successfully removed " + symbolToRemove + " from portfolioItems"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to remove " + symbolToRemove + " from portfolioItems: " + e.getMessage()));
            }
        }

        // Debug the portfolio after removal
        Log.d(TAG, "Portfolio after removal:");
        for (PortfolioItem item : portfolioItems) {
            Log.d(TAG, item.getSymbol() + ": " + item.getQuantity() + " shares");
        }

        updatePortfolioValue();
    }

    private int getOwnedShares(String symbol) {
        for (PortfolioItem item : portfolioItems) {
            if (item.getSymbol().equals(symbol)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    private void updateUI() {
        updateBalanceUI();
        updatePortfolioValue();
    }

    private void updateBalanceUI() {
        userBalance.setText(String.format("$%,.2f", availableBalance));
    }

    private void updatePortfolioValue() {
        // Update current prices in portfolio items
        for (PortfolioItem item : portfolioItems) {
            for (Stock stock : stocks) {
                if (item.getSymbol().equals(stock.getSymbol())) {
                    item.updatePrice(stock.getPrice());
                    break;
                }
            }
        }

        portfolioTotalValue = portfolioItems.stream()
                .mapToDouble(PortfolioItem::getCurrentValue)
                .sum();

        double totalInvested = portfolioItems.stream()
                .mapToDouble(PortfolioItem::getInvestedAmount)
                .sum();

        double profitPercent = totalInvested > 0 ?
                ((portfolioTotalValue - totalInvested) / totalInvested) * 100 : 0;

        updatePortfolioCard(portfolioTotalValue, profitPercent);

        // To make sure to update Firebase with the calculated value
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && portfolioTotalValue > 0) {
            // Update portfolioValue in user node
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .child("portfolioValue")
                    .setValue(portfolioTotalValue);

            // Also update the leaderboard value directly
            FirebaseDatabase.getInstance().getReference("leaderboard")
                    .child(user.getUid())
                    .child("portfolioValue")
                    .setValue(portfolioTotalValue);

            // Update daily change percentage
            FirebaseDatabase.getInstance().getReference("leaderboard")
                    .child(user.getUid())
                    .child("dailyChangePercent")
                    .setValue(profitPercent);
        }

        // Save to Firebase
        savePortfolioToDatabase();
    }

    private void updatePortfolioCard(double value, double percentChange) {
        portfolioValue.setText(String.format("$%,.2f", value));
        portfolioChange.setText(String.format("%.2f%%", percentChange));

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                percentChange >= 0 ?
                        new int[]{Color.parseColor("#FF4CAF50"), Color.parseColor("#FF8BC34A")} :
                        new int[]{Color.parseColor("#FFF44336"), Color.parseColor("#FFFF9800")}
        );
        gradient.setCornerRadius(32f);
        portfolioCard.setBackground(gradient);
    }

    private void loadPortfolioFromDatabase(boolean isInitialLoad) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            Log.d(TAG, "Loading portfolio data for user: " + userId);

            // REMOVE THIS CONDITION - It's causing the problem
            // Instead, always load the values from Firebase
        /*
        if (!isInitialLoad && !portfolioItems.isEmpty()) {
            Log.d(TAG, "Skipping reload after trade to preserve local state");
            updateUI();
            fetchInitialPrices();
            return;
        }
        */

            // Reference to the user's data in Realtime Database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            // Also check portfolioValue directly
            userRef.child("portfolioValue").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        double storedValue = snapshot.getValue(Double.class);
                        if (storedValue > 0) {
                            // If we have a valid stored value, use it
                            portfolioTotalValue = storedValue;
                            updatePortfolioCard(portfolioTotalValue, 0); // Will update percent later
                        }
                    }

                    // Continue loading the full portfolio data
                    loadFullPortfolioData(userRef);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadFullPortfolioData(userRef);
                }
            });
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    // New helper method to load the full portfolio data
    private void loadFullPortfolioData(DatabaseReference userRef) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    portfolioItems.clear();

                    // Process stocks
                    DataSnapshot stocksSnapshot = snapshot.child("stocks");
                    if (stocksSnapshot.exists()) {
                        Log.d(TAG, "Found " + stocksSnapshot.getChildrenCount() + " stocks in database");
                        double totalInvested = 0;

                        for (DataSnapshot stockSnapshot : stocksSnapshot.getChildren()) {
                            try {
                                String symbol = stockSnapshot.getKey();
                                double avgPrice = stockSnapshot.child("avgPrice").getValue(Double.class);
                                int quantity = stockSnapshot.child("qty").getValue(Integer.class);

                                Log.d(TAG, String.format("Loading stock: %s, %d shares @ $%.2f",
                                        symbol, quantity, avgPrice));

                                double investedAmount = avgPrice * quantity;
                                totalInvested += investedAmount;

                                // Create portfolio item
                                PortfolioItem item = new PortfolioItem(
                                        symbol,
                                        getStockName(symbol),
                                        avgPrice,  // Current price initially same as avg price
                                        quantity,
                                        investedAmount
                                );

                                portfolioItems.add(item);
                            } catch (Exception e) {
                                Log.e("Firebase", "Error parsing stock data: " + e.getMessage());
                            }
                        }

                        // Update available balance
                        availableBalance = 10000 - totalInvested;
                    }
                }

                // Update UI
                updateUI();

                // Fetch latest prices
                fetchInitialPrices();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error loading portfolio: " + error.getMessage());
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    // Helper method to get stock name
    private String getStockName(String symbol) {
        for (Stock stock : stocks) {
            if (stock.getSymbol().equals(symbol)) {
                return stock.getName();
            }
        }
        return symbol + " Inc.";
    }

    private void savePortfolioToDatabase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            Log.d(TAG, "Saving portfolio for user: " + userId);

            // Calculate total portfolio value - ensure it's accurate based on current prices
            final double calculatedPortfolioValue = portfolioItems.stream()
                    .mapToDouble(PortfolioItem::getCurrentValue)
                    .sum();

            for (PortfolioItem item : portfolioItems) {
                Log.d(TAG, String.format("Portfolio item: %s, %d shares, value: $%.2f",
                        item.getSymbol(), item.getQuantity(), item.getCurrentValue()));
            }

            Log.d(TAG, "Total portfolio value: $" + calculatedPortfolioValue);

            // Reference to user data
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            // Update portfolio value
            userRef.child("portfolioValue").setValue(calculatedPortfolioValue);
            userRef.child("lastUpdated").setValue(ServerValue.TIMESTAMP);

            // Calculate daily change percentage
            final double totalInvested = portfolioItems.stream()
                    .mapToDouble(PortfolioItem::getInvestedAmount)
                    .sum();
            final double changePercent = totalInvested > 0 ?
                    ((calculatedPortfolioValue - totalInvested) / totalInvested) * 100 : 0;
            userRef.child("dailyChangePercent").setValue(changePercent);

            // Update each stock individually
            for (PortfolioItem item : portfolioItems) {
                if (item.getQuantity() > 0) {
                    // Make sure we're using the CURRENT price, not just avg price
                    double currentPrice = item.getCurrentPrice();
                    if (currentPrice <= 0) {
                        // If price isn't loaded yet, use avg price as fallback
                        currentPrice = item.getAvgPurchasePrice();
                    }

                    // Update legacy format
                    Map<String, Object> legacyData = new HashMap<>();
                    legacyData.put("avgPrice", item.getAvgPurchasePrice());
                    legacyData.put("qty", item.getQuantity());
                    userRef.child("stocks").child(item.getSymbol()).setValue(legacyData);
                    Log.d(TAG, String.format("Saving legacy format: %s, %d shares @ $%.2f",
                            item.getSymbol(), item.getQuantity(), item.getAvgPurchasePrice()));

                    // Update new format
                    Map<String, Object> newData = new HashMap<>();
                    newData.put("symbol", item.getSymbol());
                    newData.put("companyName", item.getCompanyName());
                    newData.put("currentPrice", currentPrice);  // Make sure we use current price
                    newData.put("quantity", item.getQuantity());
                    newData.put("investedAmount", item.getInvestedAmount());
                    newData.put("avgPurchasePrice", item.getAvgPurchasePrice());
                    userRef.child("portfolioItems").child(item.getSymbol()).setValue(newData);
                    Log.d(TAG, String.format("Saving new format: %s, %d shares @ $%.2f, current: $%.2f",
                            item.getSymbol(), item.getQuantity(), item.getAvgPurchasePrice(), currentPrice));
                }
            }

            // Update leaderboard entry
            updateLeaderboardEntry(userId, calculatedPortfolioValue, changePercent);

            Log.d(TAG, "Portfolio data saved");
        }
    }

    private void updateLeaderboardEntry(String userId, double portfolioValue, double changePercent) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Create leaderboard entry
        Map<String, Object> entryData = new HashMap<>();
        entryData.put("name", user.getDisplayName());
        entryData.put("displayName", user.getDisplayName());
        entryData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        entryData.put("portfolioValue", portfolioValue);
        entryData.put("dailyChangePercent", changePercent);
        entryData.put("position", -1); // Position will be calculated when displaying the leaderboard

        // Update in Realtime Database
        FirebaseDatabase.getInstance().getReference("leaderboard")
                .child(userId)
                .setValue(entryData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Leaderboard entry updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating leaderboard", e));
    }

    private void setupLeaderboardListener() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("leaderboard")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<LeaderboardEntry> allEntries = new ArrayList<>();

                        // 1. Collect all entries and find current user
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            try {
                                String userId = userSnapshot.getKey();
                                String name = userSnapshot.child("name").getValue(String.class);
                                String photoUrl = userSnapshot.child("photoUrl").getValue(String.class);
                                Double portfolioValue = userSnapshot.child("portfolioValue").getValue(Double.class);
                                Double dailyChangePercent = userSnapshot.child("dailyChangePercent").getValue(Double.class);

                                if (name != null && portfolioValue != null) {
                                    LeaderboardEntry entry = new LeaderboardEntry(
                                            name,
                                            photoUrl,
                                            portfolioValue,
                                            dailyChangePercent != null ? dailyChangePercent : 0.0
                                    );
                                    entry.setUserId(userId);

                                    allEntries.add(entry);
                                    Log.d(TAG, "Added leaderboard entry: " + name + " - $" + portfolioValue);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing leaderboard entry", e);
                            }
                        }

                        // 2. Sort by portfolio value (descending)
                        Collections.sort(allEntries, (o1, o2) ->
                                Double.compare(o2.getPortfolioValue(), o1.getPortfolioValue()));

                        // Update adapter
                        leaderboardAdapter.setEntries(allEntries, currentUserId);
                        Log.d(TAG, "Updated leaderboard adapter with " + allEntries.size() + " entries");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading leaderboard", error.toException());
                    }
                });
    }

    // StockShareListener implementation
    @Override
    public void onStockShareRequested(Stock stock) {
        showShareOptions(stock);
    }

    private void showShareOptions(Stock stock) {
        // Check if user has contacts to share with
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("contacts")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            // No contacts found
                            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                            builder.setTitle("No Contacts")
                                    .setMessage("You don't have any contacts to share with. Would you like to find traders to connect with?")
                                    .setPositiveButton("Find Traders", (dialog, which) -> {
                                        startActivity(new Intent(HomeActivity.this, ChatListActivity.class));
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            return;
                        }

                        // Build list of contacts
                        List<ChatContact> contacts = new ArrayList<>();
                        for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                            try {
                                ChatContact contact = contactSnapshot.getValue(ChatContact.class);
                                if (contact != null && contact.isConnected()) {
                                    contacts.add(contact);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing contact", e);
                            }
                        }

                        if (contacts.isEmpty()) {
                            Toast.makeText(HomeActivity.this, "No connected contacts to share with", Toast.LENGTH_SHORT).show();
                        } else {
                            showContactSelectionDialog(contacts, stock);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HomeActivity.this, "Error loading contacts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showContactSelectionDialog(List<ChatContact> contacts, Stock stock) {
        // Create dialog with contacts list
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share with");

        // Create list of contact names
        String[] contactNames = new String[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            contactNames[i] = contacts.get(i).getDisplayName();
        }

        builder.setItems(contactNames, (dialog, which) -> {
            // Share stock with selected contact
            ChatContact selectedContact = contacts.get(which);
            shareStockWithContact(selectedContact, stock);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void shareStockWithContact(ChatContact contact, Stock stock) {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();

        // Generate chat ID (alphabetical order of user IDs)
        String chatId = currentUserId.compareTo(contact.getUserId()) < 0 ?
                currentUserId + "_" + contact.getUserId() : contact.getUserId() + "_" + currentUserId;

        // Create stock share message
        ChatMessage message = new ChatMessage(
                currentUserId,
                currentUser.getDisplayName(),
                stock.getSymbol(),
                stock.getPrice()
        );

        // Save to Firebase
        FirebaseDatabase.getInstance().getReference("messages")
                .child(chatId)
                .child(message.getMessageId())
                .setValue(message.toMap())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Stock shared with " + contact.getDisplayName(), Toast.LENGTH_SHORT).show();

                    // Update last message and time for both users
                    updateLastMessage(currentUserId, contact.getUserId(), message);
                    updateLastMessage(contact.getUserId(), currentUserId, message);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to share stock", Toast.LENGTH_SHORT).show());
    }

    private void updateLastMessage(String userId, String contactId, ChatMessage message) {
        DatabaseReference contactRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(userId)
                .child("contacts")
                .child(contactId);

        contactRef.child("lastMessage").setValue(message.getContent());
        contactRef.child("lastMessageTime").setValue(message.getTimestamp());

        // Increment unread count for recipient
        if (!userId.equals(message.getSenderId())) {
            contactRef.child("unreadCount").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer currentCount = snapshot.getValue(Integer.class);
                    int newCount = (currentCount != null ? currentCount : 0) + 1;
                    contactRef.child("unreadCount").setValue(newCount);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
}