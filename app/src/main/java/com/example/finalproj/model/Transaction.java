package com.example.finalproj.model;

import java.util.Date;

public class Transaction {
    private String type;     // "buy" or "sell"
    private String symbol;   // Stock ticker
    private double price;    // Transaction price
    private int quantity;    // Quantity of stocks
    private Date date;       // Date of transaction

    // Default constructor (required for Firebase)
    public Transaction() {
    }

    // Constructor with fields
    public Transaction(String type, String symbol, double price, int quantity, Date date) {
        this.type = type;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.date = date;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
