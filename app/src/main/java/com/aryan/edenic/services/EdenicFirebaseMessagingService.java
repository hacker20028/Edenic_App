package com.aryan.edenic.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.aryan.edenic.HomeActivity;
import com.aryan.edenic.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class EdenicFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "EdenicMessagingService";
    private static final String CHANNEL_ID = "edenic_notifications";
    private static final String CHANNEL_NAME = "Edenic Notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Save the token to Firebase Database for this user
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot save token: User not logged in");
            return;
        }

        String userId = currentUser.getUid();

        // Save token to Firebase Realtime Database under user's record
        DatabaseReference userTokenRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("fcmToken");

        userTokenRef.setValue(token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM token saved successfully for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save FCM token to database", e);
                });

        // Optional: Also save token to device tokens collection for sending to specific devices
        DatabaseReference tokensRef = FirebaseDatabase.getInstance()
                .getReference("deviceTokens")
                .child(userId);

        // You might store additional info like device name, last active time, etc.
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", token);
        tokenData.put("deviceModel", Build.MODEL);
        tokenData.put("lastUpdated", ServerValue.TIMESTAMP);

        tokensRef.setValue(tokenData);
    }

    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        // Create intent for when user taps notification
        Intent intent = new Intent(this, HomeActivity.class);

        // Add any extra data from the message
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        // Set notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the NotificationChannel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build());
    }
}