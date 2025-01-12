package com.example.finalproj.utils;

public class Constants {
    // Alpha Vantage API Configuration
    public static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";
    public static final String ALPHA_VANTAGE_API_KEY = "QE6W79I0V00LRH5F"; // החלף עם המפתח שלך מאתר Alpha Vantage

    // Firebase Database References
    public static final String FB_REF_USERS = "users";
    public static final String FB_REF_PORTFOLIOS = "portfolios";
    public static final String FB_REF_WATCHLISTS = "watchlists";
    public static final String FB_REF_TRANSACTIONS = "transactions";
    public static final String FB_REF_NOTIFICATIONS = "notifications";

    // API Endpoints
    public static final String ENDPOINT_QUOTE = "GLOBAL_QUOTE";
    public static final String ENDPOINT_SEARCH = "SYMBOL_SEARCH";
    public static final String ENDPOINT_DAILY = "TIME_SERIES_DAILY";
    public static final String ENDPOINT_COMPANY_OVERVIEW = "OVERVIEW";

    // Stock Data Update Intervals (in milliseconds)
    public static final long UPDATE_INTERVAL_REALTIME = 10000;      // 10 seconds
    public static final long UPDATE_INTERVAL_STANDARD = 60000;      // 1 minute
    public static final long UPDATE_INTERVAL_BACKGROUND = 300000;   // 5 minutes

    // Transaction Types
    public static final String TRANSACTION_TYPE_BUY = "buy";
    public static final String TRANSACTION_TYPE_SELL = "sell";

    // Notification Types
    public static final String NOTIFICATION_PRICE_ALERT = "price_alert";
    public static final String NOTIFICATION_NEWS = "news";
    public static final String NOTIFICATION_TRANSACTION = "transaction";

    // Intent Keys
    public static final String INTENT_STOCK_SYMBOL = "stock_symbol";
    public static final String INTENT_STOCK_NAME = "stock_name";
    public static final String INTENT_STOCK_PRICE = "stock_price";
    public static final String INTENT_TRANSACTION_ID = "transaction_id";

    // Shared Preferences Keys
    public static final String PREF_NAME = "StockTrackerPrefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USERNAME = "username";
    public static final String PREF_LAST_UPDATE = "last_update";
    public static final String PREF_THEME = "app_theme";

    // Error Messages
    public static final String ERROR_NETWORK = "Network error occurred. Please check your connection.";
    public static final String ERROR_API = "Error fetching stock data. Please try again later.";
    public static final String ERROR_INVALID_SYMBOL = "Invalid stock symbol.";
    public static final String ERROR_INSUFFICIENT_FUNDS = "Insufficient funds for this transaction.";
    public static final String ERROR_LOGIN_FAILED = "Login failed. Please check your credentials.";
    public static final String ERROR_REGISTRATION_FAILED = "Registration failed. Please try again.";

    // Success Messages
    public static final String SUCCESS_PURCHASE = "Stock purchase successful!";
    public static final String SUCCESS_SALE = "Stock sale successful!";
    public static final String SUCCESS_WATCHLIST_ADD = "Stock added to watchlist.";
    public static final String SUCCESS_WATCHLIST_REMOVE = "Stock removed from watchlist.";
    public static final String SUCCESS_LOGIN = "Login successful.";
    public static final String SUCCESS_REGISTRATION = "Registration successful.";

    // Date Formats
    public static final String DATE_FORMAT_API = "yyyy-MM-dd";
    public static final String DATE_FORMAT_DISPLAY = "dd/MM/yyyy";
    public static final String TIME_FORMAT_DISPLAY = "HH:mm:ss";

    // Chart Configuration
    public static final int CHART_DAYS_DEFAULT = 30;
    public static final int CHART_DAYS_MAX = 365;
    public static final String CHART_TIME_DAILY = "Daily";
    public static final String CHART_TIME_WEEKLY = "Weekly";
    public static final String CHART_TIME_MONTHLY = "Monthly";

    // Default Values
    public static final int DEFAULT_PORTFOLIO_SIZE = 10;
    public static final int DEFAULT_WATCHLIST_SIZE = 20;
    public static final double DEFAULT_PRICE_ALERT_PERCENTAGE = 5.0;

    // API Status Codes
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_FAILED = "failed";

    // Quantity Limits
    public static final int MIN_STOCK_QUANTITY = 1;
    public static final int MAX_STOCK_QUANTITY = 10000;

    // Cache Duration (in milliseconds)
    public static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
}