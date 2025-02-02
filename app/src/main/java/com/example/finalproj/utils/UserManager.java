package com.example.finalproj.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserManager {
    private static final String TAG = "UserManager";
    private static final double INITIAL_BALANCE = 300000.00;
    private static DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

    public interface BalanceCallback {
        void onBalanceReceived(double balance);
        void onError(String error);
    }

    // Set initial balance for existing user
    public static void setInitialBalanceForUser(String userId) {
        DatabaseReference userRef = databaseRef.child("users").child(userId);

        userRef.child("balance").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    userRef.child("balance").setValue(INITIAL_BALANCE)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Initial balance set for existing user"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to set initial balance: " + e.getMessage()));
                }
            }
        });
    }

    // Get current user's balance
    public static void getUserBalance(BalanceCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }

        DatabaseReference balanceRef = databaseRef.child("users")
                .child(currentUser.getUid())
                .child("balance");

        balanceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Double balance = dataSnapshot.getValue(Double.class);
                    if (balance != null) {
                        callback.onBalanceReceived(balance);
                    } else {
                        setInitialBalanceForUser(currentUser.getUid());
                        callback.onBalanceReceived(INITIAL_BALANCE);
                    }
                } else {
                    setInitialBalanceForUser(currentUser.getUid());
                    callback.onBalanceReceived(INITIAL_BALANCE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Balance read failed: " + databaseError.getMessage());
                callback.onError(databaseError.getMessage());
            }
        });
    }

    // Update user's balance
    public static void updateBalance(double newBalance, BalanceCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }

        DatabaseReference balanceRef = databaseRef.child("users")
                .child(currentUser.getUid())
                .child("balance");

        balanceRef.setValue(newBalance)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Balance updated successfully");
                    callback.onBalanceReceived(newBalance);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update balance: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }
}