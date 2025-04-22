package com.aryan.edenic;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is already signed in BEFORE setting content view
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, go straight to HomeActivity
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
            return;
        }

        // Only show the landing page if not signed in
        setContentView(R.layout.activity_main);

        // Find the "Get Started" button
        Button getStartedButton = findViewById(R.id.get_started_button);

        // Set click listener for the "Get Started" button
        getStartedButton.setOnClickListener(v -> {
            // Navigate to Auth_Activity
            Intent intent = new Intent(MainActivity.this, Auth_Activity.class);
            startActivity(intent);
        });
    }
}