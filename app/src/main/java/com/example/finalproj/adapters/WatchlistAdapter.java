package com.example.finalproj.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.ViewHolder> {
    private static final String TAG = "WatchlistAdapter";
    private final List<Stock> stockList;
    private final Context context;
    private final WatchlistListener listener;

    public interface WatchlistListener {
        void onRemoveClicked(Stock stock);
    }

    public WatchlistAdapter(Context context, List<Stock> stockList, WatchlistListener listener) {
        this.context = context;
        this.stockList = stockList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_watchlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stockList.get(position);

        // Set stock details
        holder.stockName.setText(stock.getName());
        holder.stockSymbol.setText(stock.getSymbol());
        holder.stockPrice.setText(String.format("Current Price: $%.2f", stock.getPrice()));

        // Set price change
        double changePercent = stock.getChangePercent();
        holder.stockPriceChange.setText(String.format("%.2f%%", changePercent));
        holder.stockPriceChange.setTextColor(changePercent >= 0 ?
                context.getColor(R.color.green) : context.getColor(R.color.red));


        // Set button listeners
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClicked(stock);
            }
        });

        // View graph button listener
        holder.viewGraphButton.setOnClickListener(v -> {
            boolean isVisible = holder.graphContainer.getVisibility() == View.VISIBLE;
            holder.graphContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);

            if (!isVisible) {
                loadGraphData(stock, holder, "1D");
            }
        });

        // Time period button listeners
        holder.btn1D.setOnClickListener(v -> loadGraphData(stock, holder, "1D"));
        holder.btn1W.setOnClickListener(v -> loadGraphData(stock, holder, "1W"));
        holder.btn1M.setOnClickListener(v -> loadGraphData(stock, holder, "1M"));
        holder.btn3M.setOnClickListener(v -> loadGraphData(stock, holder, "3M"));
        holder.btn1Y.setOnClickListener(v -> loadGraphData(stock, holder, "1Y"));

        // Reset graph container state
        holder.graphContainer.setVisibility(View.GONE);
        holder.priceChart.clear();
    }

    private void loadGraphData(Stock stock, ViewHolder holder, String timespan) {
        holder.chartProgress.setVisibility(View.VISIBLE);
        holder.priceChart.setVisibility(View.GONE);

        ApiManager.getStockTimeSeriesData(context, stock.getSymbol(), timespan,
                new ApiManager.ApiCallback() {
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
                                case "1D": maxDataPoints = 78; break; // Every 5 minutes for 6.5 hours
                                case "1W": maxDataPoints = 7; break; // Daily for a week
                                case "1M": maxDataPoints = 4; break; // Weekly for a month
                                case "3M": maxDataPoints = 3; break; // Monthly for 3 months
                                case "1Y": maxDataPoints = 4; break; // Quarterly for a year
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
                                setupChart(holder, entries, stock.getSymbol(), timespan);
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

    private void setupChart(ViewHolder holder, List<Entry> entries, String symbol, String timespan) {
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

            switch (timespan) {
                case "1D":
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int hour = (9 + (int)value) % 24; // Market opens at 9
                            return String.format("%02d:00", hour);
                        }
                    });
                    xAxis.setLabelCount(6, true);
                    break;

                case "1W":
                    xAxis.setValueFormatter(new ValueFormatter() {
                        private final String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                        @Override
                        public String getFormattedValue(float value) {
                            return days[(int) value % 7];
                        }
                    });
                    xAxis.setLabelCount(7, true);
                    break;

                case "1M":
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            return "Week " + (((int)value) + 1);
                        }
                    });
                    xAxis.setLabelCount(4, true);
                    break;

                case "3M":
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            return "Month " + (((int)value) + 1);
                        }
                    });
                    xAxis.setLabelCount(3, true);
                    break;

                case "1Y":
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            String[] quarters = {"Q1", "Q2", "Q3", "Q4"};
                            return quarters[(int)value % 4];
                        }
                    });
                    xAxis.setLabelCount(4, true);
                    break;
            }

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
        Button removeButton, viewGraphButton;
        View graphContainer;
        LineChart priceChart;
        MaterialButton btn1D, btn1W, btn1M, btn3M, btn1Y;
        ProgressBar chartProgress;

        ViewHolder(@NonNull View view) {
            super(view);
            stockLogo = view.findViewById(R.id.stock_logo);
            stockName = view.findViewById(R.id.stock_name);
            stockSymbol = view.findViewById(R.id.stock_symbol);
            stockPrice = view.findViewById(R.id.stock_price);
            stockPriceChange = view.findViewById(R.id.stock_price_change);
            removeButton = view.findViewById(R.id.remove_from_watchlist_button);
            viewGraphButton = view.findViewById(R.id.view_graph_button);
            graphContainer = view.findViewById(R.id.graphContainer);
            priceChart = view.findViewById(R.id.priceChart);
            chartProgress = view.findViewById(R.id.chartProgress);

            // Time period buttons
            btn1D = view.findViewById(R.id.btn1D);
            btn1W = view.findViewById(R.id.btn1W);
            btn1M = view.findViewById(R.id.btn1M);
            btn3M = view.findViewById(R.id.btn3M);
            btn1Y = view.findViewById(R.id.btn1Y);
        }
    }
}