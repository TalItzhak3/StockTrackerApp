package com.example.finalproj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalproj.R;
import com.example.finalproj.adapters.NotificationsAdapter;
import com.example.finalproj.model.NotificationItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";

    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private List<NotificationItem> notifications;
    private ProgressBar progressBar;
    private TextView tvNoNotifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Switch enablePriceAlertsSwitch;
    private Switch enableTransactionAlertsSwitch;
    private Switch enableWatchlistAlertsSwitch;
    private TextInputEditText priceChangeThreshold;
    private FirebaseAuth mAuth;
    private DatabaseReference notificationsRef;
    private DatabaseReference settingsRef;
    private ValueEventListener notificationsListener;
    private ValueEventListener settingsListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        initializeFirebase();
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadNotificationSettings();
        loadNotifications();
        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            notificationsRef = FirebaseDatabase.getInstance().getReference()
                    .child("notifications").child(userId);
            settingsRef = FirebaseDatabase.getInstance().getReference()
                    .child("notification_settings").child(userId);

            // Initialize default settings if they don't exist
            settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("price_alerts", true);
                        defaultSettings.put("transaction_alerts", true);
                        defaultSettings.put("watchlist_alerts", true);
                        defaultSettings.put("price_threshold", "5.0");

                        settingsRef.setValue(defaultSettings)
                                .addOnFailureListener(e -> Log.e(TAG, "Error setting defaults: " + e.getMessage()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking settings: " + error.getMessage());
                }
            });
        }
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.rvNotifications);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoNotifications = view.findViewById(R.id.tvNoNotifications);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        enablePriceAlertsSwitch = view.findViewById(R.id.switchPriceAlerts);
        enableTransactionAlertsSwitch = view.findViewById(R.id.switchTransactionAlerts);
        enableWatchlistAlertsSwitch = view.findViewById(R.id.switchWatchlistAlerts);
        priceChangeThreshold = view.findViewById(R.id.etPriceChangeThreshold);
    }

    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications, this::onNotificationClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);

        enablePriceAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationSetting("price_alerts", isChecked));

        enableTransactionAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationSetting("transaction_alerts", isChecked));

        enableWatchlistAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationSetting("watchlist_alerts", isChecked));

        priceChangeThreshold.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && priceChangeThreshold.getText() != null) {
                String threshold = priceChangeThreshold.getText().toString().trim();
                if (!threshold.isEmpty()) {
                    saveNotificationSetting("price_threshold", threshold);
                }
            }
        });
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null || notificationsRef == null) {
            hideLoading();
            showError("Please login to view notifications");
            return;
        }

        showLoading();
        notifications.clear();
        adapter.notifyDataSetChanged();

        if (notificationsListener != null) {
            notificationsRef.removeEventListener(notificationsListener);
        }

        Query query = notificationsRef.orderByChild("timestamp");
        notificationsListener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notifications.clear();

                if (!dataSnapshot.exists()) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            hideLoading();
                            showError("No notifications yet");
                        });
                    }
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        NotificationItem notification = snapshot.getValue(NotificationItem.class);
                        if (notification != null) {
                            notification.setId(snapshot.getKey());
                            notifications.add(notification);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing notification: " + e.getMessage());
                    }
                }

                Collections.reverse(notifications);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        hideLoading();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        hideLoading();
                        showError("Failed to load notifications");
                    });
                }
            }
        });
    }

    private void loadNotificationSettings() {
        if (settingsRef == null) return;

        if (settingsListener != null) {
            settingsRef.removeEventListener(settingsListener);
        }

        settingsListener = settingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    Boolean priceAlerts = snapshot.child("price_alerts").getValue(Boolean.class);
                    Boolean transactionAlerts = snapshot.child("transaction_alerts").getValue(Boolean.class);
                    Boolean watchlistAlerts = snapshot.child("watchlist_alerts").getValue(Boolean.class);
                    String threshold = snapshot.child("price_threshold").getValue(String.class);

                    enablePriceAlertsSwitch.setChecked(priceAlerts != null && priceAlerts);
                    enableTransactionAlertsSwitch.setChecked(transactionAlerts != null && transactionAlerts);
                    enableWatchlistAlertsSwitch.setChecked(watchlistAlerts != null && watchlistAlerts);
                    if (threshold != null) {
                        priceChangeThreshold.setText(threshold);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading settings", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveNotificationSetting(String setting, boolean enabled) {
        if (settingsRef == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Error: Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        settingsRef.child(setting).setValue(enabled)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Setting saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        // Revert the switch state
                        switch (setting) {
                            case "price_alerts":
                                enablePriceAlertsSwitch.setChecked(!enabled);
                                break;
                            case "transaction_alerts":
                                enableTransactionAlertsSwitch.setChecked(!enabled);
                                break;
                            case "watchlist_alerts":
                                enableWatchlistAlertsSwitch.setChecked(!enabled);
                                break;
                        }
                        Toast.makeText(getContext(), "Failed to save setting", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveNotificationSetting(String setting, String value) {
        if (settingsRef == null || mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Error: Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        settingsRef.child(setting).setValue(value)
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to save setting", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onNotificationClick(NotificationItem notification) {
        if (notificationsRef == null) return;

        // Mark as read
        notificationsRef.child(notification.getId()).child("read").setValue(true);

        // Handle navigation based on type
        if (notification.getStockSymbol() != null) {
            switch (notification.getType()) {
                case PRICE_CHANGE:
                case PRICE_TARGET:
                case WATCHLIST_UPDATE:
                    // Navigate to stock details
                    Bundle args = new Bundle();
                    args.putString("symbol", notification.getStockSymbol());
                    // Implement navigation
                    break;
                case TRANSACTION:
                    // Navigate to transactions
                    // Implement navigation
                    break;
            }
        }
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (tvNoNotifications != null) {
            tvNoNotifications.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showError(String message) {
        if (isAdded() && tvNoNotifications != null) {
            tvNoNotifications.setText(message);
            tvNoNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        if (tvNoNotifications != null) {
            tvNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
            if (notifications.isEmpty()) {
                tvNoNotifications.setText("No notifications");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationsListener != null && notificationsRef != null) {
            notificationsRef.removeEventListener(notificationsListener);
        }
        if (settingsListener != null && settingsRef != null) {
            settingsRef.removeEventListener(settingsListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }
}