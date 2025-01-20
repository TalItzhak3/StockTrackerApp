package com.example.finalproj.fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalproj.R;
import com.example.finalproj.activities.MainActivity;
import com.example.finalproj.adapters.WatchlistAdapter;
import com.example.finalproj.model.NotificationItem;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.ApiManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchlistFragment extends Fragment implements WatchlistAdapter.WatchlistListener {
    private static final String TAG = "WatchlistFragment";

    private RecyclerView recyclerView;
    private WatchlistAdapter adapter;
    private List<Stock> watchlist;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvError;
    private DatabaseReference watchlistRef;
    private Map<String, Stock> stockCache;
    private ValueEventListener watchlistListener;
    private boolean isFragmentActive = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        watchlist = new ArrayList<>();
        stockCache = new HashMap<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);
        initializeViews(view);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        isFragmentActive = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isFragmentActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWatchlistData();
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.rvWatchlist);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvError = view.findViewById(R.id.tvError);

        swipeRefreshLayout.setOnRefreshListener(this::loadWatchlistData);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        watchlistRef = FirebaseDatabase.getInstance().getReference("watchlists").child(userId);
    }

    private void setupRecyclerView() {
        adapter = new WatchlistAdapter(requireContext(), watchlist, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadWatchlistData() {
        if (!isFragmentActive) return;

        showLoading();
        watchlist.clear();
        adapter.notifyDataSetChanged();

        if (watchlistListener != null) {
            watchlistRef.removeEventListener(watchlistListener);
        }

        watchlistListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFragmentActive) return;

                watchlist.clear();
                stockCache.clear();

                if (!snapshot.exists()) {
                    hideLoading();
                    showError("No stocks in watchlist");
                    return;
                }

                int totalStocks = (int) snapshot.getChildrenCount();
                final int[] loadedStocks = {0};

                for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                    String symbol = stockSnapshot.getKey();
                    if (symbol != null) {
                        fetchStockData(symbol, () -> {
                            if (!isFragmentActive) return;

                            loadedStocks[0]++;
                            if (loadedStocks[0] == totalStocks) {
                                hideLoading();
                                if (watchlist.isEmpty()) {
                                    showError("Failed to load watchlist data");
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFragmentActive) return;
                Log.e(TAG, "Database error: " + error.getMessage());
                hideLoading();
                showError("Failed to load watchlist");
            }
        };

        watchlistRef.addValueEventListener(watchlistListener);
    }

    private void fetchStockData(String symbol, Runnable onComplete) {
        if (!isFragmentActive) {
            onComplete.run();
            return;
        }

        ApiManager.getStockQuotes(requireContext(), symbol, new ApiManager.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONObject quote = response.getJSONObject("Global Quote");
                    Stock stock = new Stock(
                            symbol,
                            getCompanyName(symbol),
                            Double.parseDouble(quote.getString("05. price")),
                            0,
                            quote.getString("07. latest trading day"),
                            Double.parseDouble(quote.getString("08. previous close"))
                    );

                    if (isFragmentActive && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (!isFragmentActive) return;
                            stockCache.put(symbol, stock);
                            watchlist.add(stock);
                            adapter.notifyDataSetChanged();
                            onComplete.run();
                        });
                    } else {
                        onComplete.run();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing data for " + symbol, e);
                    handleError("Error processing data", onComplete);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "API error for " + symbol + ": " + errorMessage);
                handleError("Failed to load stock data", onComplete);
            }
        });
    }

    private void handleError(String message, Runnable onComplete) {
        if (isFragmentActive && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!isFragmentActive) return;
                showError(message);
                onComplete.run();
            });
        } else {
            onComplete.run();
        }
    }

    @Override
    public void onRemoveClicked(Stock stock) {
        if (!isFragmentActive) return;

        watchlistRef.child(stock.getSymbol()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (!isFragmentActive) return;
                    createWatchlistAlert(stock, "remove");
                    showToast(stock.getSymbol() + " removed from watchlist");
                    stockCache.remove(stock.getSymbol());
                })
                .addOnFailureListener(e -> {
                    if (!isFragmentActive) return;
                    showToast("Failed to remove " + stock.getSymbol());
                });
    }

    public void addToWatchlist(Stock stock) {
        if (!isFragmentActive) return;

        watchlistRef.child(stock.getSymbol()).setValue(stock)
                .addOnSuccessListener(aVoid -> {
                    if (!isFragmentActive) return;
                    createWatchlistAlert(stock, "add");
                    showToast(stock.getSymbol() + " added to watchlist");
                    stockCache.put(stock.getSymbol(), stock);
                })
                .addOnFailureListener(e -> {
                    if (!isFragmentActive) return;
                    showToast("Failed to add " + stock.getSymbol());
                });
    }

    private void createWatchlistAlert(Stock stock, String action) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference settingsRef = FirebaseDatabase.getInstance().getReference()
                .child("notification_settings")
                .child(userId);
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                .child("notifications")
                .child(userId);

        settingsRef.child("watchlist_alerts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean watchlistAlerts = snapshot.getValue(Boolean.class);
                if (watchlistAlerts != null && watchlistAlerts) {
                    String notifId = notifRef.push().getKey();
                    if (notifId == null) return;

                    String title = "Watchlist Update: " + stock.getSymbol();
                    String message = String.format("%s has been %s your watchlist",
                            stock.getSymbol(),
                            action.equals("add") ? "added to" : "removed from");

                    NotificationItem notification = new NotificationItem(
                            notifId,
                            title,
                            message,
                            stock.getSymbol(),
                            NotificationItem.NotificationType.WATCHLIST_UPDATE,
                            0
                    );

                    notifRef.child(notifId).setValue(notification)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Watchlist alert saved");
                                createSystemNotification(getContext(), title, message);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to save watchlist alert: " + e.getMessage()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check watchlist alert settings: " + error.getMessage());
            }
        });
    }

    private void createSystemNotification(Context context, String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "watchlist_alerts",
                    "Watchlist Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "watchlist_alerts")
                .setSmallIcon(R.drawable.ic_watchlist)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        int notificationId = (title + message).hashCode();
        notificationManager.notify(notificationId, builder.build());
    }

    private void showToast(String message) {
        if (isFragmentActive && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private String getCompanyName(String symbol) {
        switch (symbol) {
            case "AAPL": return "Apple Inc.";
            case "GOOGL": return "Alphabet Inc.";
            case "MSFT": return "Microsoft Corporation";
            case "AMZN": return "Amazon.com Inc.";
            case "TSLA": return "Tesla, Inc.";
            case "META": return "Meta Platforms Inc.";
            case "NVDA": return "NVIDIA Corporation";
            case "NFLX": return "Netflix, Inc.";
            case "JPM": return "JPMorgan Chase & Co.";
            case "V": return "Visa Inc.";
            case "WMT": return "Walmart Inc.";
            case "DIS": return "The Walt Disney Company";
            case "ADBE": return "Adobe Inc.";
            case "PYPL": return "PayPal Holdings, Inc.";
            case "INTC": return "Intel Corporation";
            default: return symbol;
        }
    }

    private void showLoading() {
        if (isFragmentActive) {
            progressBar.setVisibility(View.VISIBLE);
            tvError.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (isFragmentActive) {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showError(String message) {
        if (!isFragmentActive) return;

        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(watchlist.isEmpty() ? View.VISIBLE : View.GONE);
            if (!watchlist.isEmpty()) {
                showToast(message);
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (watchlistListener != null && watchlistRef != null) {
            watchlistRef.removeEventListener(watchlistListener);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        isFragmentActive = false;
        super.onDestroy();
    }
}