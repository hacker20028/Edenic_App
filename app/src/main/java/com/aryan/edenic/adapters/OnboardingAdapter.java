// OnboardingAdapter.java
package com.aryan.edenic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final Context context;
    private final int[] illustrations = {
            R.drawable.onboarding_trading,
            R.drawable.onboarding_portfolio,
            R.drawable.onboarding_social,
            R.drawable.onboarding_leaderboard
    };

    private final String[] titles = {
            "Trade Stocks",
            "Track Your Portfolio",
            "Connect with Traders",
            "Compete on Leaderboard"
    };

    private final String[] descriptions = {
            "Buy and sell stocks with real-time market data in a risk-free environment.",
            "Monitor your holdings and track your performance with detailed analytics.",
            "Share stock recommendations and chat with other traders in the community.",
            "See how your trading skills compare with others on the global leaderboard."
    };

    public OnboardingAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_onboarding_page, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.illustration.setImageResource(illustrations[position]);
        holder.title.setText(titles[position]);
        holder.description.setText(descriptions[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        ImageView illustration;
        TextView title;
        TextView description;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            illustration = itemView.findViewById(R.id.illustration);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
        }
    }
}