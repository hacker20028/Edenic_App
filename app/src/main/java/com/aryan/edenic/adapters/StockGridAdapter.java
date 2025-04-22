package com.aryan.edenic.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;
import com.aryan.edenic.models.Stock;
import com.aryan.edenic.utils.StockLogoLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class StockGridAdapter extends RecyclerView.Adapter<StockGridAdapter.ViewHolder> implements Filterable {
    private static final String TAG = "StockGridAdapter";
    private final Context context;
    private final List<Stock> stocks;
    private List<Stock> filteredStocks;
    private final Consumer<Stock> onStockClick;
    private final StockAdapter.StockShareListener shareListener;

    public StockGridAdapter(Context context, List<Stock> stocks, Consumer<Stock> onStockClick,
                            StockAdapter.StockShareListener shareListener) {
        this.context = context;
        this.stocks = new ArrayList<>(stocks);  // Create a copy
        this.filteredStocks = new ArrayList<>(stocks);  // Create a copy
        this.onStockClick = onStockClick;
        this.shareListener = shareListener;
        Log.d(TAG, "Adapter created with " + this.stocks.size() + " stocks");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_grid, parent, false);
        return new ViewHolder(view);
    }

    // In StockGridAdapter.java, modify the change display part in onBindViewHolder
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = filteredStocks.get(position);

        // Set company logo
        StockLogoLoader.loadStockLogo(context, stock.getSymbol(), holder.companyLogo);

        // Set stock info
        holder.stockSymbol.setText(stock.getSymbol());
        holder.stockPrice.setText(String.format(Locale.US, "$ %.2f", stock.getPrice()));

        // Set Percentage Close
        double changePercent = stock.getChangePercent();

        // Enhanced change display with debugging
        double previousClose = stock.getPreviousClose();
        double currentPrice = stock.getPrice();

        Log.d(TAG, "Stock: " + stock.getSymbol() +
                ", Price: " + currentPrice +
                ", PrevClose: " + previousClose);

        // Check if price has been loaded yet
        if (currentPrice <= 0) {
            // Show loading state for price
            holder.stockPrice.setText("Loading...");
            holder.stockChange.setVisibility(View.GONE);
        } else {
            // Price is loaded, show it
            holder.stockPrice.setText(String.format(Locale.US, "$ %.2f", currentPrice));

            // Handle percentage change display
            String changeText = String.format(Locale.US, "%s%.2f%%",
                    changePercent >= 0 ? "+" : "", changePercent);

            holder.stockChange.setText(changeText);
            holder.stockChange.setTextColor(changePercent >= 0 ?
                    ContextCompat.getColor(context, R.color.green) :
                    ContextCompat.getColor(context, R.color.red));
        }

        // Set card background gradient
        holder.cardContainer.setBackgroundResource(getPersistentGradient(stock.getSymbol()));

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (onStockClick != null) {
                onStockClick.accept(stock);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (shareListener != null) {
                shareListener.onStockShareRequested(stock);
                return true;
            }
            return false;
        });
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Stock> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // If no constraint, show all stocks
                    filteredList.addAll(stocks);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    // Filter based on symbol or name containing the pattern
                    for (Stock stock : stocks) {
                        if (stock.getSymbol().toLowerCase().contains(filterPattern) ||
                                stock.getName().toLowerCase().contains(filterPattern)) {
                            filteredList.add(stock);
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
                filteredStocks.clear();
                //noinspection unchecked
                filteredStocks.addAll((List<Stock>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public int getItemCount() {
        return filteredStocks.size();
    }

    /**
     * Filter stocks based on search query
     */
    public void filter(String query) {
        filteredStocks.clear();

        if (query.isEmpty()) {
            // If query is empty, show all stocks
            filteredStocks.addAll(stocks);
        } else {
            // Otherwise, filter by symbol or name containing the query
            String lowerCaseQuery = query.toLowerCase();
            for (Stock stock : stocks) {
                if (stock.getSymbol().toLowerCase().contains(lowerCaseQuery) ||
                        stock.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredStocks.add(stock);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Update the full list of stocks
     */
    // In StockGridAdapter.java - modify the updateStocks method
    public void updateStocks(List<Stock> newStocks) {
        Log.d(TAG, "updateStocks called with " + newStocks.size() + " stocks");
        this.stocks.clear();
        this.stocks.addAll(newStocks);
        this.filteredStocks.clear();
        this.filteredStocks.addAll(newStocks);
        notifyDataSetChanged();
        Log.d(TAG, "After update: stocks size = " + this.stocks.size() +
                ", filtered size = " + this.filteredStocks.size());
    }

    /**
     * Get a consistent gradient based on the stock symbol
     */
    private int getPersistentGradient(String symbol) {
        int[] gradients = {
                R.drawable.gradient_stock_1,
                R.drawable.gradient_stock_2,
                R.drawable.gradient_stock_3,
                R.drawable.gradient_stock_4,
                R.drawable.gradient_stock_5,
                R.drawable.gradient_stock_6,
                R.drawable.gradient_stock_7,
                R.drawable.gradient_stock_8,
                R.drawable.gradient_stock_9,
                R.drawable.gradient_stock_10,
                R.drawable.gradient_stock_11,
                R.drawable.gradient_stock_12,
                R.drawable.gradient_stock_13
        };

        // Use the hash code of the symbol to get a consistent gradient
        int index = Math.abs(symbol.hashCode()) % gradients.length;
        return gradients[index];
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout cardContainer;
        ImageView companyLogo;
        TextView stockSymbol;
        TextView stockPrice;
        TextView stockChange;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.stock_card_container);
            companyLogo = itemView.findViewById(R.id.company_logo);
            stockSymbol = itemView.findViewById(R.id.stock_symbol);
            stockPrice = itemView.findViewById(R.id.stock_price);
            stockChange = itemView.findViewById(R.id.stock_change);
        }
    }
}