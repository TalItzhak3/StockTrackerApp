package com.example.finalproj.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalproj.R;
import com.example.finalproj.adapters.WatchlistAdapter;
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

        // Remove previous listener if exists
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

        Context context = getContext();
        if (context == null) {
            onComplete.run();
            return;
        }

        ApiManager.getStockQuotes(context, symbol, new ApiManager.ApiCallback() {
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
                    showToast(stock.getSymbol() + " removed from watchlist");
                    stockCache.remove(stock.getSymbol());
                })
                .addOnFailureListener(e -> {
                    if (!isFragmentActive) return;
                    showToast("Failed to remove " + stock.getSymbol());
                });
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