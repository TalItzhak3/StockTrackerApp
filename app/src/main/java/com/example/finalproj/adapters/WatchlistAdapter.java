package com.example.finalproj.adapters;

import android.content.Context;
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

import com.example.finalproj.R;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.ApiManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
                .inflate(R.layout.item_card_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stockList.get(position);

        holder.stockName.setText(stock.getName());
        holder.stockSymbol.setText(stock.getSymbol());
        holder.stockPrice.setText(String.format("Current Price: $%.2f", stock.getPrice()));

        double changePercent = stock.getChangePercent();
        holder.stockPriceChange.setText(String.format("%.2f%%", changePercent));
        holder.stockPriceChange.setTextColor(context.getColor(
                changePercent >= 0 ? R.color.green : R.color.red));

        holder.stockLogo.setImageResource(stock.getLogoResource());

        holder.buyButton.setVisibility(View.GONE);
        holder.sellButton.setVisibility(View.GONE);
        holder.holdingsContainer.setVisibility(View.GONE);

        holder.favoriteButton.setText("Remove");
        holder.favoriteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClicked(stock);
            }
        });

        holder.viewGraphButton.setOnClickListener(v -> {
            boolean isVisible = holder.graphContainer.getVisibility() == View.VISIBLE;
            holder.graphContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);

            if (!isVisible) {
                loadGraphData(stock, holder, "1D");
            }
        });

        holder.btn1D.setOnClickListener(v -> loadGraphData(stock, holder, "1D"));
        holder.btn1W.setOnClickListener(v -> loadGraphData(stock, holder, "1W"));
        holder.btn1M.setOnClickListener(v -> loadGraphData(stock, holder, "1M"));
        holder.btn3M.setOnClickListener(v -> loadGraphData(stock, holder, "3M"));
        holder.btn1Y.setOnClickListener(v -> loadGraphData(stock, holder, "1Y"));

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
                            List<Entry> entries = processResponseData(response, timespan);
                            if (entries.isEmpty()) {
                                holder.priceChart.post(() -> showChartError(holder));
                                return;
                            }
                            holder.priceChart.post(() -> setupChart(holder, entries,
                                    stock.getSymbol(), timespan));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing data: " + e.getMessage());
                            holder.priceChart.post(() -> showChartError(holder));
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to load data: " + errorMessage);
                        holder.priceChart.post(() -> showChartError(holder));
                    }
                });
    }

    private List<Entry> processResponseData(JSONObject response, String timespan) throws Exception {
        List<Entry> entries = new ArrayList<>();
        String timeSeriesKey = timespan.equals("1D") ?
                "Time Series (5min)" : "Time Series (Daily)";
        JSONObject timeSeries = response.getJSONObject(timeSeriesKey);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"));

        Iterator<String> dates = timeSeries.keys();
        while (dates.hasNext()) {
            String dateStr = dates.next();
            JSONObject dataPoint = timeSeries.getJSONObject(dateStr);
            float closePrice = Float.parseFloat(dataPoint.getString("4. close"));
            long timestamp = dateFormat.parse(dateStr).getTime();
            entries.add(new Entry(timestamp, closePrice));
        }

        Collections.sort(entries, (e1, e2) ->
                Float.compare(e1.getX(), e2.getX()));

        return entries;
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
            xAxis.setValueFormatter(new ValueFormatter() {
                private final SimpleDateFormat formatter = getFormatterForTimespan(timespan);

                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    try {
                        return formatter.format(new Date((long) value));
                    } catch (Exception e) {
                        return "";
                    }
                }
            });
            xAxis.setLabelRotationAngle(45f);
            xAxis.setLabelCount(5, true);

            YAxis leftAxis = holder.priceChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setDrawZeroLine(false);
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
            Log.e(TAG, "Error setting up chart", e);
            showChartError(holder);
        }
    }

    private SimpleDateFormat getFormatterForTimespan(String timespan) {
        SimpleDateFormat formatter;
        switch (timespan) {
            case "1D":
                formatter = new SimpleDateFormat("HH:mm", Locale.US);
                break;
            case "1W":
                formatter = new SimpleDateFormat("EEE", Locale.US);
                break;
            case "1M":
                formatter = new SimpleDateFormat("dd/MM", Locale.US);
                break;
            case "3M":
            case "1Y":
                formatter = new SimpleDateFormat("MM/yy", Locale.US);
                break;
            default:
                formatter = new SimpleDateFormat("HH:mm", Locale.US);
        }
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"));
        return formatter;
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
        View graphContainer, holdingsContainer;
        LineChart priceChart;
        MaterialButton btn1D, btn1W, btn1M, btn3M, btn1Y;
        ProgressBar chartProgress;

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
            holdingsContainer = view.findViewById(R.id.holdingsContainer);

            btn1D = view.findViewById(R.id.btn1D);
            btn1W = view.findViewById(R.id.btn1W);
            btn1M = view.findViewById(R.id.btn1M);
            btn3M = view.findViewById(R.id.btn3M);
            btn1Y = view.findViewById(R.id.btn1Y);
        }
    }
}