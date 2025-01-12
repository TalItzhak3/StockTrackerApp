package com.example.finalproj.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.finalproj.adapters.PortfolioAdapter;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.ApiManager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PortfolioFragment extends Fragment {

    private static final String TAG = "PortfolioFragment";
    private RecyclerView recyclerView;
    private PortfolioAdapter adapter;
    private List<Stock> stockList;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvTotalValue;
    private TextView tvError;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        tvTotalValue = view.findViewById(R.id.tvTotalValue);
        tvError = view.findViewById(R.id.tvError);

        swipeRefreshLayout.setOnRefreshListener(this::loadStockData);
    }

    private void setupRecyclerView() {
        stockList = new ArrayList<>();
        adapter = new PortfolioAdapter(requireContext(), stockList, new PortfolioAdapter.StockActionListener() {
            @Override
            public void onBuyClicked(Stock stock) {
                // Handle buy action
                Toast.makeText(getContext(), "Buy " + stock.getSymbol(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSellClicked(Stock stock) {
                // Handle sell action
                Toast.makeText(getContext(), "Sell " + stock.getSymbol(), Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadStockData() {
        showLoading();
        hideError();
        stockList.clear();
        adapter.notifyDataSetChanged();

        // Load stocks one by one with a delay between each
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN", "TSLA"};
        loadStocksSequentially(symbols, 0);
    }

    private void loadStocksSequentially(String[] symbols, int index) {
        if (index >= symbols.length) {
            hideLoading();
            return;
        }

        String symbol = symbols[index];
        ApiManager.getStockQuote(symbol, new ApiManager.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray results = response.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject stockData = results.getJSONObject(0);
                        Stock stock = createStockFromResponse(stockData, symbol);

                        requireActivity().runOnUiThread(() -> {
                            stockList.add(stock);
                            adapter.notifyDataSetChanged();
                            updateTotalValue();
                            if (stockList.size() == 1) {
                                hideError();
                            }
                        });
                    }

                    // Schedule next stock load with delay
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    loadStocksSequentially(symbols, index + 1),
                            1000 // 1 second delay between requests
                    );

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing stock data for " + symbol, e);
                    handleError("Error processing " + symbol + " data");
                    // Continue with next stock despite error
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    loadStocksSequentially(symbols, index + 1),
                            1000
                    );
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "API call failed for " + symbol + ": " + errorMessage);
                handleError(errorMessage);
                // Try next stock after error
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                                loadStocksSequentially(symbols, index + 1),
                        1000
                );
            }
        });
    }

    private Stock createStockFromResponse(JSONObject stockData, String symbol) throws Exception {
        double closePrice = stockData.getDouble("c");
        double openPrice = stockData.getDouble("o");
        double changePercent = ((closePrice - openPrice) / openPrice) * 100;

        Stock stock = new Stock(
                symbol,
                getCompanyName(symbol), // Temporary name until we get company details
                closePrice,
                10, // Default quantity - should come from user portfolio data
                stockData.optString("t", ""), // Trading date
                openPrice,
                "https://logo.clearbit.com/" + symbol.toLowerCase() + ".com"
        );
        stock.setChangePercent(changePercent);
        return stock;
    }

    private String getCompanyName(String symbol) {
        // Temporary helper method until we get real company names
        switch (symbol) {
            case "AAPL": return "Apple Inc.";
            case "GOOGL": return "Alphabet Inc.";
            case "MSFT": return "Microsoft Corporation";
            case "AMZN": return "Amazon.com Inc.";
            case "TSLA": return "Tesla, Inc.";
            default: return symbol;
        }
    }

    private void updateTotalValue() {
        double totalValue = stockList.stream()
                .mapToDouble(stock -> stock.getPrice() * stock.getQuantity())
                .sum();
        tvTotalValue.setText(String.format("Total Value: $%,.2f", totalValue));
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
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void handleError(String error) {
        requireActivity().runOnUiThread(() -> {
            if (stockList.isEmpty()) {
                showError(error);
            } else {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
            hideLoading();
        });
    }
}