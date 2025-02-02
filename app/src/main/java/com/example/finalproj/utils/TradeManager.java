package com.example.finalproj.utils;

import android.util.Log;
import com.example.finalproj.model.NotificationItem;
import com.example.finalproj.model.Stock;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TradeManager {
    private static final String TAG = "TradeManager";
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

            double newBalance = currentBalance - totalCost;
            userRef.child("balance").setValue(newBalance).addOnSuccessListener(aVoid -> {
                // Update portfolio
                updatePortfolio(portfolioRef, stock, quantity, true, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        recordTransaction(transactionRef, stock, quantity, "buy", totalCost, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                createTransactionAlert(stock, quantity, "buy", totalCost);
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

            userRef.child("balance").get().addOnSuccessListener(balanceSnapshot -> {
                double currentBalance = balanceSnapshot.getValue(Double.class);
                double newBalance = currentBalance + totalValue;

                userRef.child("balance").setValue(newBalance).addOnSuccessListener(aVoid -> {
                    updatePortfolio(portfolioRef, stock, quantity, false, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            recordTransaction(transactionRef, stock, quantity, "sell", totalValue, new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    createTransactionAlert(stock, quantity, "sell", totalValue);
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

    private static void createTransactionAlert(Stock stock, int quantity, String type, double totalValue) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference settingsRef = FirebaseDatabase.getInstance().getReference()
                .child("notification_settings")
                .child(userId);
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                .child("notifications")
                .child(userId);

        settingsRef.child("transaction_alerts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean transactionAlerts = snapshot.getValue(Boolean.class);
                if (transactionAlerts != null && transactionAlerts) {
                    String notifId = notifRef.push().getKey();
                    if (notifId == null) return;

                    String title = "Transaction Alert: " + stock.getSymbol();
                    String message = String.format("Successfully %s %d shares of %s for ₪%.2f",
                            type.equals("buy") ? "bought" : "sold",
                            quantity, stock.getSymbol(), totalValue);

                    NotificationItem notification = new NotificationItem(
                            notifId,
                            title,
                            message,
                            stock.getSymbol(),
                            NotificationItem.NotificationType.TRANSACTION,
                            0
                    );

                    notifRef.child(notifId).setValue(notification)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Transaction alert saved"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to save transaction alert: " + e.getMessage()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check transaction alert settings: " + error.getMessage());
            }
        });
    }
}