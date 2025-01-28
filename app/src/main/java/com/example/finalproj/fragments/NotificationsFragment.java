package com.example.finalproj.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.List;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";

    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private List<NotificationItem> notifications;
    private ProgressBar progressBar;
    private TextView tvNoNotifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Switch priceAlertsSwitch;
    private Switch watchlistAlertsSwitch;
    private TextInputEditText priceThresholdInput;
    private MaterialButton btnClearNotifications;

    private FirebaseAuth mAuth;
    private DatabaseReference notificationsRef;
    private DatabaseReference settingsRef;
    private ValueEventListener notificationsListener;
    private ValueEventListener settingsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        initializeFirebase();
        initializeViews(view);
        setupViews();
        setupClearNotificationsButton();
        loadNotifications();
        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        notificationsRef = FirebaseDatabase.getInstance().getReference()
                .child("notifications").child(userId);
        settingsRef = FirebaseDatabase.getInstance().getReference()
                .child("notification_settings").child(userId);
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.rvNotifications);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoNotifications = view.findViewById(R.id.tvNoNotifications);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        priceAlertsSwitch = view.findViewById(R.id.switchPriceAlerts);
        watchlistAlertsSwitch = view.findViewById(R.id.switchWatchlistAlerts);
        priceThresholdInput = view.findViewById(R.id.etPriceChangeThreshold);
        btnClearNotifications = view.findViewById(R.id.btnClearNotifications);
    }

    private void setupViews() {
        // Setup RecyclerView
        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications, this::onNotificationClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);

        // Setup Price Alerts Switch
        priceAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationSetting("price_alerts", isChecked));

        // Setup Watchlist Alerts Switch
        watchlistAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveNotificationSetting("watchlist_alerts", isChecked));

        // Setup Price Threshold Input
        priceThresholdInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s.length() > 0) {
                        double threshold = Double.parseDouble(s.toString());
                        if (threshold > 0) {
                            saveNotificationSetting("price_threshold", String.valueOf(threshold));
                        }
                    }
                } catch (NumberFormatException e) {
                    priceThresholdInput.setError("Please enter a valid number");
                }
            }
        });

        // Load saved settings
        loadSettings();
    }

    private void setupClearNotificationsButton() {
        btnClearNotifications.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear Notifications")
                    .setMessage("Are you sure you want to clear all notifications? This action cannot be undone.")
                    .setPositiveButton("Clear", (dialog, which) -> clearAllNotifications())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void clearAllNotifications() {
        showLoading();
        notificationsRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    notifications.clear();
                    adapter.notifyDataSetChanged();
                    hideLoading();
                    updateEmptyState();
                    Toast.makeText(requireContext(),
                            "All notifications cleared", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(requireContext(),
                            "Failed to clear notifications", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error clearing notifications: " + e.getMessage());
                });
    }

    private void loadSettings() {
        settingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean priceAlerts = snapshot.child("price_alerts").getValue(Boolean.class);
                Boolean watchlistAlerts = snapshot.child("watchlist_alerts").getValue(Boolean.class);
                String threshold = snapshot.child("price_threshold").getValue(String.class);

                priceAlertsSwitch.setChecked(priceAlerts != null && priceAlerts);
                watchlistAlertsSwitch.setChecked(watchlistAlerts != null && watchlistAlerts);

                if (threshold != null && !threshold.equals(priceThresholdInput.getText().toString())) {
                    priceThresholdInput.setText(threshold);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load settings", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveNotificationSetting(String setting, boolean enabled) {
        if (!isAdded()) return; // Check if fragment is attached

        settingsRef.child(setting).setValue(enabled)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {  // Check again before showing Toast
                        Toast.makeText(getContext(),
                                "Settings updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {  // Check again before showing Toast
                        Toast.makeText(getContext(),
                                "Failed to update settings", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveNotificationSetting(String setting, String value) {
        if (!isAdded()) return; // Check if fragment is attached

        settingsRef.child(setting).setValue(value)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {  // Check again before showing Toast
                        Toast.makeText(getContext(),
                                "Threshold updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {  // Check again before showing Toast
                        Toast.makeText(getContext(),
                                "Failed to update threshold", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Add this helper method for showing toasts safely
    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotifications() {
        showLoading();

        Query query = notificationsRef.orderByChild("timestamp");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notifications.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    NotificationItem notification = snapshot.getValue(NotificationItem.class);
                    if (notification != null) {
                        notifications.add(notification);
                    }
                }

                // Sort notifications by timestamp (newest first)
                Collections.sort(notifications, (n1, n2) ->
                        Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                adapter.notifyDataSetChanged();
                hideLoading();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                showError("Failed to load notifications");
            }
        });
    }

    private void onNotificationClick(NotificationItem notification) {
        // Mark notification as read
        notificationsRef.child(notification.getId()).child("read").setValue(true);

        // Handle notification click based on type
        switch (notification.getType()) {
            case PRICE_CHANGE:
                // Navigate to stock details
                break;
            case WATCHLIST_UPDATE:
                // Navigate to watchlist
                break;
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoNotifications.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        tvNoNotifications.setText(message);
        tvNoNotifications.setVisibility(View.VISIBLE);
    }

    private void updateEmptyState() {
        tvNoNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
        if (notifications.isEmpty()) {
            tvNoNotifications.setText("No notifications");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationsListener != null) {
            notificationsRef.removeEventListener(notificationsListener);
        }
    }
}