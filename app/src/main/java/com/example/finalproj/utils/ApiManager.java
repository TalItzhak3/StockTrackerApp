package com.example.finalproj.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.finalproj.model.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiManager {
    private static final String TAG = "ApiManager";
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final String API_KEY = "MAQC4VBTF01M1V14";
    private static final String PREFS_NAME = "StockPrefs";
    private static final String LAST_UPDATE_KEY = "last_update";
    private static final String LAST_GRAPH_UPDATE_KEY = "last_graph_update";
    private static final String GRAPH_DATA_PREFIX = "graph_data";
    private static final long UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFailure(String errorMessage);
    }

    // Stock Quote Data Methods
    private static void saveStockData(Context context, String symbol, JSONObject data) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("stock_" + symbol, data.toString());
        editor.putLong(LAST_UPDATE_KEY + "_" + symbol, System.currentTimeMillis());
        editor.apply();
        Log.d(TAG, "Saved data for " + symbol);
    }

    private static JSONObject getSavedStockData(Context context, String symbol) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString("stock_" + symbol, null);
        if (data != null) {
            try {
                return new JSONObject(data);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing saved stock data: " + e.getMessage());
            }
        }
        return null;
    }

    private static boolean shouldUpdateStock(Context context, String symbol) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(LAST_UPDATE_KEY + "_" + symbol, 0);
        return System.currentTimeMillis() - lastUpdateTime >= UPDATE_INTERVAL;
    }

    // Time Series Data Methods
    private static void saveTimeSeriesData(Context context, String symbol, String timespan, JSONObject data) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = GRAPH_DATA_PREFIX + "_" + symbol + "_" + timespan;
        editor.putString(key, data.toString());
        editor.putLong(LAST_GRAPH_UPDATE_KEY + "_" + symbol + "_" + timespan, System.currentTimeMillis());
        editor.apply();
        Log.d(TAG, "Saved time series data for " + symbol + " timespan: " + timespan);
    }

    private static JSONObject getSavedTimeSeriesData(Context context, String symbol, String timespan) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = GRAPH_DATA_PREFIX + "_" + symbol + "_" + timespan;
        String data = prefs.getString(key, null);
        if (data != null) {
            try {
                return new JSONObject(data);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing saved time series data: " + e.getMessage());
            }
        }
        return null;
    }

    private static boolean shouldUpdateTimeSeriesData(Context context, String symbol, String timespan) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(LAST_GRAPH_UPDATE_KEY + "_" + symbol + "_" + timespan, 0);
        return System.currentTimeMillis() - lastUpdateTime >= UPDATE_INTERVAL;
    }

    // Cache Management
    public static void clearCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Cache cleared");
    }

    // Main API Methods
    public static void getStockQuotes(Context context, String symbol, ApiCallback callback) {
        // Try cached data first
        JSONObject savedData = getSavedStockData(context, symbol);
        if (savedData != null && !shouldUpdateStock(context, symbol)) {
            Log.d(TAG, "Using cached data for " + symbol);
            callback.onSuccess(savedData);
            checkPriceAlert(context, symbol, savedData);
            return;
        }

        // If no cached data or needs update, fetch from API
        String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                BASE_URL, symbol, API_KEY);

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error for " + symbol + ": " + e.getMessage());
                if (savedData != null) {
                    callback.onSuccess(savedData);
                } else {
                    callback.onFailure("Network error: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject result = new JSONObject(jsonData);

                    // Check for API errors
                    if (result.has("Error Message")) {
                        throw new Exception(result.getString("Error Message"));
                    }

                    if (result.has("Note")) {
                        Log.w(TAG, "API rate limit reached for " + symbol);
                        if (savedData != null) {
                            callback.onSuccess(savedData);
                            return;
                        }
                        throw new Exception("API rate limit reached");
                    }

                    // Save and return the new data
                    saveStockData(context, symbol, result);
                    callback.onSuccess(result);
                    checkPriceAlert(context, symbol, result);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response: " + e.getMessage());
                    if (savedData != null) {
                        callback.onSuccess(savedData);
                    } else {
                        callback.onFailure("Error processing data: " + e.getMessage());
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    public static void getStockTimeSeriesData(Context context, String symbol, String timespan, ApiCallback callback) {
        // Try cached data first
        JSONObject savedData = getSavedTimeSeriesData(context, symbol, timespan);
        if (savedData != null && !shouldUpdateTimeSeriesData(context, symbol, timespan)) {
            Log.d(TAG, "Using cached time series data for " + symbol);
            callback.onSuccess(savedData);
            return;
        }

        // Determine API function and interval
        String function;
        String interval = "5min";

        switch (timespan) {
            case "1D":
                function = "TIME_SERIES_INTRADAY";
                interval = "5min";
                break;
            case "1W":
            case "1M":
            case "3M":
            case "1Y":
                function = "TIME_SERIES_DAILY";
                interval = "Daily";
                break;
            default:
                function = "TIME_SERIES_INTRADAY";
                interval = "5min";
        }

        // Build URL
        String url;
        if (function.equals("TIME_SERIES_INTRADAY")) {
            url = String.format("%s?function=%s&symbol=%s&interval=%s&apikey=%s&outputsize=full",
                    BASE_URL, function, symbol, interval, API_KEY);
        } else {
            url = String.format("%s?function=%s&symbol=%s&apikey=%s&outputsize=full",
                    BASE_URL, function, symbol, API_KEY);
        }

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error for " + symbol + " time series: " + e.getMessage());
                if (savedData != null) {
                    callback.onSuccess(savedData);
                } else {
                    callback.onFailure("Network error: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject result = new JSONObject(jsonData);

                    // Check for API errors
                    if (result.has("Error Message")) {
                        throw new Exception(result.getString("Error Message"));
                    }

                    if (result.has("Note")) {
                        Log.w(TAG, "API rate limit reached for " + symbol + " time series");
                        if (savedData != null) {
                            callback.onSuccess(savedData);
                            return;
                        }
                        throw new Exception("API rate limit reached");
                    }

                    // Save and return the new data
                    saveTimeSeriesData(context, symbol, timespan, result);
                    callback.onSuccess(result);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing time series response: " + e.getMessage());
                    if (savedData != null) {
                        callback.onSuccess(savedData);
                    } else {
                        callback.onFailure("Error processing data: " + e.getMessage());
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    // Price Alert System
    private static void checkPriceAlert(Context context, String symbol, JSONObject data) {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;

            // Get shared preferences to track last notification time
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String lastNotifTimeKey = "last_notification_" + symbol;
            long lastNotificationTime = prefs.getLong(lastNotifTimeKey, 0);
            long currentTime = System.currentTimeMillis();

            // Only proceed if enough time has passed since last notification (e.g., 15 minutes)
            if (currentTime - lastNotificationTime < TimeUnit.MINUTES.toMillis(15)) {
                Log.d(TAG, "Skipping notification for " + symbol + ": too soon since last alert");
                return;
            }

            JSONObject quote = data.getJSONObject("Global Quote");
            double currentPrice = Double.parseDouble(quote.getString("05. price"));
            double previousClose = Double.parseDouble(quote.getString("08. previous close"));
            double priceChange = ((currentPrice - previousClose) / previousClose) * 100;

            DatabaseReference settingsRef = FirebaseDatabase.getInstance().getReference()
                    .child("notification_settings")
                    .child(currentUser.getUid());

            settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean priceAlerts = snapshot.child("price_alerts").getValue(Boolean.class);
                    String thresholdStr = snapshot.child("price_threshold").getValue(String.class);

                    if (priceAlerts != null && priceAlerts && thresholdStr != null) {
                        try {
                            double threshold = Double.parseDouble(thresholdStr);
                            if (Math.abs(priceChange) >= threshold) {
                                createPriceAlert(context, symbol, currentPrice, previousClose, priceChange);

                                // Save the notification time
                                prefs.edit().putLong(lastNotifTimeKey, currentTime).apply();
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing threshold: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to check notification settings: " + error.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error checking price alert: " + e.getMessage());
        }
    }

    private static void createPriceAlert(Context context, String symbol, double currentPrice,
                                         double previousPrice, double priceChange) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Create notification reference
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(currentUser.getUid());

        // Check for recent similar notifications
        notifRef.orderByChild("timestamp")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean shouldSendNotification = true;

                        for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                            NotificationItem lastNotification = notificationSnapshot.getValue(NotificationItem.class);
                            if (lastNotification != null &&
                                    lastNotification.getStockSymbol().equals(symbol) &&
                                    System.currentTimeMillis() - lastNotification.getTimestamp() < TimeUnit.MINUTES.toMillis(15)) {
                                shouldSendNotification = false;
                                break;
                            }
                        }

                        if (shouldSendNotification) {
                            // Create notification content
                            String title = "Price Alert: " + symbol;
                            String message = String.format("%s has changed by %.2f%% (₪%.2f → ₪%.2f)",
                                    symbol, priceChange, previousPrice, currentPrice);

                            // Create notification object
                            NotificationItem notification = new NotificationItem(
                                    notifRef.push().getKey(),
                                    title,
                                    message,
                                    symbol,
                                    NotificationItem.NotificationType.PRICE_CHANGE,
                                    priceChange
                            );

                            // Save notification to database
                            notifRef.child(notification.getId()).setValue(notification)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Price alert saved for " + symbol))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save price alert: " + e.getMessage()));
                        } else {
                            Log.d(TAG, "Skipping duplicate notification for " + symbol);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking recent notifications: " + error.getMessage());
                    }
                });
    }
}