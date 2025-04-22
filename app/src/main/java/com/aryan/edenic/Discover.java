package com.aryan.edenic;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.adapters.StockAdapter;
import com.aryan.edenic.adapters.StockGridAdapter;
import com.aryan.edenic.models.ChatContact;
import com.aryan.edenic.models.ChatMessage;
import com.aryan.edenic.models.PortfolioItem;
import com.aryan.edenic.models.Stock;
import com.aryan.edenic.models.Transaction;
import com.aryan.edenic.utils.NotificationManager;
import com.aryan.edenic.utils.StockChangeClient;
import com.aryan.edenic.utils.StockLogoLoader;
import com.aryan.edenic.yahoo_finance.YahooFinanceClient;
import com.aryan.edenic.yahoo_finance.YahooResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Discover extends AppCompatActivity implements StockAdapter.StockShareListener {
    private static final String TAG = "Discover";

    // UI components
    private RecyclerView stocksGrid;
    private CardView searchContainer;
    private EditText searchInput;
    private ImageButton searchButton;
    private LinearLayout emptyView;
    private ProgressBar loadingIndicator;
    private BottomNavigationView bottomNav;

    // Data
    private List<Stock> allStocks = new ArrayList<>();
    private StockGridAdapter adapter;
    private boolean isSearchVisible = false;

    // User portfolio data
    private List<PortfolioItem> portfolioItems = new ArrayList<>();
    private double availableBalance = 10000;

    // Firestore
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        // Initialize UI
        initializeViews();
        setupListeners();
        setupStockGrid();
        setupBottomNavigation();

        // Load data
        loadUserPortfolio();
        loadAllStocks();
    }

    private void initializeViews() {
        stocksGrid = findViewById(R.id.stocks_grid);
        searchContainer = findViewById(R.id.search_container);
        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);
        emptyView = findViewById(R.id.empty_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void setupListeners() {
        // Search button click
        searchButton.setOnClickListener(v -> toggleSearchBar());

        // Search input
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterStocks(searchInput.getText().toString());
                return true;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterStocks(s.toString());
            }
        });
    }

    private void setupStockGrid() {
        // Use a grid layout with 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (position % 7 == 6) ? 2 : 1;
            }
        });

        stocksGrid.setLayoutManager(layoutManager);

        // Create and set adapter with share listener
        adapter = new StockGridAdapter(this, allStocks, this::showTradeDialog, this);
        stocksGrid.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_discovery);
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_portfolio) {
                startActivity(new Intent(this, PortfolioActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_chat) {
                startActivity(new Intent(this, ChatListActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    // In Discover.java
    private void loadAllStocks() {
        Log.d(TAG, "Starting to load all stocks");

        // Show loading indicator
        loadingIndicator.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // Popular stock symbols - expanded list
        String[] symbols = {
                "AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "NVDA",
                "JPM", "V", "WMT", "PG", "JNJ", "UNH", "HD", "BAC", "PFE",
                "CSCO", "VZ", "INTC", "NFLX", "DIS", "ADBE", "PYPL", "CRM",
                "CMCSA", "PEP", "COST", "ABT", "TMO", "MCD", "ACN", "NKE",
                "AVGO", "TXN", "QCOM", "LLY", "DHR", "NEE", "T", "CVX"
        };

        Log.d(TAG, "Adding " + symbols.length + " stocks to list");

        // Create stock objects
        for (String symbol : symbols) {
            allStocks.add(new Stock(symbol, getFullName(symbol), 0, 0));
        }

        // Update the adapter
        adapter.updateStocks(allStocks);
        Log.d(TAG, "Adapter updated with " + allStocks.size() + " stocks");

        // Fetch prices for all stocks
        fetchPricesForAllStocks();
    }

    private void fetchPricesForAllStocks() {
        Log.d(TAG, "Fetching prices for " + allStocks.size() + " stocks");

        // First fetch change percentages from Google Sheets
        StockChangeClient.fetchChangePercentages(new StockChangeClient.StockChangeCallback() {
            @Override
            public void onSuccess(Map<String, Double> changePercentMap) {
                Log.d(TAG, "Successfully fetched change percentages from Google Sheets");

                // Update stock objects with percentage changes
                for (Stock stock : allStocks) {
                    Double changePercent = changePercentMap.get(stock.getSymbol());
                    if (changePercent != null) {
                        stock.setChangePercent(changePercent);
                        Log.d(TAG, "Set change % for " + stock.getSymbol() + ": " + changePercent);
                    }
                }

                // Now fetch current prices from Yahoo Finance
                fetchCurrentPricesFromYahoo();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch change percentages from Google Sheets", e);
                // Fall back to Yahoo Finance for both price and change calculation
                fetchCurrentPricesFromYahoo();
            }
        });
    }

    // New method to handle Yahoo Finance API calls for current prices
    private void fetchCurrentPricesFromYahoo() {
        final int[] completedRequests = {0};
        final int totalRequests = allStocks.size();

        for (Stock stock : allStocks) {
            Log.d(TAG, "Fetching price for " + stock.getSymbol());

            // Only need 1d for current price since we're getting change % from Sheets
            YahooFinanceClient.getInstance().getStockData(stock.getSymbol(), "1d", "1d")
                    .enqueue(new Callback<YahooResponse>() {
                        @Override
                        public void onResponse(Call<YahooResponse> call, Response<YahooResponse> response) {
                            Log.d(TAG, "Received response for " + stock.getSymbol() + ", success: " + response.isSuccessful());

                            if (response.isSuccessful() && response.body() != null) {
                                YahooResponse yahooResponse = response.body();

                                if (yahooResponse.chart != null &&
                                        yahooResponse.chart.result != null &&
                                        !yahooResponse.chart.result.isEmpty() &&
                                        yahooResponse.chart.result.get(0) != null &&
                                        yahooResponse.chart.result.get(0).meta != null) {

                                    double price = yahooResponse.chart.result.get(0).meta.regularMarketPrice;
                                    // We still get prevClose but don't rely on it for change %
                                    double prevClose = yahooResponse.chart.result.get(0).meta.previousClose;

                                    Log.d(TAG, stock.getSymbol() + " price: $" + price);

                                    runOnUiThread(() -> {
                                        // Update stock price and previousClose as backup
                                        stock.setPrice(price);
                                        if (prevClose > 0) {
                                            stock.setPreviousClose(prevClose);
                                        }
                                        adapter.notifyDataSetChanged();

                                        // Hide loading after all requests complete
                                        checkRequestCompletion();
                                    });
                                } else {
                                    Log.e(TAG, "Invalid response structure for " + stock.getSymbol());
                                    checkRequestCompletion();
                                }
                            } else {
                                Log.e(TAG, "Error response for " + stock.getSymbol() + ": " +
                                        (response.errorBody() != null ? response.errorBody().toString() : "unknown error"));
                                checkRequestCompletion();
                            }
                        }

                        @Override
                        public void onFailure(Call<YahooResponse> call, Throwable t) {
                            Log.e(TAG, "Request failed for " + stock.getSymbol() + ": " + t.getMessage(), t);
                            checkRequestCompletion();
                        }

                        private void checkRequestCompletion() {
                            // Use the array to track completions
                            completedRequests[0]++;

                            Log.d(TAG, "Completed " + completedRequests[0] + " of " + totalRequests + " requests");

                            // Hide loading indicator when all requests complete
                            if (completedRequests[0] >= totalRequests) {
                                runOnUiThread(() -> {
                                    loadingIndicator.setVisibility(View.GONE);
                                    if (adapter.getItemCount() == 0) {
                                        emptyView.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    });
        }
    }

    private void loadUserPortfolio() {
        if (userId == null) return;

        FirebaseDatabase.getInstance().getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        portfolioItems.clear();

                        if (snapshot.exists() && snapshot.child("stocks").exists()) {
                            double totalInvested = 0;

                            for (DataSnapshot stockSnapshot : snapshot.child("stocks").getChildren()) {
                                try {
                                    String symbol = stockSnapshot.getKey();
                                    double avgPrice = stockSnapshot.child("avgPrice").getValue(Double.class);
                                    int quantity = stockSnapshot.child("qty").getValue(Integer.class);

                                    double investedAmount = avgPrice * quantity;
                                    totalInvested += investedAmount;

                                    // Create portfolio item
                                    PortfolioItem item = new PortfolioItem(
                                            symbol,
                                            getFullName(symbol),
                                            avgPrice,
                                            quantity,
                                            investedAmount
                                    );

                                    portfolioItems.add(item);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing stock", e);
                                }
                            }

                            // Update available balance
                            availableBalance = 10000 - totalInvested;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading portfolio", error.toException());
                    }
                });
    }

    private void toggleSearchBar() {
        isSearchVisible = !isSearchVisible;
        searchContainer.setVisibility(isSearchVisible ? View.VISIBLE : View.GONE);

        if (isSearchVisible) {
            searchInput.requestFocus();
            // Show keyboard (optional)
            // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            // imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        } else {
            // Clear search and show all stocks
            searchInput.setText("");
            filterStocks("");
        }
    }

    private void filterStocks(String query) {
        adapter.filter(query);

        // Show/hide empty view based on results
        if (adapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
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
                NotificationManager.subscribeToStockUpdates(stock.getSymbol());
            } else {
                executeSellOrder(amount, stock, quantity);
                NotificationManager.unsubscribeFromStockUpdates(stock.getSymbol());
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

            // Record transaction
            recordTransaction(stock, Transaction.Type.BUY, quantity, stock.getPrice());

            // Save to Firebase
            savePortfolioToFirebase();
        }
    }

    private void executeSellOrder(double amount, Stock stock, int quantity) {
        // Get avg purchase price for profit/loss calculation
        double avgPurchasePrice = 0;
        for (PortfolioItem item : portfolioItems) {
            if (item.getSymbol().equals(stock.getSymbol())) {
                avgPurchasePrice = item.getAvgPurchasePrice();
                break;
            }
        }

        availableBalance += amount;
        removeFromPortfolio(stock, quantity);

        // Record transaction with profit/loss
        recordTransaction(stock, Transaction.Type.SELL, quantity, stock.getPrice(), avgPurchasePrice);

        // Save to Firebase
        savePortfolioToFirebase();
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
    }

    private void removeFromPortfolio(Stock stock, int quantity) {
        String symbolToRemove = null;

        for (PortfolioItem item : portfolioItems) {
            if (item.getSymbol().equals(stock.getSymbol())) {
                item.removeShares(quantity);

                if (item.getQuantity() <= 0) {
                    symbolToRemove = item.getSymbol();
                }
                break;
            }
        }

        // Remove item with zero quantity
        if (symbolToRemove != null) {
            for (int i = 0; i < portfolioItems.size(); i++) {
                if (portfolioItems.get(i).getSymbol().equals(symbolToRemove)) {
                    portfolioItems.remove(i);
                    break;
                }
            }

            // Also remove from Firebase
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();

                // Remove from stocks node
                FirebaseDatabase.getInstance().getReference("users")
                        .child(userId)
                        .child("stocks")
                        .child(symbolToRemove)
                        .removeValue();

                // Remove from portfolioItems node
                FirebaseDatabase.getInstance().getReference("users")
                        .child(userId)
                        .child("portfolioItems")
                        .child(symbolToRemove)
                        .removeValue();
            }
        }
    }

    private int getOwnedShares(String symbol) {
        for (PortfolioItem item : portfolioItems) {
            if (item.getSymbol().equals(symbol)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    private void savePortfolioToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            // Calculate total portfolio value
            double portfolioValue = 0;
            for (PortfolioItem item : portfolioItems) {
                portfolioValue += item.getCurrentValue();
            }

            // Update portfolio value
            userRef.child("portfolioValue").setValue(portfolioValue);
            userRef.child("lastUpdated").setValue(ServerValue.TIMESTAMP);

            // Calculate daily change percentage
            double totalInvested = 0;
            for (PortfolioItem item : portfolioItems) {
                totalInvested += item.getInvestedAmount();
            }

            double changePercent = totalInvested > 0 ?
                    ((portfolioValue - totalInvested) / totalInvested) * 100 : 0;

            userRef.child("dailyChangePercent").setValue(changePercent);

            // Update each stock
            for (PortfolioItem item : portfolioItems) {
                // Update legacy format
                Map<String, Object> stockData = new HashMap<>();
                stockData.put("avgPrice", item.getAvgPurchasePrice());
                stockData.put("qty", item.getQuantity());

                userRef.child("stocks").child(item.getSymbol()).setValue(stockData);

                // Update new format
                Map<String, Object> portfolioItemData = new HashMap<>();
                portfolioItemData.put("symbol", item.getSymbol());
                portfolioItemData.put("companyName", item.getCompanyName());
                portfolioItemData.put("currentPrice", item.getCurrentPrice());
                portfolioItemData.put("quantity", item.getQuantity());
                portfolioItemData.put("investedAmount", item.getInvestedAmount());
                portfolioItemData.put("avgPurchasePrice", item.getAvgPurchasePrice());

                userRef.child("portfolioItems").child(item.getSymbol()).setValue(portfolioItemData);
            }

            // Update leaderboard entry
            DatabaseReference leaderboardRef = FirebaseDatabase.getInstance().getReference("leaderboard").child(userId);
            Map<String, Object> leaderboardData = new HashMap<>();
            leaderboardData.put("name", user.getDisplayName());
            leaderboardData.put("displayName", user.getDisplayName());
            leaderboardData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
            leaderboardData.put("portfolioValue", portfolioValue);
            leaderboardData.put("dailyChangePercent", changePercent);
            leaderboardData.put("position", -1);

            leaderboardRef.setValue(leaderboardData);
        }
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
                            AlertDialog.Builder builder = new AlertDialog.Builder(Discover.this);
                            builder.setTitle("No Contacts")
                                    .setMessage("You don't have any contacts to share with. Would you like to find traders to connect with?")
                                    .setPositiveButton("Find Traders", (dialog, which) -> {
                                        startActivity(new Intent(Discover.this, ChatListActivity.class));
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
                            Toast.makeText(Discover.this, "No connected contacts to share with", Toast.LENGTH_SHORT).show();
                        } else {
                            showContactSelectionDialog(contacts, stock);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Discover.this, "Error loading contacts", Toast.LENGTH_SHORT).show();
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
                    // Handle error
                }
            });
        }
    }

    private String getFullName(String symbol) {
        // Map stock symbols to company names
        Map<String, String> companyNames = new HashMap<>();
        companyNames.put("AAPL", "Apple Inc.");
        companyNames.put("MSFT", "Microsoft Corporation");
        companyNames.put("GOOGL", "Alphabet Inc.");
        companyNames.put("AMZN", "Amazon.com Inc.");
        companyNames.put("META", "Meta Platforms Inc.");
        companyNames.put("TSLA", "Tesla Inc.");
        companyNames.put("NVDA", "NVIDIA Corporation");
        companyNames.put("JPM", "JPMorgan Chase & Co.");
        companyNames.put("V", "Visa Inc.");
        companyNames.put("WMT", "Walmart Inc.");
        companyNames.put("PG", "Procter & Gamble Co.");
        companyNames.put("JNJ", "Johnson & Johnson");
        companyNames.put("UNH", "UnitedHealth Group Inc.");
        companyNames.put("HD", "Home Depot Inc.");
        companyNames.put("BAC", "Bank of America Corp.");
        companyNames.put("PFE", "Pfizer Inc.");
        companyNames.put("CSCO", "Cisco Systems Inc.");
        companyNames.put("VZ", "Verizon Communications Inc.");
        companyNames.put("INTC", "Intel Corporation");
        companyNames.put("NFLX", "Netflix Inc.");
        companyNames.put("DIS", "The Walt Disney Company");
        companyNames.put("ADBE", "Adobe Inc.");
        companyNames.put("PYPL", "PayPal Holdings Inc.");
        companyNames.put("CRM", "Salesforce Inc.");
        companyNames.put("CMCSA", "Comcast Corporation");
        companyNames.put("PEP", "PepsiCo Inc.");
        companyNames.put("COST", "Costco Wholesale Corporation");
        companyNames.put("ABT", "Abbott Laboratories");
        companyNames.put("TMO", "Thermo Fisher Scientific Inc.");
        companyNames.put("MCD", "McDonald's Corporation");
        companyNames.put("ACN", "Accenture plc");
        companyNames.put("NKE", "Nike Inc.");
        companyNames.put("AVGO", "Broadcom Inc.");
        companyNames.put("TXN", "Texas Instruments Incorporated");
        companyNames.put("QCOM", "Qualcomm Incorporated");
        companyNames.put("LLY", "Eli Lilly and Company");
        companyNames.put("DHR", "Danaher Corporation");
        companyNames.put("NEE", "NextEra Energy Inc.");
        companyNames.put("T", "AT&T Inc.");
        companyNames.put("CVX", "Chevron Corporation");

        return companyNames.getOrDefault(symbol, symbol + " Inc.");
    }
}