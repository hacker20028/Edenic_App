package com.aryan.edenic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;
import com.aryan.edenic.models.LeaderboardEntry;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private static final String TAG = "LeaderboardAdapter";
    private final Context context;
    private final List<LeaderboardEntry> entries = new ArrayList<>();
    private String currentUserId;

    public LeaderboardAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);

        // Load profile image
        if (entry.getPhotoUrl() != null && !entry.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(entry.getPhotoUrl())
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // Set name with position number
        holder.name.setText(String.format("%d. %s", position + 1, entry.getName()));

        // Set portfolio value
        holder.portfolioValue.setText(String.format(Locale.US, "$%.2f", entry.getPortfolioValue()));

        // Set daily change with appropriate color
        double changePercent = entry.getDailyChangePercent();
        boolean isPositive = changePercent >= 0;
        String changeText = String.format(Locale.US, "(%s%.2f%%)",
                isPositive ? "+" : "-", Math.abs(changePercent));

        holder.dailyChange.setText(changeText);

        // Set text color based on profit/loss
        int color = isPositive ?
                ContextCompat.getColor(context, R.color.green) :
                ContextCompat.getColor(context, R.color.red);

        holder.portfolioValue.setTextColor(color);
        holder.dailyChange.setTextColor(color);

        // Position text for current user (outside top 10)
        if (entry.getUserId().equals(currentUserId) && entry.getPosition() > 10) {
            holder.position.setVisibility(View.VISIBLE);
            holder.position.setText(String.format("#%d", entry.getPosition()));
        } else {
            holder.position.setVisibility(View.GONE);
        }

        // Highlight current user's row
        if (entry.getUserId().equals(currentUserId)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.user_highlight));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setEntries(List<LeaderboardEntry> newEntries, String userId) {
        this.entries.clear();
        this.entries.addAll(newEntries);
        this.currentUserId = userId;
        notifyDataSetChanged();
        android.util.Log.d(TAG, "Set " + newEntries.size() + " entries");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView name;
        TextView portfolioValue;
        TextView dailyChange;
        TextView position;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            name = itemView.findViewById(R.id.name);
            portfolioValue = itemView.findViewById(R.id.portfolio_value);
            dailyChange = itemView.findViewById(R.id.daily_change);
            position = itemView.findViewById(R.id.position);
        }
    }
}