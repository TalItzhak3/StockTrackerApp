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
    private TextView tvError, tvTotalValue;
    private DatabaseReference watchlistRef;
    private AtomicInteger pendingRequests;

    private final String[] STOCK_SYMBOLS = {
            "AAPL",  // Apple
            "GOOGL", // Google (Alphabet)
            "MSFT",  // Microsoft
            "AMZN",  // Amazon
            "TSLA",  // Tesla
            "META",  // Meta (Facebook)
            "NVDA",  // NVIDIA
            "NFLX",  // Netflix
            "JPM",   // JPMorgan Chase
            "V",     // Visa
            "WMT",   // Walmart
            "DIS",   // Disney
            "ADBE",  // Adobe
            "PYPL",  // PayPal
            "INTC"   // Intel
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
        swipeRefreshLayout.setOnRefreshListener(this::loadStockData);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        watchlistRef = FirebaseDatabase.getInstance().getReference("watchlists").child(userId);
    }

    private void setupRecyclerView() {
        stockList = new ArrayList<>();
        adapter = new PortfolioAdapter(requireContext(), stockList,
                new PortfolioAdapter.StockActionListener() {
                    @Override
                    public void onBuyClicked(Stock stock) {
                        Toast.makeText(getContext(),
                                "Buy clicked: " + stock.getSymbol(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSellClicked(Stock stock) {
                        Toast.makeText(getContext(),
                                "Sell clicked: " + stock.getSymbol(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFavoriteClicked(Stock stock) {
                        addToWatchlist(stock);
                    }

                    @Override
                    public void onBalanceUpdated(double newBalance) {
                        if (getActivity() != null) {
                            ((MainActivity) getActivity()).updateBalance(newBalance);
                        }
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadStockData() {
        showLoading();
        stockList.clear();
        adapter.notifyDataSetChanged();

        pendingRequests = new AtomicInteger(STOCK_SYMBOLS.length);
        double totalPortfolioValue = 0.0;

        for (String symbol : STOCK_SYMBOLS) {
            ApiManager.getStockQuotes(requireContext(), symbol, new ApiManager.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        JSONObject globalQuote = response.getJSONObject("Global Quote");

                        Stock stock = new Stock(
                                symbol,
                                getCompanyName(symbol),
                                Double.parseDouble(globalQuote.getString("05. price")),
                                0, // Default quantity
                                globalQuote.getString("07. latest trading day"),
                                Double.parseDouble(globalQuote.getString("08. previous close"))
                        );

                        requireActivity().runOnUiThread(() -> {
                            stockList.add(stock);
                            adapter.notifyDataSetChanged();
                            updateTotalValue();
                            checkLoadingComplete();
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing data for " + symbol, e);
                        requireActivity().runOnUiThread(() -> {
                            checkLoadingComplete();
                            showError("Error processing data for " + symbol);
                        });
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Failed to load " + symbol + ": " + errorMessage);
                    requireActivity().runOnUiThread(() -> {
                        checkLoadingComplete();
                        showError("Failed to load " + symbol + ": " + errorMessage);
                    });
                }
            });
        }
    }

    private void updateTotalValue() {
        double totalValue = 0.0;
        for (Stock stock : stockList) {
            totalValue += (stock.getPrice() * stock.getQuantity());
        }
        tvTotalValue.setText(String.format("Total Portfolio Value: ₪%.2f", totalValue));
    }

    private void addToWatchlist(Stock stock) {
        watchlistRef.child(stock.getSymbol()).setValue(stock)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            stock.getSymbol() + " added to favorites",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Failed to add to favorites",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void checkLoadingComplete() {
        if (pendingRequests.decrementAndGet() == 0) {
            hideLoading();
            if (stockList.isEmpty()) {
                showError("No stocks data available");
            }
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
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        if (isAdded()) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStockData(); // Refresh data when fragment becomes visible
    }
}