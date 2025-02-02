package com.example.finalproj.model;

import com.example.finalproj.R;

public class Transaction {
    private String type;
    private String symbol;
    private String stockName;
    private double price;
    private int quantity;
    private double totalValue;
    private String date;
    private int logoResource;

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
        this.logoResource = getLogoResourceBySymbol(symbol);
    }

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
                return R.drawable.placeholder_logo;
        }
    }

    public int getLogoResource() {
        return logoResource;
    }

    public String getType() { return type; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
        this.logoResource = getLogoResourceBySymbol(symbol);
    }

    public String getStockName() { return stockName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalValue() { return totalValue; }

    public String getDate() { return date; }
}