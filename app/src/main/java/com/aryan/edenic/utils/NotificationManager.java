package com.aryan.edenic.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class NotificationManager {
    private static final String TAG = "NotificationManager";

    // Subscribe to topics based on user's portfolio
    public static void subscribeToStockUpdates(String stockSymbol) {
        FirebaseMessaging.getInstance().subscribeToTopic("stock_" + stockSymbol)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to updates for " + stockSymbol);
                    } else {
                        Log.e(TAG, "Failed to subscribe to " + stockSymbol, task.getException());
                    }
                });
    }

    // Unsubscribe from topics when stocks are sold
    public static void unsubscribeFromStockUpdates(String stockSymbol) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("stock_" + stockSymbol)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Unsubscribed from updates for " + stockSymbol);
                    } else {
                        Log.e(TAG, "Failed to unsubscribe from " + stockSymbol, task.getException());
                    }
                });
    }

    // Save FCM token to user's database record
    public static void saveUserFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save to user profile
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(user.getUid());

                        userRef.child("fcmToken").setValue(token)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to save token", e));
                    }
                });
    }

    // Setup user for all necessary notifications when they log in
    public static void setupUserNotifications() {
        // Subscribe to general app announcements
        FirebaseMessaging.getInstance().subscribeToTopic("announcements");

        // Save the user's FCM token
        saveUserFCMToken();

        // You can add more subscription setup here
    }
}