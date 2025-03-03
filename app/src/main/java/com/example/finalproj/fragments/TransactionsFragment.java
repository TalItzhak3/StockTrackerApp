package com.example.finalproj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finalproj.R;
import com.example.finalproj.adapters.TransactionsAdapter;
import com.example.finalproj.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {
    private static final String TAG = "TransactionsFragment";

    private RecyclerView recyclerView;
    private TransactionsAdapter adapter;
    private List<Transaction> transactionList;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvError;
    private DatabaseReference transactionsRef;
    private SimpleDateFormat dateFormat;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);
        dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        initializeViews(view);
        setupRecyclerView();
        loadTransactions();
        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.rvTransactions);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvError = view.findViewById(R.id.tvError);

        swipeRefreshLayout.setOnRefreshListener(this::loadTransactions);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        transactionsRef = FirebaseDatabase.getInstance()
                .getReference("transactions")
                .child(userId);
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        adapter = new TransactionsAdapter(transactionList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadTransactions() {
        showLoading();
        transactionList.clear();
        adapter.notifyDataSetChanged();

        Query query = transactionsRef.orderByChild("date");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    hideLoading();
                    showError("No transactions found");
                    return;
                }

                transactionList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Transaction transaction = snapshot.getValue(Transaction.class);
                    if (transaction != null) {
                        transactionList.add(transaction);
                    }
                }

                Collections.sort(transactionList, new Comparator<Transaction>() {
                    @Override
                    public int compare(Transaction t1, Transaction t2) {
                        try {
                            Date date1 = dateFormat.parse(t1.getDate());
                            Date date2 = dateFormat.parse(t2.getDate());
                            return date2.compareTo(date1); // Reverse order for newest first
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date: " + e.getMessage());
                            return 0;
                        }
                    }
                });

                adapter.notifyDataSetChanged();
                hideLoading();

                if (transactionList.isEmpty()) {
                    showError("No transactions available");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading transactions: " + databaseError.getMessage());
                hideLoading();
                showError("Error loading transactions");
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        if (isAdded()) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }
}