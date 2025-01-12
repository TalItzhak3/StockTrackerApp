package com.example.finalproj.model;

public class Stock {
    private String symbol;
    private String name;
    private double price;
    private int quantity;
    private String purchaseDate;
    private double previousClose;
    private String logoUrl;
    private double changePercent;  // Added this field

    // Constructor with all fields
    public Stock(String symbol, String name, double price, int quantity,
                 String purchaseDate, double previousClose, String logoUrl) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.purchaseDate = purchaseDate;
        this.previousClose = previousClose;
        this.logoUrl = logoUrl;
        this.changePercent = 0.0;  // Default value
    }

    // Constructor without logoUrl
    public Stock(String symbol, String name, double price, int quantity,
                 String purchaseDate, double previousClose) {
        this(symbol, name, price, quantity, purchaseDate, previousClose, "");
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(double previousClose) {
        this.previousClose = previousClose;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    // Helper methods
    public double getTotalValue() {
        return price * quantity;
    }

    public double getProfitLoss() {
        return (price - previousClose) * quantity;
    }

    public double getProfitLossPercentage() {
        if (previousClose > 0) {
            return ((price - previousClose) / previousClose) * 100;
        }
        return 0;
    }
}