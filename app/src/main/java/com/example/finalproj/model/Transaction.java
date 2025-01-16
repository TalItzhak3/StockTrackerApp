package com.example.finalproj.model;

import java.util.Date;

public class Transaction {
    private String type;        // "buy" or "sell"
    private String symbol;      // Stock symbol
    private String stockName;   // Stock name
    private double price;       // Transaction price per share
    private int quantity;       // Number of shares
    private double totalValue;  // Total transaction value
    private String date;        // Transaction date
    private String logoUrl;     // Stock logo URL

    // Required empty constructor for Firebase
    public Transaction() {}

    public Transaction(String type, String symbol, String stockName, double price,
                       int quantity, double totalValue, String date) {
        this.type = type;
        this.symbol = symbol;
        this.stockName = stockName;
        this.price = price;
        this.quantity = quantity;
        this.totalValue = totalValue;
        this.date = date;
        this.logoUrl = "https://logo.clearbit.com/" + symbol.toLowerCase() + ".com";
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
        this.logoUrl = "https://logo.clearbit.com/" + symbol.toLowerCase() + ".com";
    }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}