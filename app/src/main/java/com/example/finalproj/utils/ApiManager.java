package com.example.finalproj.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ApiManager {
    private static final String TAG = "ApiManager";
    private static final String API_KEY = "JQajy5Sr802JpYB8rbA_mz6dpfRWFySK"; // Replace with your API key
    private static final String BASE_URL = "https://api.polygon.io/v2";

    // Cache settings
    private static final long CACHE_DURATION = 60 * 1000; // 1 minute cache
    private static final Map<String, CachedResponse> cache = new HashMap<>();

    // Rate limiting
    private static final int REQUESTS_PER_MINUTE = 5;
    private static final long MINUTE_IN_MILLIS = 60 * 1000;
    private static final Queue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    private static class CachedResponse {
        JSONObject data;
        long timestamp;

        CachedResponse(JSONObject data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION;
        }
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onFailure(String errorMessage);
    }

    private static void checkRateLimit(Runnable onSuccess, Runnable onFailure) {
        long currentTime = System.currentTimeMillis();

        while (!requestTimestamps.isEmpty() &&
                currentTime - requestTimestamps.peek() > MINUTE_IN_MILLIS) {
            requestTimestamps.poll();
        }

        if (requestTimestamps.size() < REQUESTS_PER_MINUTE) {
            requestTimestamps.offer(currentTime);
            onSuccess.run();
        } else {
            long oldestRequest = requestTimestamps.peek();
            long waitTime = MINUTE_IN_MILLIS - (currentTime - oldestRequest);
            new Handler(Looper.getMainLooper()).postDelayed(onSuccess, waitTime);
        }
    }

    public static void getStockQuote(String symbol, ApiCallback callback) {
        CachedResponse cachedResponse = cache.get("quote_" + symbol);
        if (cachedResponse != null && cachedResponse.isValid()) {
            callback.onSuccess(cachedResponse.data);
            return;
        }

        checkRateLimit(
                () -> makeStockQuoteRequest(symbol, callback),
                () -> callback.onFailure("Rate limit exceeded. Please try again later.")
        );
    }

    private static void makeStockQuoteRequest(String symbol, ApiCallback callback) {
        String url = BASE_URL + "/aggs/ticker/" + symbol +
                "/prev?adjusted=true&apiKey=" + API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject rawResponse = new JSONObject(jsonData);

                    if (!rawResponse.getString("status").equals("OK")) {
                        callback.onFailure("API error: " +
                                rawResponse.optString("error", "Unknown error"));
                        return;
                    }

                    cache.put("quote_" + symbol, new CachedResponse(rawResponse));
                    callback.onSuccess(rawResponse);

                } catch (Exception e) {
                    callback.onFailure("Error processing data: " + e.getMessage());
                }
            }
        });
    }

    public static void getHistoricalData(String symbol, String timespan, ApiCallback callback) {
        String cacheKey = "historical_" + symbol + "_" + timespan;
        CachedResponse cachedResponse = cache.get(cacheKey);
        if (cachedResponse != null && cachedResponse.isValid()) {
            callback.onSuccess(cachedResponse.data);
            return;
        }

        checkRateLimit(
                () -> makeHistoricalRequest(symbol, timespan, callback),
                () -> callback.onFailure("Rate limit exceeded. Please try again later.")
        );
    }

    private static void makeHistoricalRequest(String symbol, String timespan, ApiCallback callback) {
        String startDate = getStartDate(timespan);
        String endDate = getEndDate();

        String url = BASE_URL + "/aggs/ticker/" + symbol +
                "/range/1/" + getPolygonTimespan(timespan) + "/" +
                startDate + "/" + endDate +
                "?adjusted=true&sort=asc&limit=120&apiKey=" + API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject rawResponse = new JSONObject(jsonData);

                    if (!rawResponse.getString("status").equals("OK")) {
                        callback.onFailure("API error: " +
                                rawResponse.optString("error", "Unknown error"));
                        return;
                    }

                    String cacheKey = "historical_" + symbol + "_" + timespan;
                    cache.put(cacheKey, new CachedResponse(rawResponse));
                    callback.onSuccess(rawResponse);

                } catch (Exception e) {
                    callback.onFailure("Error processing data: " + e.getMessage());
                }
            }
        });
    }

    private static String getStartDate(String timespan) {
        Calendar cal = Calendar.getInstance();
        switch (timespan) {
            case "day":
                cal.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case "week":
                cal.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case "month":
                cal.add(Calendar.MONTH, -1);
                break;
            case "quarter":
                cal.add(Calendar.MONTH, -3);
                break;
            case "year":
                cal.add(Calendar.YEAR, -1);
                break;
            default:
                cal.add(Calendar.MONTH, -1); // Default to 1 month
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    private static String getEndDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static String getPolygonTimespan(String timespan) {
        switch (timespan) {
            case "day": return "minute";
            case "week": return "hour";
            case "month": return "day";
            case "quarter": return "day";
            case "year": return "day";
            default: return "day";
        }
    }
}