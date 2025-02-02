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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalproj.R;
import com.example.finalproj.activities.MainActivity;
import com.example.finalproj.adapters.TradingAdapter;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.ApiManager;
import com.example.finalproj.utils.TradeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TradingFragment extends Fragment {
    private static final String TAG = "TradingFragment";

    // Views
    private RecyclerView rvTrading;
    private TradingAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvError;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvOpenPositions;
    private TextView tvTotalInvestment;
    private TextView totalProfitLoss;

    // Data
    private List<Stock> stockList;
    private FirebaseAuth mAuth;
    private DatabaseReference portfolioRef;
    private ValueEventListener portfolioListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trading, container, false);
        initializeViews(view);
        setupRecyclerView();
        loadPortfolioData();
        return view;
    }

    private void initializeViews(View view) {
        rvTrading = view.findViewById(R.id.rvTrading);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        totalProfitLoss = view.findViewById(R.id.totalProfitLoss);
        tvOpenPositions = view.findViewById(R.id.tvOpenPositions);
        tvTotalInvestment = view.findViewById(R.id.tvTotalInvestment);

        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        portfolioRef = FirebaseDatabase.getInstance().getReference("portfolios").child(userId);

        swipeRefreshLayout.setOnRefreshListener(this::loadPortfolioData);
    }

    private void setupRecyclerView() {
        stockList = new ArrayList<>();
        adapter = new TradingAdapter(stockList, new TradingAdapter.OnTradeActionListener() {
            @Override
            public void onClosePosition(Stock stock) {
                showClosePositionDialog(stock);
            }

            @Override
            public void onSellPart(Stock stock, int quantity, double value) {
                executeSellOrder(stock, quantity, value);
            }
        });
        rvTrading.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTrading.setAdapter(adapter);
    }

    private void loadPortfolioData() {
        showLoading();
        stockList.clear();

        if (portfolioListener != null) {
            portfolioRef.removeEventListener(portfolioListener);
        }

        final double[] totalInvestmentArray = {0.0};

        portfolioListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                stockList.clear();

                if (!snapshot.exists()) {
                    updateUI(0, 0);
                    hideLoading();
                    showError("No stocks in portfolio");
                    return;
                }

                AtomicInteger totalStocks = new AtomicInteger(0);
                for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                    Integer quantity = stockSnapshot.child("quantity").getValue(Integer.class);
                    if (quantity != null && quantity > 0) {
                        totalStocks.incrementAndGet();
                    }
                }

                if (totalStocks.get() == 0) {
                    updateUI(0, 0);
                    hideLoading();
                    showError("No open positions");
                    return;
                }

                AtomicInteger loadedStocks = new AtomicInteger(0);

                for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                    String symbol = stockSnapshot.child("symbol").getValue(String.class);
                    String name = stockSnapshot.child("name").getValue(String.class);
                    Integer quantity = stockSnapshot.child("quantity").getValue(Integer.class);
                    Double purchasePrice = stockSnapshot.child("lastPrice").getValue(Double.class);

                    if (quantity != null && quantity > 0 && purchasePrice != null) {
                        totalInvestmentArray[0] += purchasePrice * quantity;
                        final String finalSymbol = symbol;
                        final String finalName = name;
                        final int finalQuantity = quantity;
                        final double finalPurchasePrice = purchasePrice;

                        ApiManager.getStockQuotes(requireContext(), symbol, new ApiManager.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                try {
                                    JSONObject quote = response.getJSONObject("Global Quote");
                                    double currentPrice = Double.parseDouble(quote.getString("05. price"));

                                    Stock stock = new Stock(
                                            finalSymbol,
                                            finalName,
                                            currentPrice,
                                            finalQuantity,
                                            quote.getString("07. latest trading day"),
                                            finalPurchasePrice
                                    );

                                    requireActivity().runOnUiThread(() -> {
                                        stockList.add(stock);
                                        adapter.notifyDataSetChanged();
                                        updateTotalProfitLoss();

                                        if (loadedStocks.incrementAndGet() == totalStocks.get()) {
                                            updateUI(totalStocks.get(), totalInvestmentArray[0]);
                                            hideLoading();
                                        }
                                    });

                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing data: " + e.getMessage());
                                    handleError(totalStocks.get(), totalInvestmentArray[0]);
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "API error: " + errorMessage);
                                handleError(totalStocks.get(), totalInvestmentArray[0]);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                hideLoading();
                showError("Failed to load portfolio data");
            }
        };

        portfolioRef.addValueEventListener(portfolioListener);
    }

    private void showClosePositionDialog(Stock stock) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Close Position")
                .setMessage("Are you sure you want to sell all " + stock.getQuantity() +
                        " shares of " + stock.getSymbol() + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    double totalValue = stock.getPrice() * stock.getQuantity();
                    executeSellOrder(stock, stock.getQuantity(), totalValue);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeSellOrder(Stock stock, int quantity, double totalValue) {
        TradeManager.executeSellOrder(stock, quantity, totalValue,
                new TradeManager.TradeCallback() {
                    @Override
                    public void onSuccess(double newBalance) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    quantity == stock.getQuantity() ?
                                            "Position closed successfully" :
                                            "Stocks sold successfully",
                                    Toast.LENGTH_SHORT).show();
                        }
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateBalance(newBalance);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + error,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUI(int openPositions, double totalInvestment) {
        tvOpenPositions.setText(String.valueOf(openPositions));
        tvTotalInvestment.setText(String.format("$%.2f", totalInvestment));
    }

    private void updateTotalProfitLoss() {
        double total = 0;
        for (Stock stock : stockList) {
            total += (stock.getPrice() - stock.getPreviousClose()) * stock.getQuantity();
        }

        String profitLossText = String.format("Total P/L: $%.2f", total);
        totalProfitLoss.setText(profitLossText);
        totalProfitLoss.setTextColor(requireContext().getColor(total >= 0 ? R.color.green : R.color.red));
    }

    private void handleError(int openPositions, double totalInvestment) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                updateUI(openPositions, totalInvestment);
                hideLoading();
                if (stockList.isEmpty()) {
                    showError("Error loading data");
                }
            });
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
            tvError.setVisibility(stockList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (portfolioListener != null) {
            portfolioRef.removeEventListener(portfolioListener);
        }
    }
}