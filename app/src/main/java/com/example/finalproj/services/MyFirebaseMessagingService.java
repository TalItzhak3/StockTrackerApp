package com.example.finalproj.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.navigation.NavDeepLinkBuilder;

import com.example.finalproj.R;
import com.example.finalproj.activities.MainActivity;
import com.example.finalproj.model.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMessaging";
    private static final String CHANNEL_ID = "stock_alerts";
    private static final String CHANNEL_NAME = "Stock Alerts";
    private static final String CHANNEL_DESC = "Alerts for stock prices and trading activities";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        // Create NotificationItem from received data
        NotificationItem notification = new NotificationItem(
                data.get("id"),
                data.get("title"),
                data.get("message"),
                data.get("stockSymbol"),
                NotificationItem.NotificationType.valueOf(data.get("type")),
                Double.parseDouble(data.getOrDefault("priceChange", "0.0"))
        );

        // Save notification to database
        saveNotification(notification);

        // Show the notification
        sendNotification(notification);
    }

    private void saveNotification(NotificationItem notification) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(currentUser.getUid())
                .child(notification.getId());

        notifRef.setValue(notification)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save notification: " + e.getMessage()));
    }

    private void sendNotification(NotificationItem notification) {
        // Create pending intent for notification click
        PendingIntent pendingIntent = new NavDeepLinkBuilder(this)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.nav_notifications)
                .createPendingIntent();

        // Build the notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(getNotificationIcon(notification.getType()))
                        .setContentTitle(notification.getTitle())
                        .setContentText(notification.getMessage())
                        .setAutoCancel(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }

        // Show notification
        notificationManager.notify(notification.getId().hashCode(), notificationBuilder.build());
    }

    private int getNotificationIcon(NotificationItem.NotificationType type) {
        switch (type) {
            case PRICE_CHANGE:
                return R.drawable.ic_notifications;
            case TRANSACTION:
                return R.drawable.ic_trans;
            case PRICE_TARGET:
            case WATCHLIST_UPDATE:
                return R.drawable.ic_watchlist;
            default:
                return R.drawable.ic_notifications;
        }
    }

    @Override
    public void onNewToken(String token) {
        // Save new FCM token to user's database entry
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("fcmToken");

            tokenRef.setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token: " + e.getMessage()));
        }
    }
}