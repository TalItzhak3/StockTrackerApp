package com.example.finalproj.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproj.R;
import com.example.finalproj.model.Stock;

import java.util.List;

public class WatchlistAdapter extends RecyclerView.Adapter<WatchlistAdapter.ViewHolder> {

    private final List<Stock> stockList;

    public WatchlistAdapter(List<Stock> stockList) {
        this.stockList = stockList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_watchlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stockList.get(position);
        holder.stockName.setText(stock.getName());
        holder.stockSymbol.setText(stock.getSymbol());
        holder.stockPrice.setText(String.format("Current Price: $%.2f", stock.getPrice()));
        holder.stockPriceChange.setText(String.format("Price Change: %.2f%%", stock.getChangePercent()));

        // Set listeners for buttons
        holder.removeButton.setOnClickListener(v -> {
            // Example: Remove stock from list
            stockList.remove(position);
            notifyItemRemoved(position);
        });

        holder.infoButton.setOnClickListener(v -> {
            // Example: Show stock info (implement logic)
        });

        holder.graphButton.setOnClickListener(v -> {
            // Example: Show graph (implement logic)
        });
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stockName, stockSymbol, stockPrice, stockPriceChange;
        Button removeButton, infoButton, graphButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stockName = itemView.findViewById(R.id.stock_name);
            stockSymbol = itemView.findViewById(R.id.stock_symbol);
            stockPrice = itemView.findViewById(R.id.stock_price);
            stockPriceChange = itemView.findViewById(R.id.stock_price_change);
            removeButton = itemView.findViewById(R.id.remove_from_watchlist_button);
            infoButton = itemView.findViewById(R.id.stock_info_button);
            graphButton = itemView.findViewById(R.id.view_graph_button);
        }
    }
}
