package com.example.finalproj.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.finalproj.BuildConfig;
import com.example.finalproj.model.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiManager {
    private static final String TAG = "ApiManager";
    private static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";
    private static final String YAHOO_BASE_URL = "https://query1.finance.yahoo.com/";
    private static final String API_KEY = BuildConfig.API_KEY;

    // Cache configuration
    private static final String PREFS_NAME = "StockPrefs";
    private static final String LAST_UPDATE_KEY = "last_update";
    private static final String LAST_GRAPH_UPDATE_KEY = "last_graph_update";
    private static final String GRAPH_DATA_PREFIX = "graph_data";
    private static final long CACHE_DURATION = 12 * 60 * 60 * 1000;
    private static final String TIMEZONE_ISRAEL = "Asia/Jerusalem";

    private static final int MARKET_OPEN_HOUR = 16;  // 16:00 Israel time (9:30 EST)
    private static final int MARKET_CLOSE_HOUR = 23; // 23:00 Israel time (4:00 EST)

    private static final long REQUEST_SPACING = 15000; // 15 seconds between requests
    private static final Queue<PendingRequest> requestQueue = new LinkedList<>();
    private static boolean isProcessingQueue = false;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    // HTTP clients
    private static final OkHttpClient alphaVantageClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final OkHttpClient yahooClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static class TimeRange {
        final long startTime;
        final long endTime;

        TimeRange(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    private static class PendingRequest {
        String symbol;
        String timespan;
        ApiCallback callback;
        Context context;
        boolean isGraphRequest;

        PendingRequest(String symbol, String timespan, ApiCallback callback,
                       Context context, boolean isGraphRequest) {
            this.symbol = symbol;
            this.timespan = timespan;
            this.callback = callback;
            this.context = context;
            this.isGraphRequest = isGraphRequest;
        }
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFailure(String errorMessage);
    }

    public static void getStockQuotes(Context context, String symbol, ApiCallback callback) {
        JSONObject cachedData = getSavedStockData(context, symbol);
        if (cachedData != null && !shouldUpdateStock(context, symbol)) {
            callback.onSuccess(cachedData);
            return;
        }

        requestQueue.add(new PendingRequest(symbol, null, callback, context, false));
        processQueue();
    }

    // Updated Yahoo Finance Methods for Charts
    public static void getStockTimeSeriesData(Context context, String symbol,
                                              String timespan, ApiCallback callback) {
        JSONObject cachedData = getSavedTimeSeriesData(context, symbol, timespan);
        if (cachedData != null && !shouldUpdateTimeSeriesData(context, symbol, timespan)) {
            callback.onSuccess(cachedData);
            return;
        }

        TimeRange timeRange = calculateTimeRange(timespan);
        String interval = getInterval(timespan);

        String url = String.format(Locale.US,
                "%sv8/finance/chart/%s?interval=%s&period1=%d&period2=%d",
                YAHOO_BASE_URL,
                symbol,
                interval,
                timeRange.startTime / 1000,
                timeRange.endTime / 1000);

        Request request = new Request.Builder().url(url).build();
        Log.d(TAG, "Yahoo Finance URL: " + url);

        yahooClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Yahoo API call failed", e);
                handleTimeSeriesFailure(new PendingRequest(symbol, timespan, callback, context, true), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject yahooResponse = new JSONObject(jsonData);
                    JSONObject convertedData = processYahooResponse(yahooResponse, timespan, timeRange);

                    saveTimeSeriesData(context, symbol, timespan, convertedData);
                    handler.post(() -> callback.onSuccess(convertedData));
                } catch (Exception e) {
                    Log.e(TAG, "Error processing Yahoo response", e);
                    handleTimeSeriesError(new PendingRequest(symbol, timespan, callback, context, true), e);
                } finally {
                    response.close();
                }
            }
        });
    }

    private static TimeRange calculateTimeRange(String timespan) {
        Calendar calEnd = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ISRAEL));
        Calendar calStart = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ISRAEL));

        // קביעת זמן סיום
        int currentHour = calEnd.get(Calendar.HOUR_OF_DAY);
        if (currentHour < MARKET_OPEN_HOUR || currentHour >= MARKET_CLOSE_HOUR) {
            if (currentHour < MARKET_OPEN_HOUR) {
                calEnd.add(Calendar.DAY_OF_YEAR, -1);
            }
            calEnd.set(Calendar.HOUR_OF_DAY, MARKET_CLOSE_HOUR);
            calEnd.set(Calendar.MINUTE, 0);
            calEnd.set(Calendar.SECOND, 0);
            calEnd.set(Calendar.MILLISECOND, 0);
        }

        // קביעת זמן התחלה לפי טווח הזמן המבוקש
        switch (timespan) {
            case "1D":
                calStart.setTimeInMillis(calEnd.getTimeInMillis());
                calStart.add(Calendar.DAY_OF_YEAR, -1);
                calStart.set(Calendar.HOUR_OF_DAY, MARKET_OPEN_HOUR);
                calStart.set(Calendar.MINUTE, 0);
                break;
            case "1W":
                calStart.setTimeInMillis(calEnd.getTimeInMillis());
                calStart.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case "1M":
                calStart.setTimeInMillis(calEnd.getTimeInMillis());
                calStart.add(Calendar.MONTH, -1);
                break;
            case "3M":
                calStart.setTimeInMillis(calEnd.getTimeInMillis());
                calStart.add(Calendar.MONTH, -3);
                break;
            case "1Y":
                calStart.setTimeInMillis(calEnd.getTimeInMillis());
                calStart.add(Calendar.YEAR, -1);
                break;
        }

        return new TimeRange(calStart.getTimeInMillis(), calEnd.getTimeInMillis());
    }

    private static String getInterval(String timespan) {
        switch (timespan) {
            case "1D": return "5m";      // 5 minute intervals
            case "1W": return "15m";     // 15 minute intervals
            case "1M": return "60m";     // 1 hour intervals
            case "3M": return "1d";      // Daily intervals
            case "1Y": return "1d";      // Daily intervals
            default: return "5m";
        }
    }

    private static JSONObject processYahooResponse(JSONObject yahooResponse, String timespan, TimeRange timeRange)
            throws Exception {
        JSONObject result = new JSONObject();
        String timeSeriesKey = getTimeSeriesKey(timespan);
        JSONObject timeSeries = new JSONObject();

        JSONObject chart = yahooResponse.getJSONObject("chart");
        JSONArray resultArray = chart.getJSONArray("result");

        if (resultArray.length() == 0) {
            throw new Exception("No data in response");
        }

        JSONObject data = resultArray.getJSONObject(0);
        JSONArray timestamps = data.getJSONArray("timestamp");
        JSONObject indicators = data.getJSONObject("indicators");
        JSONArray quote = indicators.getJSONArray("quote");

        if (quote.length() == 0) {
            throw new Exception("No quote data");
        }

        JSONObject quoteData = quote.getJSONObject(0);
        JSONArray closePrices = quoteData.getJSONArray("close");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE_ISRAEL));

        for (int i = 0; i < timestamps.length(); i++) {
            if (!closePrices.isNull(i)) {
                long timestamp = timestamps.getLong(i) * 1000; // Convert to milliseconds

                // Check if data point is within desired time range
                if (timestamp >= timeRange.startTime && timestamp <= timeRange.endTime) {
                    JSONObject candleData = new JSONObject();
                    candleData.put("4. close", closePrices.getDouble(i));

                    String dateStr = dateFormat.format(new Date(timestamp));
                    timeSeries.put(dateStr, candleData);
                }
            }
        }

        result.put(timeSeriesKey, timeSeries);
        return result;
    }

    private static String getTimeSeriesKey(String timespan) {
        return timespan.equals("1D") ? "Time Series (5min)" : "Time Series (Daily)";
    }

    private static synchronized void processQueue() {
        if (isProcessingQueue || requestQueue.isEmpty()) return;
        isProcessingQueue = true;

        PendingRequest request = requestQueue.poll();
        if (request != null) {
            executeQuoteRequest(request);
        }

        handler.postDelayed(() -> {
            isProcessingQueue = false;
            if (!requestQueue.isEmpty()) {
                processQueue();
            }
        }, REQUEST_SPACING);
    }

    private static void executeQuoteRequest(PendingRequest request) {
        String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                ALPHA_VANTAGE_BASE_URL, request.symbol, API_KEY);

        Request okRequest = new Request.Builder().url(url).build();

        alphaVantageClient.newCall(okRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleQuoteFailure(request, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleQuoteResponse(request, response);
            }
        });
    }

    private static void handleQuoteFailure(PendingRequest request, IOException e) {
        Log.e(TAG, "Network error for " + request.symbol + ": " + e.getMessage());
        JSONObject cachedData = getSavedStockData(request.context, request.symbol);
        if (cachedData != null) {
            request.callback.onSuccess(cachedData);
        } else {
            provideDummyData(request.symbol, request.callback);
        }
    }

    private static void handleQuoteResponse(PendingRequest request, Response response) {
        try {
            String jsonData = response.body().string();
            JSONObject result = new JSONObject(jsonData);

            if (result.has("Note")) {
                String note = result.getString("Note");
                Log.w(TAG, "API Message: " + note);
                if (note.contains("API call frequency")) {
                    handleRateLimitExceeded(request);
                    return;
                }
            }

            if (!result.has("Global Quote")) {
                throw new Exception("Invalid API response");
            }

            saveStockData(request.context, request.symbol, result);
            request.callback.onSuccess(result);
            checkPriceAlert(request.context, request.symbol, result);

        } catch (Exception e) {
            Log.e(TAG, "Error processing response: " + e.getMessage());
            handleQuoteError(request, e);
        } finally {
            if (response.body() != null) {
                response.body().close();
            }
        }
    }

    private static void handleTimeSeriesFailure(PendingRequest request, IOException e) {
        Log.e(TAG, "Network error for " + request.symbol + " time series: " + e.getMessage());
        JSONObject cachedData = getSavedTimeSeriesData(request.context, request.symbol, request.timespan);
        if (cachedData != null) {
            request.callback.onSuccess(cachedData);
        } else {
            request.callback.onFailure("Network error: " + e.getMessage());
        }
    }

    private static void handleTimeSeriesError(PendingRequest request, Exception e) {
        JSONObject cachedData = getSavedTimeSeriesData(request.context,
                request.symbol,
                request.timespan);
        if (cachedData != null) {
            request.callback.onSuccess(cachedData);
        } else {
            request.callback.onFailure("Error processing data: " + e.getMessage());
        }
    }

    private static void handleRateLimitExceeded(PendingRequest request) {
        Log.e(TAG, "API rate limit reached for " + request.symbol);
        if (request.context != null) {
            handler.post(() -> {
                Toast.makeText(request.context,
                        "API rate limit reached. Using cached data.",
                        Toast.LENGTH_SHORT).show();
            });
        }

        JSONObject cachedData = getSavedStockData(request.context, request.symbol);
        if (cachedData != null) {
            request.callback.onSuccess(cachedData);
        } else {
            provideDummyData(request.symbol, request.callback);
        }
    }

    private static void handleQuoteError(PendingRequest request, Exception e) {
        JSONObject cachedData = getSavedStockData(request.context, request.symbol);
        if (cachedData != null) {
            request.callback.onSuccess(cachedData);
        } else {
            provideDummyData(request.symbol, request.callback);
        }
    }

    // Cache Methods
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
        return System.currentTimeMillis() - lastUpdateTime >= CACHE_DURATION;
    }

    private static void saveTimeSeriesData(Context context, String symbol,
                                           String timespan, JSONObject data) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = GRAPH_DATA_PREFIX + "_" + symbol + "_" + timespan;
        editor.putString(key, data.toString());
        editor.putLong(LAST_GRAPH_UPDATE_KEY + "_" + symbol + "_" + timespan,
                System.currentTimeMillis());
        editor.apply();
        Log.d(TAG, "Saved time series data for " + symbol + " timespan: " + timespan);
    }

    private static JSONObject getSavedTimeSeriesData(Context context, String symbol,
                                                     String timespan) {
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

    private static boolean shouldUpdateTimeSeriesData(Context context, String symbol,
                                                      String timespan) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong(LAST_GRAPH_UPDATE_KEY + "_" + symbol + "_" + timespan, 0);
        return System.currentTimeMillis() - lastUpdateTime >= CACHE_DURATION;
    }

    private static void provideDummyData(String symbol, ApiCallback callback) {
        try {
            JSONObject dummyData = new JSONObject();
            JSONObject globalQuote = new JSONObject();
            globalQuote.put("01. symbol", symbol);
            globalQuote.put("05. price", "150.00");
            globalQuote.put("08. previous close", "148.00");
            globalQuote.put("07. latest trading day", new Date().toString());
            dummyData.put("Global Quote", globalQuote);
            callback.onSuccess(dummyData);
        } catch (Exception ex) {
            callback.onFailure("Error creating dummy data");
        }
    }

    private static void checkPriceAlert(Context context, String symbol, JSONObject data) {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String lastNotifTimeKey = "last_notification_" + symbol;
            long lastNotificationTime = prefs.getLong(lastNotifTimeKey, 0);
            long currentTime = System.currentTimeMillis();

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

        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(currentUser.getUid());

        String notificationId = String.valueOf(System.currentTimeMillis()) + "_" + symbol;
        DatabaseReference newNotifRef = notifRef.child(notificationId);

        String title = "Price Alert: " + symbol;
        String message = String.format("%s has changed by %.2f%% ($%.2f → $%.2f)",
                symbol, priceChange, previousPrice, currentPrice);

        NotificationItem notification = new NotificationItem(
                notificationId,
                title,
                message,
                symbol,
                NotificationItem.NotificationType.PRICE_CHANGE,
                priceChange
        );

        newNotifRef.setValue(notification)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Price alert saved for " + symbol))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save price alert: " + e.getMessage()));
    }

    public static void clearCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Cache cleared");
    }
}



