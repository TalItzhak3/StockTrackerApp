package com.example.finalproj.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproj.R;
import com.example.finalproj.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private final List<Transaction> transactionList;

    public TransactionsAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.transactionLogo.setImageResource(transaction.getLogoResource());



        // Set company name and symbol
        holder.transactionName.setText(transaction.getStockName());
        holder.transactionSymbol.setText(transaction.getSymbol());

        // Set date and time
        holder.transactionDate.setText("Date: " + transaction.getDate());

        // Set price and quantity
        holder.transactionPrice.setText(String.format(Locale.getDefault(),
                "Price: $%.2f", transaction.getPrice()));
        holder.transactionQuantity.setText(String.format(Locale.getDefault(),
                "Quantity: %d", transaction.getQuantity()));

        // Set total value with color based on transaction type
        String totalValueText = String.format(Locale.getDefault(),
                "Total Value: $%.2f", transaction.getTotalValue());
        holder.transactionValue.setText(totalValueText);

        // Set transaction type with appropriate color
        int colorResId = transaction.getType().equals("buy") ? R.color.green : R.color.red;
        holder.transactionType.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), colorResId));
        holder.transactionType.setText(transaction.getType().toUpperCase());
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView transactionLogo;
        TextView transactionName, transactionSymbol, transactionDate;
        TextView transactionPrice, transactionQuantity, transactionValue, transactionType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionLogo = itemView.findViewById(R.id.transaction_logo);
            transactionName = itemView.findViewById(R.id.transaction_name);
            transactionSymbol = itemView.findViewById(R.id.transaction_symbol);
            transactionDate = itemView.findViewById(R.id.transaction_date);
            transactionPrice = itemView.findViewById(R.id.transaction_price);
            transactionQuantity = itemView.findViewById(R.id.transaction_quantity);
            transactionValue = itemView.findViewById(R.id.transaction_value);
            transactionType = itemView.findViewById(R.id.transaction_type);
        }
    }
}