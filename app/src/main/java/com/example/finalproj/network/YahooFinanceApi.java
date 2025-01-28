package com.example.finalproj.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface YahooFinanceApi {
    @GET("v8/finance/chart/{symbol}")
    Call<YahooFinanceResponse> getStockData(
            @Path("symbol") String symbol,
            @Query("interval") String interval,
            @Query("range") String range
    );
}