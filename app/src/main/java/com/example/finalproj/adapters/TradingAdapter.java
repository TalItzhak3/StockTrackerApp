package com.example.finalproj.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproj.R;
import com.example.finalproj.activities.MainActivity;
import com.example.finalproj.model.Stock;
import com.example.finalproj.utils.TradeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.bumptech.glide.Glide;

import java.util.List;

public class TradingAdapter extends RecyclerView.Adapter<TradingAdapter.ViewHolder> {
    private final List<Stock> stockList;
    private final OnTradeActionListener listener;

    public interface OnTradeActionListener {
        void onClosePosition(Stock stock);
        void onSellPart(Stock stock, int quantity, double value);
    }

    public TradingAdapter(List<Stock> stockList, OnTradeActionListener listener) {
        this.stockList = stockList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_trading, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Stock stock = stockList.get(position);
        Context context = holder.itemView.getContext();

        // Set basic info
        holder.stockName.setText(stock.getName());
        holder.stockSymbol.setText(stock.getSymbol());

        holder.stockLogo.setImageResource(stock.getLogoResource());

        // Set prices
        holder.buyPrice.setText(String.format("Buy: $%.2f", stock.getPreviousClose()));
        holder.sellPrice.setText(String.format("Sell: $%.2f", stock.getPrice()));

        double profitLoss = (stock.getPrice() - stock.getPreviousClose()) * stock.getQuantity();
        holder.profitLoss.setText(String.format("$%.2f", profitLoss));
        holder.profitLoss.setTextColor(context.getColor(profitLoss >= 0 ? R.color.green : R.color.red));

        holder.quantity.setText(String.format("Quantity: %d", stock.getQuantity()));

        double totalValue = stock.getPrice() * stock.getQuantity();
        holder.totalValue.setText(String.format("Total Value: $%.2f", totalValue));

        holder.closeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClosePosition(stock);
            }
        });

        holder.detailsButton.setText("Sell Part");
        holder.detailsButton.setOnClickListener(v -> {
            showSellPartDialog(context, stock);
        });
    }

    private void showSellPartDialog(Context context, Stock stock) {
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_trade_stock, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Sell " + stock.getSymbol())
                .setView(dialogView)
                .create();

        ImageView stockLogo = dialogView.findViewById(R.id.dialogStockLogo);
        TextView stockName = dialogView.findViewById(R.id.dialogStockName);
        TextView stockPrice = dialogView.findViewById(R.id.dialogStockPrice);
        TextView currentBalance = dialogView.findViewById(R.id.dialogCurrentBalance);
        TextInputEditText amountInput = dialogView.findViewById(R.id.dialogAmountInput);
        TextView stocksQuantity = dialogView.findViewById(R.id.dialogStocksQuantity);
        TextView totalCost = dialogView.findViewById(R.id.dialogTotalCost);
        Button confirmButton = dialogView.findViewById(R.id.dialogConfirmButton);
        Button cancelButton = dialogView.findViewById(R.id.dialogCancelButton);



        currentBalance.setText(String.format("Current Holdings: %d stocks", stock.getQuantity()));

        // Calculate total cost as user types
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s.length() > 0) {
                        double sellAmount = Double.parseDouble(s.toString());
                        int quantity = (int) (sellAmount / stock.getPrice());
                        double actualValue = quantity * stock.getPrice();

                        if (quantity > stock.getQuantity()) {
                            amountInput.setError("Cannot sell more than you own");
                            confirmButton.setEnabled(false);
                        } else {
                            stocksQuantity.setText(String.format("Number of stocks: %d", quantity));
                            totalCost.setText(String.format("Total Value: $%.2f", actualValue));
                            confirmButton.setEnabled(quantity > 0);
                        }
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

        confirmButton.setOnClickListener(v -> {
            try {
                double sellAmount = Double.parseDouble(amountInput.getText().toString());
                int quantity = (int) (sellAmount / stock.getPrice());
                double actualValue = quantity * stock.getPrice();

                if (quantity <= 0 || quantity > stock.getQuantity()) {
                    Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (listener != null) {
                    listener.onSellPart(stock, quantity, actualValue);
                }
                dialog.dismiss();

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView stockLogo;
        TextView stockName, stockSymbol, buyPrice, sellPrice, profitLoss;
        TextView quantity, totalValue;
        Button closeButton, detailsButton;

        ViewHolder(View view) {
            super(view);
            stockLogo = view.findViewById(R.id.stock_logo);
            stockName = view.findViewById(R.id.stock_name);
            stockSymbol = view.findViewById(R.id.stock_symbol);
            buyPrice = view.findViewById(R.id.buy_price);
            sellPrice = view.findViewById(R.id.sell_price);
            profitLoss = view.findViewById(R.id.profit_loss);
            quantity = view.findViewById(R.id.quantity);
            totalValue = view.findViewById(R.id.total_value);
            closeButton = view.findViewById(R.id.close_button);
            detailsButton = view.findViewById(R.id.details_button);
        }
    }
}