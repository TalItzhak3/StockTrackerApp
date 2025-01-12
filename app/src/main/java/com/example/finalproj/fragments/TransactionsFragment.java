package com.example.finalproj.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.finalproj.R;
import com.example.finalproj.adapters.TransactionsAdapter;
import com.example.finalproj.model.Transaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransactionsFragment extends Fragment {

    public TransactionsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.rvTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create a list of transactions (replace this with your actual data source)
        List<Transaction> transactionList = new ArrayList<>();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            transactionList.add(new Transaction("01/01/2025", "AAPL", 150.0, 10, dateFormat.parse("01/01/2025")));
            transactionList.add(new Transaction("02/01/2025", "GOOG", 2800.0, 5, dateFormat.parse("02/01/2025")));
            transactionList.add(new Transaction("03/01/2025", "AMZN", 3400.0, 2, dateFormat.parse("03/01/2025")));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Set the adapter
        TransactionsAdapter adapter = new TransactionsAdapter(transactionList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
