package com.example.finalproj.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproj.R;
import com.example.finalproj.adapters.TradingAdapter;
import com.example.finalproj.model.Stock;

import java.util.ArrayList;
import java.util.List;

public class TradingFragment extends Fragment {

    private RecyclerView rvTrading;
    private TradingAdapter adapter;
    private List<Stock> stockList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trading, container, false);

        // Initialize RecyclerView
        rvTrading = view.findViewById(R.id.rvTrading);
        rvTrading.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize stock data
        stockList = new ArrayList<>();
        stockList.add(new Stock("AAPL", "Apple Inc.", 150.0, 10, "01/01/2023", 145.0));
        stockList.add(new Stock("GOOG", "Google LLC", 2800.0, 5, "01/01/2023", 2750.0));
        stockList.add(new Stock("AMZN", "Amazon", 3400.0, 8, "01/01/2023", 3300.0));

        // Set adapter
        adapter = new TradingAdapter(stockList);
        rvTrading.setAdapter(adapter);

        return view;
    }
}
