package com.example.finalproj.model;

public class Stock {
    private String symbol;
    private String name;
    private double price;
    private int quantity;
    private String purchaseDate;
    private double previousClose;
    private String logoUrl;
    private double changePercent;

    public Stock(String symbol, String name, double price, int quantity,
                 String purchaseDate, double previousClose) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.purchaseDate = purchaseDate;
        this.previousClose = previousClose;
        this.logoUrl = "https://logo.clearbit.com/" + symbol.toLowerCase() + ".com";
        this.changePercent = ((price - previousClose) / previousClose) * 100;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getPurchaseDate() { return purchaseDate; }
    public double getPreviousClose() { return previousClose; }
    public String getLogoUrl() { return logoUrl; }
    public double getChangePercent() { return changePercent; }

    // Setters
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) {
        this.price = price;
        this.changePercent = ((price - previousClose) / previousClose) * 100;
    }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public void setPreviousClose(double previousClose) {
        this.previousClose = previousClose;
        this.changePercent = ((price - previousClose) / previousClose) * 100;
    }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}