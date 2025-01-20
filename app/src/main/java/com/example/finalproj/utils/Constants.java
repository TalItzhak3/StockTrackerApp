package com.example.finalproj.utils;

import com.example.finalproj.BuildConfig;

public class Constants {
    // API Configuration
    public static final String BASE_URL = "https://www.alphavantage.co/query";
    public static final String API_KEY = BuildConfig.API_KEY; // Replace with your API key

    // SharedPreferences Keys
    public static final String PREFS_NAME = "StockPrefs";
    public static final String LAST_UPDATE_KEY = "last_update_time";
    public static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

    // Firebase Database References
    public static final String FB_REF_USERS = "users";
    public static final String FB_REF_PORTFOLIOS = "portfolios";
    public static final String FB_REF_WATCHLISTS = "watchlists";
    public static final String FB_REF_TRANSACTIONS = "transactions";

    // Default Values
    public static final double INITIAL_BALANCE = 100000.00; // ₪100,000
    public static final int DEFAULT_QUANTITY = 0;

    // Error Messages
    public static final String ERROR_NETWORK = "Network connection error";
    public static final String ERROR_API = "API error occurred";
    public static final String ERROR_DATABASE = "Database error occurred";
    public static final String ERROR_INSUFFICIENT_FUNDS = "Insufficient funds";
    public static final String ERROR_INVALID_QUANTITY = "Invalid quantity";

    // Success Messages
    public static final String SUCCESS_PURCHASE = "Stock purchase successful";
    public static final String SUCCESS_SALE = "Stock sale successful";
    public static final String SUCCESS_UPDATE = "Data updated successfully";
}