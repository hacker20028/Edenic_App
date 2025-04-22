package com.aryan.edenic.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;
import com.aryan.edenic.models.Stock;
import com.aryan.edenic.utils.StockLogoLoader;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {
    private static final String TAG = "StockAdapter";
    private List<Stock> stocks;
    private Consumer<Stock> onStockClick;
    private StockShareListener shareListener;

    // Interface for stock sharing
    public interface StockShareListener {
        void onStockShareRequested(Stock stock);
    }

    // Updated constructor to accept share listener
    public StockAdapter(List<Stock> stocks, Consumer<Stock> onStockClick, StockShareListener shareListener) {
        this.stocks = stocks;
        this.onStockClick = onStockClick;
        this.shareListener = shareListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stocks.get(position);

        // Set persistent gradient for stock
        holder.itemView.setBackgroundResource(getPersistentGradient(stock.getSymbol()));

        // Set basic info
        holder.stockName.setText(stock.getSymbol());
        holder.stockPrice.setText(String.format("$%.2f", stock.getPrice()));

        // Get Percentage Change
        double changePercent = stock.getChangePercent();

        // Load logo
        StockLogoLoader.loadStockLogo(holder.itemView.getContext(), stock.getSymbol(), holder.stockLogo);

        double previousClose = stock.getPreviousClose();

        // Set change display
        String changeText = String.format(Locale.US, "%s%.2f%%",
                changePercent >= 0 ? "+" : "", changePercent);

        holder.stockChange.setText(changeText);
        holder.stockChange.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                changePercent >= 0 ? R.color.green : R.color.red));

        // Set click listener for trading
        holder.itemView.setOnClickListener(v -> {
            if (onStockClick != null) {
                onStockClick.accept(stock);
            }
        });

        // Set long-press listener for sharing
        holder.itemView.setOnLongClickListener(v -> {
            if (shareListener != null) {
                shareListener.onStockShareRequested(stock);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }

    // Helper method for persistent gradients
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

        // Use symbol hash for consistent gradient per stock
        return gradients[Math.abs(symbol.hashCode()) % gradients.length];
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView stockLogo;
        TextView stockName;
        TextView stockPrice;
        TextView stockChange;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stockLogo = itemView.findViewById(R.id.stock_logo);
            stockName = itemView.findViewById(R.id.stock_name);
            stockPrice = itemView.findViewById(R.id.stock_price);
            stockChange = itemView.findViewById(R.id.stock_change);
        }
    }
}