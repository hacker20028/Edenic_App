package com.aryan.edenic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.aryan.edenic.adapters.OnboardingAdapter;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button nextButton;
    private Button skipButton;
    private LinearLayout dotsContainer;
    private OnboardingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding has been completed before
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);

        // Skip onboarding if not first launch
        if (!isFirstLaunch) {
            navigateToMainActivity();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        // Initialize views
        viewPager = findViewById(R.id.onboarding_viewpager);
        nextButton = findViewById(R.id.next_button);
        skipButton = findViewById(R.id.skip_button);
        dotsContainer = findViewById(R.id.dots_container);

        // Set up adapter with onboarding pages
        adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        // Set up indicator dots
        setupIndicatorDots();

        // Next button click
        nextButton.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });

        // Skip button click
        skipButton.setOnClickListener(v -> completeOnboarding());
    }

    private void setupIndicatorDots() {
        dotsContainer.removeAllViews(); // Clear any existing dots

        // Create dots based on number of pages
        for (int i = 0; i < adapter.getItemCount(); i++) {
            View dot = new View(this);
            int size = (int) getResources().getDimension(R.dimen.dot_size);
            int margin = (int) getResources().getDimension(R.dimen.dot_margin);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);

            // Set the initial state (first dot active, others inactive)
            dot.setBackground(ContextCompat.getDrawable(this,
                    i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive));

            dotsContainer.addView(dot);
        }

        // Update dots when page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Update dots to reflect current position
                for (int i = 0; i < dotsContainer.getChildCount(); i++) {
                    dotsContainer.getChildAt(i).setBackground(
                            ContextCompat.getDrawable(OnboardingActivity.this,
                                    i == position ? R.drawable.dot_active : R.drawable.dot_inactive));
                }

                // Update button text
                if (position == adapter.getItemCount() - 1) {
                    nextButton.setText("Get Started");
                    skipButton.setVisibility(View.GONE);
                } else {
                    nextButton.setText("Next");
                    skipButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void completeOnboarding() {
        // Mark onboarding as completed
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isFirstLaunch", false).apply();

        // Navigate to main activity
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}