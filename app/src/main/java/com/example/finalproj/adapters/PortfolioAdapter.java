package com.example.finalproj.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproj.R;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.ApiManager;
import com.example.finalproj.utils.TradeManager;
import com.example.finalproj.utils.UserManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.ViewHolder> {
    private static final String TAG = "PortfolioAdapter";
    private final List<Stock> stockList;
    private final Context context;
    private final StockActionListener actionListener;

    public interface StockActionListener {
        void onBuyClicked(Stock stock);
        void onSellClicked(Stock stock);
        void onFavoriteClicked(Stock stock);
        void onBalanceUpdated(double newBalance);
    }

    public PortfolioAdapter(Context context, List<Stock> stockList, StockActionListener listener) {
        this.context = context;
        this.stockList = stockList;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stockList.get(position);

        // Set stock details
        holder.stockName.setText(stock.getName());
        holder.stockSymbol.setText(stock.getSymbol());
        holder.stockPrice.setText(String.format("Current Price: ₪%.2f", stock.getPrice()));

        // Set price change
        double changePercent = stock.getChangePercent();
        holder.stockPriceChange.setText(String.format("%.2f%%", changePercent));
        holder.stockPriceChange.setTextColor(changePercent >= 0 ?
                context.getColor(R.color.green) : context.getColor(R.color.red));

        // Load stock logo
        Glide.with(context)
                .load(stock.getLogoUrl())
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .into(holder.stockLogo);

        // Set button listeners
        holder.buyButton.setOnClickListener(v -> showTradeDialog(stock, true));
        holder.sellButton.setOnClickListener(v -> showTradeDialog(stock, false));

        holder.favoriteButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onFavoriteClicked(stock);
            }
        });

        // Graph functionality
        holder.viewGraphButton.setOnClickListener(v -> {
            boolean isVisible = holder.graphContainer.getVisibility() == View.VISIBLE;
            holder.graphContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);

            if (!isVisible) {
                loadGraphData(stock, holder, "1D");
            }
        });

        // Time period buttons
        holder.btn1D.setOnClickListener(v -> loadGraphData(stock, holder, "1D"));
        holder.btn1W.setOnClickListener(v -> loadGraphData(stock, holder, "1W"));
        holder.btn1M.setOnClickListener(v -> loadGraphData(stock, holder, "1M"));
        holder.btn3M.setOnClickListener(v -> loadGraphData(stock, holder, "3M"));
        holder.btn1Y.setOnClickListener(v -> loadGraphData(stock, holder, "1Y"));

        // Reset graph container state
        holder.graphContainer.setVisibility(View.GONE);
        holder.priceChart.clear();
    }

    private void showTradeDialog(Stock stock, boolean isBuying) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_trade_stock, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(isBuying ? "Buy " + stock.getSymbol() : "Sell " + stock.getSymbol())
                .setView(dialogView)
                .create();

        // Initialize dialog views
        ImageView stockLogo = dialogView.findViewById(R.id.dialogStockLogo);
        TextView stockName = dialogView.findViewById(R.id.dialogStockName);
        TextView stockPrice = dialogView.findViewById(R.id.dialogStockPrice);
        TextView currentBalance = dialogView.findViewById(R.id.dialogCurrentBalance);
        TextInputEditText amountInput = dialogView.findViewById(R.id.dialogAmountInput);
        TextView stocksQuantity = dialogView.findViewById(R.id.dialogStocksQuantity);
        TextView totalCost = dialogView.findViewById(R.id.dialogTotalCost);
        Button confirmButton = dialogView.findViewById(R.id.dialogConfirmButton);
        Button cancelButton = dialogView.findViewById(R.id.dialogCancelButton);

        // Set initial values
        stockName.setText(stock.getName());
        stockPrice.setText(String.format("Current Price: ₪%.2f", stock.getPrice()));
        Glide.with(context)
                .load(stock.getLogoUrl())
                .placeholder(R.drawable.logo)
                .into(stockLogo);

        confirmButton.setText(isBuying ? "Buy" : "Sell");
        confirmButton.setBackgroundTintList(ColorStateList.valueOf(
                context.getColor(isBuying ? R.color.green : R.color.red)));

        // Get and display current balance
        UserManager.getUserBalance(new UserManager.BalanceCallback() {
            @Override
            public void onBalanceReceived(double balance) {
                currentBalance.setText(String.format("Available Balance: ₪%.2f", balance));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Error loading balance: " + error, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Calculate quantity and total as user types
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s.length() > 0) {
                        double amount = Double.parseDouble(s.toString());
                        int quantity = (int) (amount / stock.getPrice());
                        double actualCost = quantity * stock.getPrice();

                        stocksQuantity.setText(String.format("Number of stocks: %d", quantity));
                        totalCost.setText(String.format("Total %s: ₪%.2f",
                                isBuying ? "Cost" : "Value", actualCost));

                        confirmButton.setEnabled(quantity > 0);
                    } else {
                        stocksQuantity.setText("");
                        totalCost.setText("");
                        confirmButton.setEnabled(false);
                    }
                } catch (NumberFormatException e) {
                    stocksQuantity.setText("");
                    totalCost.setText("");
                    confirmButton.setEnabled(false);
                }
            }
        });

        // Handle button clicks
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            try {
                double amount = Double.parseDouble(amountInput.getText().toString());
                int quantity = (int) (amount / stock.getPrice());
                double actualCost = quantity * stock.getPrice();

                if (isBuying) {
                    TradeManager.executeBuyOrder(stock, quantity, actualCost, new TradeManager.TradeCallback() {
                        @Override
                        public void onSuccess(double newBalance) {
                            dialog.dismiss();
                            Toast.makeText(context,
                                    "Successfully bought " + quantity + " stocks",
                                    Toast.LENGTH_SHORT).show();

                            if (actionListener != null) {
                                actionListener.onBalanceUpdated(newBalance);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    TradeManager.executeSellOrder(stock, quantity, actualCost, new TradeManager.TradeCallback() {
                        @Override
                        public void onSuccess(double newBalance) {
                            dialog.dismiss();
                            Toast.makeText(context,
                                    "Successfully sold " + quantity + " stocks",
                                    Toast.LENGTH_SHORT).show();

                            if (actionListener != null) {
                                actionListener.onBalanceUpdated(newBalance);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void loadGraphData(Stock stock, ViewHolder holder, String timespan) {
        holder.chartProgress.setVisibility(View.VISIBLE);
        holder.priceChart.setVisibility(View.GONE);

        ApiManager.getStockTimeSeriesData(context, stock.getSymbol(), timespan, new ApiManager.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    List<Entry> entries = new ArrayList<>();

                    String timeSeriesKey = timespan.equals("1D") ?
                            "Time Series (5min)" : "Time Series (Daily)";

                    JSONObject timeSeries = response.getJSONObject(timeSeriesKey);
                    Iterator<String> dateKeys = timeSeries.keys();

                    List<String> dateList = new ArrayList<>();
                    while (dateKeys.hasNext()) {
                        dateList.add(dateKeys.next());
                    }
                    Collections.sort(dateList);

                    int maxDataPoints;
                    switch (timespan) {
                        case "1D": maxDataPoints = 78; break;
                        case "1W": maxDataPoints = 5; break;
                        case "1M": maxDataPoints = 21; break;
                        case "3M": maxDataPoints = 63; break;
                        case "1Y": maxDataPoints = 252; break;
                        default: maxDataPoints = 78;
                    }

                    int startIndex = Math.max(0, dateList.size() - maxDataPoints);
                    for (int i = startIndex; i < dateList.size(); i++) {
                        String date = dateList.get(i);
                        JSONObject dataPoint = timeSeries.getJSONObject(date);
                        float price = Float.parseFloat(dataPoint.getString("4. close"));
                        entries.add(new Entry(i - startIndex, price));
                    }

                    if (entries.isEmpty()) {
                        holder.priceChart.post(() -> showChartError(holder));
                        return;
                    }

                    holder.priceChart.post(() -> {
                        setupChart(holder, entries, stock.getSymbol());
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing data", e);
                    holder.priceChart.post(() -> showChartError(holder));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "API error: " + errorMessage);
                holder.priceChart.post(() -> showChartError(holder));
            }
        });
    }

    private void setupChart(ViewHolder holder, List<Entry> entries, String symbol) {
        try {
            holder.priceChart.getDescription().setEnabled(false);
            holder.priceChart.setTouchEnabled(true);
            holder.priceChart.setDragEnabled(true);
            holder.priceChart.setScaleEnabled(true);
            holder.priceChart.setPinchZoom(false);
            holder.priceChart.setDrawGridBackground(false);

            XAxis xAxis = holder.priceChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setLabelCount(5, true);

            YAxis leftAxis = holder.priceChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setDrawZeroLine(false);
            leftAxis.setLabelCount(6, true);

            holder.priceChart.getAxisRight().setEnabled(false);

            LineDataSet dataSet = new LineDataSet(entries, symbol);
            dataSet.setDrawIcons(false);
            dataSet.setDrawValues(false);
            dataSet.setDrawCircles(false);
            dataSet.setMode(LineDataSet.Mode.LINEAR);
            dataSet.setColor(context.getColor(R.color.purple_500));
            dataSet.setLineWidth(1.5f);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(context.getColor(R.color.purple_200));
            dataSet.setFillAlpha(50);

            LineData lineData = new LineData(dataSet);
            holder.priceChart.setData(lineData);

            holder.priceChart.invalidate();
            holder.chartProgress.setVisibility(View.GONE);
            holder.priceChart.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error creating chart", e);
            showChartError(holder);
        }
    }

    private void showChartError(ViewHolder holder) {
        holder.chartProgress.setVisibility(View.GONE);
        holder.priceChart.setVisibility(View.VISIBLE);
        holder.priceChart.setNoDataText("Error loading chart data");
        holder.priceChart.invalidate();
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView stockLogo;
        TextView stockName, stockSymbol, stockPrice, stockPriceChange;
        Button buyButton, sellButton, viewGraphButton, favoriteButton;
        View graphContainer;
        LineChart priceChart;
        MaterialButton btn1D, btn1W, btn1M, btn3M, btn1Y;
        ProgressBar chartProgress;
        TextView holdingsQuantity, holdingsValue, profitLoss;
        View holdingsContainer;

        ViewHolder(View view) {
            super(view);
            stockLogo = view.findViewById(R.id.stock_logo);
            stockName = view.findViewById(R.id.stock_name);
            stockSymbol = view.findViewById(R.id.stock_symbol);
            stockPrice = view.findViewById(R.id.stock_price);
            stockPriceChange = view.findViewById(R.id.stock_price_change);
            buyButton = view.findViewById(R.id.buy_button);
            sellButton = view.findViewById(R.id.sell_button);
            viewGraphButton = view.findViewById(R.id.view_graph_button);
            favoriteButton = view.findViewById(R.id.favorite_button);
            graphContainer = view.findViewById(R.id.graphContainer);
            priceChart = view.findViewById(R.id.priceChart);
            chartProgress = view.findViewById(R.id.chartProgress);

            // Time period buttons
            btn1D = view.findViewById(R.id.btn1D);
            btn1W = view.findViewById(R.id.btn1W);
            btn1M = view.findViewById(R.id.btn1M);
            btn3M = view.findViewById(R.id.btn3M);
            btn1Y = view.findViewById(R.id.btn1Y);

            // Holdings info
            holdingsContainer = view.findViewById(R.id.holdingsContainer);
            holdingsQuantity = view.findViewById(R.id.holdings_quantity);
            holdingsValue = view.findViewById(R.id.holdings_value);
            profitLoss = view.findViewById(R.id.profit_loss);
        }
    }
}