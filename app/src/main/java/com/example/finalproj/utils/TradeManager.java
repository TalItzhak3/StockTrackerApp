package com.example.finalproj.utils;

import com.example.finalproj.model.Stock;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TradeManager {
    private static final DatabaseReference DATABASE = FirebaseDatabase.getInstance().getReference();

    public interface TradeCallback {
        void onSuccess(double newBalance);
        void onError(String error);
    }

    public static void executeBuyOrder(Stock stock, int quantity, double totalCost, TradeCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = DATABASE.child("users").child(userId);
        DatabaseReference portfolioRef = DATABASE.child("portfolios").child(userId);
        DatabaseReference transactionRef = DATABASE.child("transactions").child(userId).push();

        // Verify user balance
        userRef.child("balance").get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                callback.onError("Balance not found");
                return;
            }

            double currentBalance = snapshot.getValue(Double.class);
            if (currentBalance < totalCost) {
                callback.onError("Insufficient funds");
                return;
            }

            // Update balance
            double newBalance = currentBalance - totalCost;
            userRef.child("balance").setValue(newBalance).addOnSuccessListener(aVoid -> {
                // Update portfolio
                updatePortfolio(portfolioRef, stock, quantity, true, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // Record transaction
                        recordTransaction(transactionRef, stock, quantity, "buy", totalCost, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                callback.onSuccess(newBalance);
                            }
                        });
                    }
                });
            }).addOnFailureListener(e -> callback.onError("Failed to update balance"));
        }).addOnFailureListener(e -> callback.onError("Failed to get balance"));
    }

    public static void executeSellOrder(Stock stock, int quantity, double totalValue, TradeCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = DATABASE.child("users").child(userId);
        DatabaseReference portfolioRef = DATABASE.child("portfolios").child(userId);
        DatabaseReference transactionRef = DATABASE.child("transactions").child(userId).push();

        // Verify stock ownership
        portfolioRef.child(stock.getSymbol()).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists() || !snapshot.hasChild("quantity")) {
                callback.onError("No stocks to sell");
                return;
            }

            int currentQuantity = snapshot.child("quantity").getValue(Integer.class);
            if (currentQuantity < quantity) {
                callback.onError("Not enough stocks to sell");
                return;
            }

            // Update balance
            userRef.child("balance").get().addOnSuccessListener(balanceSnapshot -> {
                double currentBalance = balanceSnapshot.getValue(Double.class);
                double newBalance = currentBalance + totalValue;

                userRef.child("balance").setValue(newBalance).addOnSuccessListener(aVoid -> {
                    // Update portfolio
                    updatePortfolio(portfolioRef, stock, quantity, false, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            // Record transaction
                            recordTransaction(transactionRef, stock, quantity, "sell", totalValue, new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    callback.onSuccess(newBalance);
                                }
                            });
                        }
                    });
                }).addOnFailureListener(e -> callback.onError("Failed to update balance"));
            }).addOnFailureListener(e -> callback.onError("Failed to get balance"));
        }).addOnFailureListener(e -> callback.onError("Failed to verify stock ownership"));
    }

    private static void updatePortfolio(DatabaseReference portfolioRef, Stock stock,
                                        int quantity, boolean isBuying, OnSuccessListener<Void> callback) {
        portfolioRef.child(stock.getSymbol()).get().addOnSuccessListener(snapshot -> {
            int currentQuantity = 0;
            if (snapshot.exists() && snapshot.hasChild("quantity")) {
                currentQuantity = snapshot.child("quantity").getValue(Integer.class);
            }

            int newQuantity = isBuying ? currentQuantity + quantity : currentQuantity - quantity;

            Map<String, Object> updates = new HashMap<>();
            if (newQuantity > 0) {
                updates.put("symbol", stock.getSymbol());
                updates.put("name", stock.getName());
                updates.put("quantity", newQuantity);
                updates.put("lastUpdateDate", new Date().toString());
                updates.put("lastPrice", stock.getPrice());
                portfolioRef.child(stock.getSymbol()).updateChildren(updates)
                        .addOnSuccessListener(callback);
            } else {
                portfolioRef.child(stock.getSymbol()).removeValue()
                        .addOnSuccessListener(callback);
            }
        });
    }

    private static void recordTransaction(DatabaseReference transactionRef, Stock stock,
                                          int quantity, String type, double totalValue,
                                          OnSuccessListener<Void> callback) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("symbol", stock.getSymbol());
        transaction.put("name", stock.getName());
        transaction.put("quantity", quantity);
        transaction.put("price", stock.getPrice());
        transaction.put("totalValue", totalValue);
        transaction.put("type", type);
        transaction.put("date", new Date().toString());

        transactionRef.setValue(transaction)
                .addOnSuccessListener(callback);
    }
}