package com.aryan.edenic.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;
import com.aryan.edenic.models.PortfolioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.ViewHolder> implements Filterable {
    private static final String TAG = "PortfolioAdapter";
    private final Context context;
    private final List<PortfolioItem> portfolioItems;
    private List<PortfolioItem> filteredItems;

    public PortfolioAdapter(Context context, List<PortfolioItem> portfolioItems) {
        this.context = context;
        this.portfolioItems = portfolioItems;
        this.filteredItems = new ArrayList<>(portfolioItems);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_portfolio_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PortfolioItem item = filteredItems.get(position);

        // Log binding details to debug
        Log.d(TAG, "Binding item at position " + position + ": " + item.getSymbol() +", qty: " + item.getQuantity() + ", price: " + item.getCurrentPrice());

        // Set stock symbol
        holder.stockSymbol.setText(item.getSymbol());

        // Format quantity and average price - use capital X to match reference image
        holder.quantity.setText(String.format(Locale.US, "%d X", item.getQuantity()));
        holder.avgPrice.setText(String.format(Locale.US, "$%.2f", item.getAvgPurchasePrice()));

        // Calculate profit/loss
        double profitLoss = item.getProfitLoss();
        double profitLossPercent = item.getProfitLossPercentage();

        // Format total change with sign and color
        boolean isProfit = profitLoss >= 0;
        int textColor = isProfit ?
                ContextCompat.getColor(context, R.color.green) :
                ContextCompat.getColor(context, R.color.red);

        // We'll use the combined format with sign included directly in text
        holder.changePrefix.setVisibility(View.GONE);

        // Match the exact format in the reference image with rupee symbol
        holder.changeAmount.setText(String.format(Locale.US, "%s$%.2f",
                isProfit ? "+" : "-", Math.abs(profitLoss)));
        holder.changeAmount.setTextColor(textColor);

        // Format percentage in parentheses
        holder.changePercent.setText(String.format(Locale.US, " (%s%.2f%%)",
                isProfit ? "+" : "-", Math.abs(profitLossPercent)));
        holder.changePercent.setTextColor(textColor);

        // Set LTP (Last Traded Price) - this is the current price
        holder.ltpValue.setText(String.format(Locale.US, "$%.2f", item.getCurrentPrice()));

        // Calculate LTP change vs average purchase price
        double ltpChangePercent = ((item.getCurrentPrice() - item.getAvgPurchasePrice()) / item.getAvgPurchasePrice()) * 100;
        boolean isLtpProfit = ltpChangePercent >= 0;

        holder.ltpChange.setText(String.format(Locale.US, " (%s%.2f%%)",
                isLtpProfit ? "+" : "-", Math.abs(ltpChangePercent)));

        // Set LTP change color
        holder.ltpChange.setTextColor(isLtpProfit ?
                ContextCompat.getColor(context, R.color.green) :
                ContextCompat.getColor(context, R.color.red));
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    public void updateItems(List<PortfolioItem> items) {
        this.portfolioItems.clear();
        this.portfolioItems.addAll(items);
        this.filteredItems.clear();
        this.filteredItems.addAll(items);
        notifyDataSetChanged();

        // Log update for debugging
        Log.d(TAG, "Updated adapter with " + items.size() + " items");
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<PortfolioItem> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(portfolioItems);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (PortfolioItem item : portfolioItems) {
                        if (item.getSymbol().toLowerCase().contains(filterPattern) ||
                                item.getCompanyName().toLowerCase().contains(filterPattern)) {
                            filteredList.add(item);
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
                filteredItems.clear();
                //noinspection unchecked
                filteredItems.addAll((List<PortfolioItem>) results.values);
                notifyDataSetChanged();

                Log.d(TAG, "Filter applied, showing " + filteredItems.size() + " items");
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stockSymbol;
        TextView quantity;
        TextView avgPrice;
        TextView changePrefix;
        TextView changeAmount;
        TextView changePercent;
        TextView ltpValue;
        TextView ltpChange;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stockSymbol = itemView.findViewById(R.id.stock_symbol);
            quantity = itemView.findViewById(R.id.quantity);
            avgPrice = itemView.findViewById(R.id.avg_price);
            changePrefix = itemView.findViewById(R.id.change_prefix);
            changeAmount = itemView.findViewById(R.id.change_amount);
            changePercent = itemView.findViewById(R.id.change_percent);
            ltpValue = itemView.findViewById(R.id.ltp_value);
            ltpChange = itemView.findViewById(R.id.ltp_change);
        }
    }
}