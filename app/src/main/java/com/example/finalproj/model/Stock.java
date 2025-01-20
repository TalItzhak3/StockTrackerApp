package com.example.finalproj.model;

import com.example.finalproj.R;

public class Stock {
    private String symbol;
    private String name;
    private double price;
    private int quantity;
    private String purchaseDate;
    private double previousClose;
    private int logoResource;  // Changed from String logoUrl to int logoResource
    private double changePercent;

    public Stock(String symbol, String name, double price, int quantity,
                 String purchaseDate, double previousClose) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.purchaseDate = purchaseDate;
        this.previousClose = previousClose;
        this.logoResource = getLogoResourceBySymbol(symbol); // Get local resource ID
        this.changePercent = ((price - previousClose) / previousClose) * 100;
    }

    // Method to map stock symbols to drawable resources
    private int getLogoResourceBySymbol(String symbol) {
        switch (symbol.toUpperCase()) {
            case "AAPL":
                return R.drawable.logo_aapl;
            case "GOOGL":
                return R.drawable.logo_googl;
            case "MSFT":
                return R.drawable.logo_msft;
            case "AMZN":
                return R.drawable.logo_amzn;
            case "TSLA":
                return R.drawable.logo_tsla;
            case "META":
                return R.drawable.logo_meta;
            case "NVDA":
                return R.drawable.logo_nvda;
            case "NFLX":
                return R.drawable.logo_nflx;
            case "JPM":
                return R.drawable.logo_jpm;
            case "V":
                return R.drawable.logo_v;
            case "WMT":
                return R.drawable.logo_wmt;
            case "DIS":
                return R.drawable.logo_dis;
            case "ADBE":
                return R.drawable.logo_adbe;
            case "PYPL":
                return R.drawable.logo_pypl;
            case "INTC":
                return R.drawable.logo_intc;
            default:
                return R.drawable.placeholder_logo; // Default logo
        }
    }

    // Updated getter for logo
    public int getLogoResource() {
        return logoResource;
    }

    // Other getters and setters remain the same
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getPurchaseDate() { return purchaseDate; }
    public double getPreviousClose() { return previousClose; }
    public double getChangePercent() { return changePercent; }

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
}