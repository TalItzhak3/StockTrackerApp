package com.example.finalproj.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproj.R;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.ApiManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.ViewHolder> {
    private final List<Stock> stockList;
    private final Context context;

    public interface StockActionListener {
        void onBuyClicked(Stock stock);
        void onSellClicked(Stock stock);
    }

    private final StockActionListener actionListener;

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

        // Set basic stock info
        holder.stockName.setText(stock.getName());
        holder.stockSymbol.setText(stock.getSymbol());
        holder.stockPrice.setText(String.format("$%.2f", stock.getPrice()));

        // Set price change color and text
        double changePercent = stock.getChangePercent();
        holder.stockPriceChange.setText(String.format("%.2f%%", changePercent));
        holder.stockPriceChange.setTextColor(changePercent >= 0 ?
                Color.GREEN : Color.RED);

        // Load stock logo
        Glide.with(context)
                .load(stock.getLogoUrl())
                .placeholder(R.drawable.placeholder_logo)
                .error(R.drawable.placeholder_logo)
                .into(holder.stockLogo);

        // Set button click listeners
        holder.buyButton.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onBuyClicked(stock);
        });

        holder.sellButton.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onSellClicked(stock);
        });

        holder.viewGraphButton.setOnClickListener(v -> {
            boolean isVisible = holder.graphContainer.getVisibility() == View.VISIBLE;
            holder.graphContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            if (!isVisible) {
                loadGraphData(stock, holder, "day"); // Default to daily view
            }
        });

        // Set time period button listeners
        holder.btn1D.setOnClickListener(v -> loadGraphData(stock, holder, "day"));
        holder.btn1W.setOnClickListener(v -> loadGraphData(stock, holder, "week"));
        holder.btn1M.setOnClickListener(v -> loadGraphData(stock, holder, "month"));
        holder.btn3M.setOnClickListener(v -> loadGraphData(stock, holder, "quarter"));
        holder.btn1Y.setOnClickListener(v -> loadGraphData(stock, holder, "year"));
    }

    private void loadGraphData(Stock stock, ViewHolder holder, String timespan) {
        holder.chartProgress.setVisibility(View.VISIBLE);
        holder.priceChart.setVisibility(View.GONE);

        ApiManager.getHistoricalData(stock.getSymbol(), timespan, new ApiManager.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray results = response.getJSONArray("results");
                    List<Entry> entries = new ArrayList<>();

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject bar = results.getJSONObject(i);
                        float closePrice = (float) bar.getDouble("c");
                        entries.add(new Entry(i, closePrice));
                    }

                    LineDataSet dataSet = new LineDataSet(entries, stock.getSymbol() + " Price");
                    styleDataSet(dataSet);

                    LineData lineData = new LineData(dataSet);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        holder.priceChart.setData(lineData);
                        holder.priceChart.invalidate();
                        holder.chartProgress.setVisibility(View.GONE);
                        holder.priceChart.setVisibility(View.VISIBLE);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    showChartError(holder);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                showChartError(holder);
            }
        });
    }

    private void styleDataSet(LineDataSet dataSet) {
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#300000FF")); // Semi-transparent blue
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
    }

    private void showChartError(ViewHolder holder) {
        new Handler(Looper.getMainLooper()).post(() -> {
            holder.chartProgress.setVisibility(View.GONE);
            holder.priceChart.setVisibility(View.VISIBLE);
            holder.priceChart.setNoDataText("Error loading chart data");
        });
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView stockLogo;
        TextView stockName, stockSymbol, stockPrice, stockPriceChange;
        Button buyButton, sellButton, viewGraphButton;
        View graphContainer;
        LineChart priceChart;
        ProgressBar chartProgress;
        MaterialButton btn1D, btn1W, btn1M, btn3M, btn1Y;

        ViewHolder(View view) {
            super(view);
            // Initialize views
            stockLogo = view.findViewById(R.id.stock_logo);
            stockName = view.findViewById(R.id.stock_name);
            stockSymbol = view.findViewById(R.id.stock_symbol);
            stockPrice = view.findViewById(R.id.stock_price);
            stockPriceChange = view.findViewById(R.id.stock_price_change);
            buyButton = view.findViewById(R.id.buy_button);
            sellButton = view.findViewById(R.id.sell_button);
            viewGraphButton = view.findViewById(R.id.view_graph_button);
            graphContainer = view.findViewById(R.id.graphContainer);
            priceChart = view.findViewById(R.id.priceChart);
            chartProgress = view.findViewById(R.id.chartProgress);

            // Initialize time period buttons
            btn1D = view.findViewById(R.id.btn1D);
            btn1W = view.findViewById(R.id.btn1W);
            btn1M = view.findViewById(R.id.btn1M);
            btn3M = view.findViewById(R.id.btn3M);
            btn1Y = view.findViewById(R.id.btn1Y);

            // Configure chart
            setupChart();
        }

        private void setupChart() {
            priceChart.getDescription().setEnabled(false);
            priceChart.setTouchEnabled(true);
            priceChart.setDragEnabled(true);
            priceChart.setScaleEnabled(true);
            priceChart.setPinchZoom(true);
            priceChart.setDrawGridBackground(false);

            // Customize X-axis
            XAxis xAxis = priceChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);

            // Customize Y-axis
            YAxis leftAxis = priceChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setDrawZeroLine(true);

            // Disable right Y-axis
            priceChart.getAxisRight().setEnabled(false);

            // Customize legend
            priceChart.getLegend().setEnabled(false);

            // Set default text when no data is available
            priceChart.setNoDataText("No chart data available");
        }
    }
}