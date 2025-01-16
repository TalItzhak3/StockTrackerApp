package com.example.finalproj.model;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String stockSymbol;
    private NotificationType type;
    private long timestamp;
    private boolean read;
    private double priceChange; // For price change notifications
    private double targetPrice; // For price target notifications

    public enum NotificationType {
        PRICE_CHANGE,
        TRANSACTION,
        PRICE_TARGET,
        WATCHLIST_UPDATE
    }

    // Required empty constructor for Firebase
    public NotificationItem() {}

    public NotificationItem(String id, String title, String message, String stockSymbol,
                            NotificationType type, double priceChange) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.stockSymbol = stockSymbol;
        this.type = type;
        this.priceChange = priceChange;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public double getPriceChange() { return priceChange; }
    public void setPriceChange(double priceChange) { this.priceChange = priceChange; }

    public double getTargetPrice() { return targetPrice; }
    public void setTargetPrice(double targetPrice) { this.targetPrice = targetPrice; }
}