package com.aryan.edenic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.adapters.TransactionAdapter;
import com.aryan.edenic.models.Transaction;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";

    // UI components
    private TextView userBalance;
    private ImageView userProfile;
    private TabLayout filterTabs;
    private TextView transactionCount;
    private TextView totalProfitLoss;
    private RecyclerView historyList;
    private TextView emptyHistory;
    private BottomNavigationView bottomNav;

    // Data
    private final List<Transaction> transactions = new ArrayList<>();
    private TransactionAdapter adapter;
    private String userId;
    private double availableBalance = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Get user ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            // Not logged in
            startActivity(new Intent(this, Auth_Activity.class));
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();
        setupBottomNavigation();
        setupFilterTabs();
        setupRecyclerView();
        setupListeners();

        // Load user data and transaction history
        loadUserProfile();
        loadTransactionHistory();

    }

    private void setupListeners() {
        // Add profile click listener
        userProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        userBalance = findViewById(R.id.user_balance);
        userProfile = findViewById(R.id.user_profile);
        filterTabs = findViewById(R.id.filter_tabs);
        transactionCount = findViewById(R.id.transaction_count);
        totalProfitLoss = findViewById(R.id.total_profit_loss);
        historyList = findViewById(R.id.history_list);
        emptyHistory = findViewById(R.id.empty_history);
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_history);
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_discovery) {
                startActivity(new Intent(this, Discover.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_portfolio) {
                startActivity(new Intent(this, PortfolioActivity.class));
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

    private void setupFilterTabs() {
        filterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();

                // Reset adapter to ensure clean state
                adapter.clearFilters();

                switch (position) {
                    case 0: // All
                        // No filters - already cleared
                        break;
                    case 1: // Purchases
                        adapter.filterByType(Transaction.Type.BUY);
                        break;
                    case 2: // Sales
                        adapter.filterByType(Transaction.Type.SELL);
                        break;
                    case 3: // Profitable
                        adapter.filterByProfitLoss(true, false);
                        break;
                    case 4: // Loss
                        adapter.filterByProfitLoss(false, true);
                        break;
                }
                updateEmptyState();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Refresh the current filter if tab is reselected
                onTabSelected(tab);
            }
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        historyList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyHistory.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this, transactions);
        historyList.setLayoutManager(new LinearLayoutManager(this));
        historyList.setAdapter(adapter);
    }

    private void loadUserProfile() {
        // Load user photo
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(userProfile);
        }

        // Load user balance
        FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {

                            double investedAmount = 0;
                            if (snapshot.child("stocks").exists()) {
                                for (DataSnapshot stockSnapshot : snapshot.child("stocks").getChildren()) {
                                    if (stockSnapshot.child("avgPrice").exists() && stockSnapshot.child("qty").exists()) {
                                        double avgPrice = stockSnapshot.child("avgPrice").getValue(Double.class);
                                        int qty = stockSnapshot.child("qty").getValue(Integer.class);
                                        investedAmount += avgPrice * qty;
                                    }
                                }
                            }

                            availableBalance = 10000 - investedAmount;
                            updateBalanceUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading user data", error.toException());
                    }
                });
    }

    private void updateBalanceUI() {
        userBalance.setText(String.format(Locale.US, "$%,.2f", availableBalance));
    }

    private void loadTransactionHistory() {
        Log.d(TAG, "Loading transaction history for user: " + userId);

        FirebaseDatabase.getInstance().getReference("transactions")
                .child(userId)
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Transaction data snapshot exists: " + snapshot.exists() +
                                ", children count: " + snapshot.getChildrenCount());

                        transactions.clear();
                        double totalProfit = 0;

                        if (snapshot.exists()) {
                            for (DataSnapshot transactionSnapshot : snapshot.getChildren()) {
                                try {
                                    Log.d(TAG, "Processing transaction: " + transactionSnapshot.getKey());

                                    Map<String, Object> data = new HashMap<>();
                                    for (DataSnapshot child : transactionSnapshot.getChildren()) {
                                        data.put(child.getKey(), child.getValue());
                                        Log.d(TAG, "  - " + child.getKey() + ": " + child.getValue());
                                    }

                                    Transaction transaction = Transaction.fromMap(data);
                                    transactions.add(transaction);
                                    Log.d(TAG, "Added transaction: " + transaction.getSymbol() +
                                            ", Type: " + transaction.getType());

                                    // Add to total profit if it's a sell
                                    if (transaction.getType() == Transaction.Type.SELL) {
                                        totalProfit += transaction.getProfitLoss();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing transaction: " + e.getMessage(), e);
                                }
                            }

                            // Log transaction list
                            Log.d(TAG, "Loaded " + transactions.size() + " transactions");

                            // Sort by timestamp descending (newest first)
                            Collections.sort(transactions,
                                    (t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));

                            // Update transaction count
                            transactionCount.setText(String.valueOf(transactions.size()));

                            // Update total profit/loss
                            boolean isProfit = totalProfit >= 0;
                            totalProfitLoss.setText(String.format(Locale.US, "%s$%.2f",
                                    isProfit ? "+" : "", Math.abs(totalProfit)));
                            totalProfitLoss.setTextColor(ContextCompat.getColor(HistoryActivity.this,
                                    isProfit ? R.color.green : R.color.red));

                            // Update adapter
                            adapter.updateTransactions(transactions);

                            // Debug RecyclerView state
                            Log.d(TAG, "RecyclerView visibility: " + (historyList.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                            Log.d(TAG, "RecyclerView adapter item count: " + adapter.getItemCount());

                            // Show/hide empty state based on filtered items count
                            boolean isEmpty = adapter.getItemCount() == 0;
                            historyList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                            emptyHistory.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                        } else {
                            // No transactions
                            Log.d(TAG, "No transactions found for user");
                            transactionCount.setText("0");
                            totalProfitLoss.setText("$0.00");

                            // Show empty state
                            historyList.setVisibility(View.GONE);
                            emptyHistory.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading transaction history", error.toException());
                        Toast.makeText(HistoryActivity.this,
                                "Failed to load transaction history", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}