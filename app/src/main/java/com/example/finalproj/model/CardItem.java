package com.example.finalproj.model;

public class CardItem {
    private String companyName;
    private String totalAmount;
    private String monthlyAmount;
    private String percentageAmount;
    private String category;
    private String price;
    private String graph;

    // Constructor
    public CardItem(String companyName, String totalAmount, String monthlyAmount,
                    String percentageAmount, String category, String price, String graph) {
        this.companyName = companyName;
        this.totalAmount = totalAmount;
        this.monthlyAmount = monthlyAmount;
        this.percentageAmount = percentageAmount;
        this.category = category;
        this.price = price;
        this.graph = graph;
    }

    // Getters and setters
    // Add all necessary getter and setter methods here
}