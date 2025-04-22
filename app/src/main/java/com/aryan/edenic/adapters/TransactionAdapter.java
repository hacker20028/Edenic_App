package com.aryan.edenic.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;
import com.aryan.edenic.models.Transaction;
import com.aryan.edenic.utils.StockLogoLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> implements Filterable {
    private static final String TAG = "TransactionAdapter";
    private final Context context;
    private final List<Transaction> transactions;
    private List<Transaction> filteredTransactions;
    private Transaction.Type currentFilter = null; // null means show all
    private boolean showProfitOnly = false;
    private boolean showLossOnly = false;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = new ArrayList<>(transactions);
        this.filteredTransactions = new ArrayList<>(transactions);
        Log.d(TAG, "Adapter created with " + transactions.size() + " transactions");
        applyFilters(); // Apply initial filters
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = filteredTransactions.get(position);
        Log.d(TAG, "Binding transaction at position " + position + ": " + transaction.getSymbol());

        // Load stock logo
        StockLogoLoader.loadStockLogo(context, transaction.getSymbol(), holder.stockLogo);

        // Set basic info
        holder.stockSymbol.setText(transaction.getSymbol());
        holder.companyName.setText(transaction.getCompanyName());

        // Format date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        holder.transactionDate.setText(dateFormat.format(transaction.getTimestamp()));

        // Set transaction type
        boolean isBuy = transaction.getType() == Transaction.Type.BUY;
        holder.transactionType.setText(isBuy ? "BUY" : "SELL");
        holder.transactionType.setBackground(ContextCompat.getDrawable(context,
                isBuy ? R.drawable.bg_buy_pill : R.drawable.bg_sell_pill));

        // Set quantity and price
        holder.quantity.setText(String.format(Locale.US, "%d shares", transaction.getQuantity()));
        holder.price.setText(String.format(Locale.US, "$%.2f/share", transaction.getPrice()));

        // Set total value
        holder.totalValue.setText(String.format(Locale.US, "$%.2f", transaction.getTotalValue()));

        // Show or hide profit/loss section based on transaction type
        if (transaction.getType() == Transaction.Type.SELL) {
            holder.profitLossContainer.setVisibility(View.VISIBLE);

            // Format profit/loss
            double profitLoss = transaction.getProfitLoss();
            double profitLossPercent = transaction.getProfitLossPercentage();
            boolean isProfit = profitLoss >= 0;

            holder.profitLoss.setText(String.format(Locale.US, "%s$%.2f (%.1f%%)",
                    isProfit ? "+" : "", Math.abs(profitLoss), Math.abs(profitLossPercent)));

            // Set color based on profit/loss
            int textColor = isProfit ?
                    ContextCompat.getColor(context, R.color.green) :
                    ContextCompat.getColor(context, R.color.red);
            holder.profitLoss.setTextColor(textColor);
        } else {
            holder.profitLossContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return filteredTransactions.size();
    }

    // Update the list of transactions
    public void updateTransactions(List<Transaction> newTransactions) {
        Log.d(TAG, "Updating with " + newTransactions.size() + " transactions");
        this.transactions.clear();
        this.transactions.addAll(newTransactions);
        applyFilters(); // Will update filteredTransactions and notify dataset changed
    }

    // Filter by transaction type
    public void filterByType(Transaction.Type type) {
        Log.d(TAG, "Filtering by type: " + type);
        this.currentFilter = type;
        this.showProfitOnly = false;  // Reset profit/loss filters when filtering by type
        this.showLossOnly = false;
        applyFilters();
    }

    // Filter by profit/loss
    public void filterByProfitLoss(boolean showProfitOnly, boolean showLossOnly) {
        Log.d(TAG, "Filtering by profit/loss - showProfitOnly: " + showProfitOnly + ", showLossOnly: " + showLossOnly);
        this.currentFilter = null;  // Reset type filter when filtering by profit/loss
        this.showProfitOnly = showProfitOnly;
        this.showLossOnly = showLossOnly;
        applyFilters();
    }

    // Show all transactions
    public void clearFilters() {
        Log.d(TAG, "Clearing all filters");
        this.currentFilter = null;
        this.showProfitOnly = false;
        this.showLossOnly = false;
        applyFilters();
    }

    // Apply all active filters
    private void applyFilters() {
        Log.d(TAG, "Applying filters - Type: " + currentFilter +
                ", ShowProfit: " + showProfitOnly + ", ShowLoss: " + showLossOnly);

        filteredTransactions.clear();

        // If no filters are active, show all transactions
        if (currentFilter == null && !showProfitOnly && !showLossOnly) {
            filteredTransactions.addAll(transactions);
            Log.d(TAG, "No filters active, showing all " + transactions.size() + " transactions");
        } else {
            // Apply filters
            for (Transaction transaction : transactions) {
                // Check type filter
                if (currentFilter != null && transaction.getType() != currentFilter) {
                    Log.d(TAG, "Filtered out by type: " + transaction.getSymbol());
                    continue;
                }

                // Check profit/loss filters
                if (transaction.getType() == Transaction.Type.SELL) {
                    double profitLoss = transaction.getProfitLoss();

                    if (showProfitOnly && profitLoss <= 0) {
                        Log.d(TAG, "Filtered out not-profitable: " + transaction.getSymbol());
                        continue;
                    }

                    if (showLossOnly && profitLoss >= 0) {
                        Log.d(TAG, "Filtered out not-loss: " + transaction.getSymbol());
                        continue;
                    }
                } else if (showProfitOnly || showLossOnly) {
                    // Skip buy transactions if filtering by profit/loss
                    Log.d(TAG, "Filtered out buy when profit/loss filter: " + transaction.getSymbol());
                    continue;
                }

                // Transaction passed all filters
                filteredTransactions.add(transaction);
                Log.d(TAG, "Passed all filters: " + transaction.getSymbol());
            }
        }

        Log.d(TAG, "After filtering: " + filteredTransactions.size() + " transactions");
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Transaction> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // No search constraint, apply current type/profit filters
                    for (Transaction transaction : transactions) {
                        // Apply type filter
                        if (currentFilter != null && transaction.getType() != currentFilter) {
                            continue;
                        }

                        // Apply profit/loss filter
                        if (transaction.getType() == Transaction.Type.SELL) {
                            double profitLoss = transaction.getProfitLoss();
                            if ((showProfitOnly && profitLoss <= 0) ||
                                    (showLossOnly && profitLoss >= 0)) {
                                continue;
                            }
                        } else if (showProfitOnly || showLossOnly) {
                            continue;
                        }

                        filteredList.add(transaction);
                    }
                } else {
                    // Apply search + other filters
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (Transaction transaction : transactions) {
                        // Apply all filters
                        if ((currentFilter != null && transaction.getType() != currentFilter) ||
                                (transaction.getType() == Transaction.Type.SELL &&
                                        ((showProfitOnly && transaction.getProfitLoss() <= 0) ||
                                                (showLossOnly && transaction.getProfitLoss() >= 0))) ||
                                ((showProfitOnly || showLossOnly) && transaction.getType() == Transaction.Type.BUY)) {
                            continue;
                        }

                        // Check if matches search query
                        if (transaction.getSymbol().toLowerCase().contains(filterPattern) ||
                                transaction.getCompanyName().toLowerCase().contains(filterPattern)) {
                            filteredList.add(transaction);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredTransactions.clear();
                //noinspection unchecked
                filteredTransactions.addAll((List<Transaction>) results.values);
                notifyDataSetChanged();
                Log.d(TAG, "Search filter applied, showing " + filteredTransactions.size() + " items");
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView stockLogo;
        TextView stockSymbol;
        TextView companyName;
        TextView transactionType;
        TextView transactionDate;
        TextView quantity;
        TextView price;
        TextView totalValue;
        LinearLayout profitLossContainer;
        TextView profitLoss;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stockLogo = itemView.findViewById(R.id.stock_logo);
            stockSymbol = itemView.findViewById(R.id.stock_symbol);
            companyName = itemView.findViewById(R.id.company_name);
            transactionType = itemView.findViewById(R.id.transaction_type);
            transactionDate = itemView.findViewById(R.id.transaction_date);
            quantity = itemView.findViewById(R.id.quantity);
            price = itemView.findViewById(R.id.price);
            totalValue = itemView.findViewById(R.id.total_value);
            profitLossContainer = itemView.findViewById(R.id.profit_loss_container);
            profitLoss = itemView.findViewById(R.id.profit_loss);
        }
    }
}