package com.aryan.edenic;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class EdenicApplication extends Application {
    private static final String TAG = "EdenicApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize notification channels
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Transaction notifications channel
            NotificationChannel transactionChannel = new NotificationChannel(
                    "transactions",
                    "Transactions",
                    NotificationManager.IMPORTANCE_DEFAULT);
            transactionChannel.setDescription("Notifications for buy and sell transactions");
            notificationManager.createNotificationChannel(transactionChannel);

            // General notifications channel
            NotificationChannel generalChannel = new NotificationChannel(
                    "general",
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            generalChannel.setDescription("General app notifications and updates");
            notificationManager.createNotificationChannel(generalChannel);

            Log.d(TAG, "Notification channels created");
        }
    }
}