package com.example.finalproj.fragments;

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
import com.example.finalproj.activities.MainActivity;
import com.example.finalproj.adapters.PortfolioAdapter;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.ApiManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PortfolioFragment extends Fragment {
    private static final String TAG = "PortfolioFragment";

    private RecyclerView recyclerView;
    private PortfolioAdapter adapter;
    private List<Stock> stockList;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvError;
    private DatabaseReference watchlistRef;
    private AtomicInteger pendingRequests;
    private int failedRequests = 0;
    private volatile boolean isLoadingData = false;

    private final String[] STOCK_SYMBOLS = {
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
            "META", "NVDA", "NFLX", "JPM", "V",
            "WMT", "DIS", "ADBE", "PYPL", "INTC"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio, container, false);
        initializeViews(view);
        setupRecyclerView();
        loadStockData();
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.rvPortfolio);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvError = view.findViewById(R.id.tvError);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isLoadingData) {
                loadStockData();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        watchlistRef = FirebaseDatabase.getInstance().getReference("watchlists").child(userId);
    }

    private void setupRecyclerView() {
        stockList = new ArrayList<>();
        adapter = new PortfolioAdapter(requireContext(), stockList,
                new PortfolioAdapter.StockActionListener() {
                    @Override
                    public void onBuyClicked(Stock stock) {
                        if (isAdded()) {
                            Toast.makeText(getContext(),
                                    "Buy clicked: " + stock.getSymbol(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onSellClicked(Stock stock) {
                        if (isAdded()) {
                            Toast.makeText(getContext(),
                                    "Sell clicked: " + stock.getSymbol(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFavoriteClicked(Stock stock) {
                        addToWatchlist(stock);
                    }

                    @Override
                    public void onBalanceUpdated(double newBalance) {
                        if (isAdded() && getActivity() != null) {
                            ((MainActivity) getActivity()).updateBalance(newBalance);
                        }
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private synchronized void loadStockData() {
        if (isLoadingData || !isAdded()) return;

        isLoadingData = true;
        showLoading();
        stockList.clear();
        adapter.notifyDataSetChanged();
        failedRequests = 0;

        pendingRequests = new AtomicInteger(STOCK_SYMBOLS.length);

        for (String symbol : STOCK_SYMBOLS) {
            if (!isAdded()) {
                isLoadingData = false;
                return;
            }

            ApiManager.getStockQuotes(requireContext(), symbol, new ApiManager.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    if (!isAdded()) return;

                    try {
                        JSONObject globalQuote = response.getJSONObject("Global Quote");
                        Stock stock = new Stock(
                                symbol,
                                getCompanyName(symbol),
                                Double.parseDouble(globalQuote.getString("05. price")),
                                0,
                                globalQuote.getString("07. latest trading day"),
                                Double.parseDouble(globalQuote.getString("08. previous close"))
                        );

                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            stockList.add(stock);
                            adapter.notifyDataSetChanged();
                            updateTotalValue();
                            checkLoadingComplete(false);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing data for " + symbol, e);
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            failedRequests++;
                            checkLoadingComplete(true);
                        });
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (!isAdded()) return;

                    Log.e(TAG, "Failed to load " + symbol + ": " + errorMessage);
                    requireActivity().runOnUiThread(() -> {
                        failedRequests++;
                        checkLoadingComplete(true);
                    });
                }
            });
        }
    }

    private void checkLoadingComplete(boolean failed) {
        int remaining = pendingRequests.decrementAndGet();
        if (remaining == 0) {
            isLoadingData = false;
            hideLoading();

            if (stockList.isEmpty()) {
                showError("No stocks data available");
            } else if (failedRequests > 0) {
                String errorMsg = String.format("Failed to load %d stocks", failedRequests);
                if (isAdded()) {
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void updateTotalValue() {
        if (!isAdded()) return;

        double totalValue = 0.0;
        for (Stock stock : stockList) {
            totalValue += (stock.getPrice() * stock.getQuantity());
        }
    }

    private void addToWatchlist(Stock stock) {
        if (!isAdded()) return;

        watchlistRef.child(stock.getSymbol()).setValue(stock)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                stock.getSymbol() + " added to favorites",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(),
                                "Failed to add to favorites",
                                Toast.LENGTH_SHORT).show();
                    }
                });
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
        if (!isAdded()) return;
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (!isAdded()) return;
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        if (!isAdded()) return;
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && !isLoadingData) {
            loadStockData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isLoadingData = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isLoadingData = false;
    }
}