package com.example.finalproj.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproj.R;
import com.example.finalproj.model.Transaction;

import java.util.List;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private final List<Transaction> transactionList;

    public TransactionsAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        // Bind data to views
        holder.transactionDate.setText("Date: " + transaction.getDate());
        holder.transactionSymbol.setText("Symbol: " + transaction.getSymbol());
        holder.transactionPrice.setText(String.format("Price: $%.2f", transaction.getPrice()));
        holder.transactionQuantity.setText(String.format("Quantity: %d", transaction.getQuantity()));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView transactionDate, transactionSymbol, transactionPrice, transactionQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            transactionDate = itemView.findViewById(R.id.transaction_date);
            transactionSymbol = itemView.findViewById(R.id.transaction_symbol);
            transactionPrice = itemView.findViewById(R.id.transaction_price);
            transactionQuantity = itemView.findViewById(R.id.transaction_quantity);
        }
    }
}
