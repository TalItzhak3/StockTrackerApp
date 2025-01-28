package com.example.finalproj.network;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class YahooFinanceService {
    private static final String TAG = "YahooFinanceService";
    private static final String BASE_URL = "https://query1.finance.yahoo.com/";
    private final YahooFinanceApi api;

    public interface StockDataCallback {
        void onSuccess(YahooFinanceResponse response);
        void onFailure(String error);
    }

    public YahooFinanceService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(YahooFinanceApi.class);
    }

    public void getStockData(String symbol, String interval, String range, StockDataCallback callback) {
        api.getStockData(symbol, interval, range).enqueue(new Callback<YahooFinanceResponse>() {
            @Override
            public void onResponse(Call<YahooFinanceResponse> call, Response<YahooFinanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("Failed to fetch data");
                }
            }

            @Override
            public void onFailure(Call<YahooFinanceResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onFailure(t.getMessage());
            }
        });
    }
}