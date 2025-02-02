package com.example.finalproj.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.finalproj.R;
import com.example.finalproj.utils.ApiManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final long UPDATE_INTERVAL = 8 * 60 * 60 * 1000; // 8 hours in milliseconds

    private NavController navController;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private MenuItem balanceMenuItem;
    private MenuItem totalValueMenuItem;
    private AppBarConfiguration appBarConfiguration;
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.topAppBar);
        bottomNav = findViewById(R.id.bottom_navigation);

        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_portfolio, R.id.nav_trading, R.id.nav_watchlist,
                R.id.nav_transactions, R.id.nav_options)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(bottomNav, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment ||
                    destination.getId() == R.id.registerFragment) {
                hideNavigationElements();
            } else {
                showNavigationElements();
                if (isUserLoggedIn()) {
                    updateBalanceAndTotal();
                }
            }
        });

        if (isUserLoggedIn()) {
            startPeriodicUpdates();
        }
    }

    private void startPeriodicUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isUserLoggedIn()) {
                    Log.d(TAG, "Performing periodic update");
                    updateBalanceAndTotal();
                }
                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        updateHandler.post(updateRunnable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_navigation_menu, menu);
        balanceMenuItem = menu.findItem(R.id.menu_balance);
        totalValueMenuItem = menu.findItem(R.id.menu_total_value);
        if (isUserLoggedIn()) {
            updateBalanceAndTotal();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateBalanceAndTotal() {
        if (balanceMenuItem != null && totalValueMenuItem != null && isUserLoggedIn()) {
            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId);

            userRef.child("balance").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        double balance = snapshot.getValue(Double.class);
                        String formattedBalance = String.format("Available: $%.2f", balance);
                        balanceMenuItem.setTitle(formattedBalance);
                        calculateTotalValue(balance);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to read balance", error.toException());
                }
            });
        }
    }

    private void calculateTotalValue(double availableBalance) {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference portfolioRef = FirebaseDatabase.getInstance().getReference()
                .child("portfolios").child(userId);

        portfolioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final double[] investedValue = {0};
                final double[] currentValue = {0};
                final AtomicInteger totalStocks = new AtomicInteger(0);
                final AtomicInteger pendingRequests = new AtomicInteger(0);

                for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                    Integer quantity = stockSnapshot.child("quantity").getValue(Integer.class);
                    Double purchasePrice = stockSnapshot.child("lastPrice").getValue(Double.class);
                    String symbol = stockSnapshot.child("symbol").getValue(String.class);

                    if (quantity != null && quantity > 0 && purchasePrice != null && symbol != null) {
                        investedValue[0] += (purchasePrice * quantity);
                        totalStocks.incrementAndGet();
                        pendingRequests.incrementAndGet();

                        ApiManager.getStockQuotes(MainActivity.this, symbol, new ApiManager.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                try {
                                    JSONObject quote = response.getJSONObject("Global Quote");
                                    double currentPrice = Double.parseDouble(quote.getString("05. price"));
                                    currentValue[0] += (currentPrice * quantity);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing stock price", e);
                                }

                                if (pendingRequests.decrementAndGet() == 0) {
                                    // All requests completed
                                    double totalValue = availableBalance + currentValue[0];
                                    String formattedTotal = String.format("Total: $%.2f", totalValue);
                                    runOnUiThread(() -> totalValueMenuItem.setTitle(formattedTotal));
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e(TAG, "Failed to get stock price: " + errorMessage);
                                if (pendingRequests.decrementAndGet() == 0) {
                                    // Use invested value if current value cannot be fetched
                                    double totalValue = availableBalance + investedValue[0];
                                    String formattedTotal = String.format("Total: $%.2f", totalValue);
                                    runOnUiThread(() -> totalValueMenuItem.setTitle(formattedTotal));
                                }
                            }
                        });
                    }
                }

                if (totalStocks.get() == 0) {
                    String formattedTotal = String.format("Total: $%.2f", availableBalance);
                    totalValueMenuItem.setTitle(formattedTotal);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read portfolio", error.toException());
            }
        });
    }

    private void hideNavigationElements() {
        toolbar.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);
    }

    private void showNavigationElements() {
        toolbar.setVisibility(View.VISIBLE);
        bottomNav.setVisibility(View.VISIBLE);
    }

    public void updateBalance(double newBalance) {
        if (balanceMenuItem != null) {
            String formattedBalance = String.format("Available: $%.2f", newBalance);
            balanceMenuItem.setTitle(formattedBalance);
            calculateTotalValue(newBalance);
        }
    }

    private void logout() {
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
        mAuth.signOut();
        navController.navigate(R.id.loginFragment);
    }

    private boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
}