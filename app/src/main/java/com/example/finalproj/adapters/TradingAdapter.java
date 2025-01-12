package com.example.finalproj.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproj.R;
import com.example.finalproj.model.Stock;

import java.util.List;

public class TradingAdapter extends RecyclerView.Adapter<TradingAdapter.ViewHolder> {

    private final List<Stock> stockList;

    public TradingAdapter(List<Stock> stockList) {
        this.stockList = stockList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_trading, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stockList.get(position);

        holder.stockName.setText(stock.getName());
        holder.stockSymbol.setText(stock.getSymbol());
        holder.buyPrice.setText(String.format("Buy: $%.2f", stock.getPreviousClose()));
        holder.sellPrice.setText(String.format("Sell: $%.2f", stock.getPrice()));

        // Calculate Profit/Loss
        double profitLoss = (stock.getPrice() - stock.getPreviousClose()) * stock.getQuantity();
        holder.profitLoss.setText(String.format("Profit/Loss: $%.2f", profitLoss));
        holder.profitLoss.setTextColor(profitLoss >= 0 ?
                holder.itemView.getContext().getColor(R.color.green) :
                holder.itemView.getContext().getColor(R.color.red));

        // Handle actions (e.g., close or view details)
        holder.closeButton.setOnClickListener(v -> {
            // Handle close button logic
        });

        holder.detailsButton.setOnClickListener(v -> {
            // Handle view details logic
        });
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stockName, stockSymbol, buyPrice, sellPrice, profitLoss;
        ImageView stockLogo;
        Button closeButton, detailsButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stockName = itemView.findViewById(R.id.stock_name);
            stockSymbol = itemView.findViewById(R.id.stock_symbol);
            buyPrice = itemView.findViewById(R.id.buy_price);
            sellPrice = itemView.findViewById(R.id.sell_price);
            profitLoss = itemView.findViewById(R.id.profit_loss);
            stockLogo = itemView.findViewById(R.id.stock_logo);
            closeButton = itemView.findViewById(R.id.close_button);
            detailsButton = itemView.findViewById(R.id.details_button);
        }
    }
}
