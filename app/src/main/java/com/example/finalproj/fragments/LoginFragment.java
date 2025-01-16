package com.example.finalproj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finalproj.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView btnNavigateToRegister;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        etUsername = view.findViewById(R.id.etLoginUsername);
        etPassword = view.findViewById(R.id.etLoginPassword);
        btnLogin = view.findViewById(R.id.btnLoginSubmit);
        btnNavigateToRegister = view.findViewById(R.id.btnNavigateToRegister);
        progressBar = view.findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());
        btnNavigateToRegister.setOnClickListener(v -> navigateToRegister());

        return view;
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                String email = userSnapshot.child("email").getValue(String.class);
                                if (email != null) {
                                    authenticateUser(email, password);
                                } else {
                                    handleLoginFailure("Email not found");
                                }
                                return;
                            }
                        } else {
                            handleLoginFailure("Username not found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handleLoginFailure("Database error: " + error.getMessage());
                    }
                });
    }

    private void authenticateUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        navigateToPortfolio();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Authentication failed";
                        handleLoginFailure(errorMessage);
                    }
                });
    }

    private void handleLoginFailure(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        Toast.makeText(getContext(), "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    private void navigateToRegister() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_loginFragment_to_registerFragment);
    }

    private void navigateToPortfolio() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_loginFragment_to_portfolioFragment);
    }
}