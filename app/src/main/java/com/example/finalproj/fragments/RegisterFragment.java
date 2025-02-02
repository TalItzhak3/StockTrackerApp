package com.example.finalproj.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finalproj.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {
    private static final String TAG = "RegisterFragment";
    private static final double INITIAL_BALANCE = 300000.00;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    private TextInputLayout tilFirstName, tilLastName, tilUsername, tilEmail, tilPassword, tilVerifyPassword;
    private TextInputEditText etFirstName, etLastName, etUsername, etEmail, etPassword, etVerifyPassword;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        initializeFirebase();
        initializeViews(view);
        setupTextWatchers();
        setupClickListeners();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
    }

    private void initializeViews(View view) {
        tilFirstName = view.findViewById(R.id.tilFirstName);
        tilLastName = view.findViewById(R.id.tilLastName);
        tilUsername = view.findViewById(R.id.tilUsername);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilVerifyPassword = view.findViewById(R.id.tilVerifyPassword);

        etFirstName = view.findViewById(R.id.etRegisterFirstName);
        etLastName = view.findViewById(R.id.etRegisterLastName);
        etUsername = view.findViewById(R.id.etRegisterUsername);
        etEmail = view.findViewById(R.id.etRegisterEmail);
        etPassword = view.findViewById(R.id.etRegisterPassword);
        etVerifyPassword = view.findViewById(R.id.etVerifyPassword);

        btnRegister = view.findViewById(R.id.btnRegisterSubmit);
        tvLoginLink = view.findViewById(R.id.tvLoginLink);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupTextWatchers() {
        etFirstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                tilFirstName.setError(null);
            }
        });

        etLastName.addTextChangedListener(createTextWatcher(tilLastName));
        etUsername.addTextChangedListener(createTextWatcher(tilUsername));
        etEmail.addTextChangedListener(createTextWatcher(tilEmail));
        etPassword.addTextChangedListener(createTextWatcher(tilPassword));
        etVerifyPassword.addTextChangedListener(createTextWatcher(tilVerifyPassword));
    }

    private TextWatcher createTextWatcher(final TextInputLayout til) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                til.setError(null);
            }
        };
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> registerUser());
        tvLoginLink.setOnClickListener(v -> navigateToLogin());
    }

    private void registerUser() {
        if (!validateInputs()) {
            return;
        }

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        showLoading(true);

        databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            showLoading(false);
                            tilUsername.setError("Username already exists");
                            tilUsername.requestFocus();
                        } else {
                            createUser(firstName, lastName, username, email, password);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        handleError(new Exception(databaseError.getMessage()));
                    }
                });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (etFirstName.getText().toString().trim().isEmpty()) {
            tilFirstName.setError("First name is required");
            isValid = false;
        } else {
            tilFirstName.setError(null);
        }

        if (etLastName.getText().toString().trim().isEmpty()) {
            tilLastName.setError("Last name is required");
            isValid = false;
        } else {
            tilLastName.setError(null);
        }

        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            tilUsername.setError("Username is required");
            isValid = false;
        } else if (username.length() < 4) {
            tilUsername.setError("Username must be at least 4 characters");
            isValid = false;
        } else {
            tilUsername.setError(null);
        }

        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email address");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        String password = etPassword.getText().toString();
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be 6 characters or longer");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        String verifyPassword = etVerifyPassword.getText().toString();
        if (verifyPassword.isEmpty()) {
            tilVerifyPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(verifyPassword)) {
            tilVerifyPassword.setError("Passwords do not match");
            isValid = false;
        } else {
            tilVerifyPassword.setError(null);
        }

        return isValid;
    }

    private void createUser(String firstName, String lastName, String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("firstName", firstName);
                        userData.put("lastName", lastName);
                        userData.put("username", username);
                        userData.put("email", email);
                        userData.put("balance", INITIAL_BALANCE);
                        userData.put("createdAt", System.currentTimeMillis());

                        DatabaseReference settingsRef = FirebaseDatabase.getInstance()
                                .getReference("notification_settings")
                                .child(firebaseUser.getUid());
                        Map<String, Object> defaultSettings = new HashMap<>();
                        defaultSettings.put("price_alerts", true);
                        defaultSettings.put("transaction_alerts", true);
                        defaultSettings.put("watchlist_alerts", true);
                        defaultSettings.put("price_threshold", "5.0");
                        settingsRef.setValue(defaultSettings);

                        databaseReference.child(firebaseUser.getUid())
                                .setValue(userData)
                                .addOnSuccessListener(aVoid -> {
                                    showLoading(false);
                                    showSuccessMessage("Registration successful!");
                                    navigateToLogin();
                                })
                                .addOnFailureListener(this::handleError);
                    }
                })
                .addOnFailureListener(e -> {
                    if (e.getMessage().contains("email")) {
                        tilEmail.setError("Email already in use");
                        tilEmail.requestFocus();
                    } else {
                        handleError(e);
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        setInputsEnabled(!isLoading);
    }

    private void setInputsEnabled(boolean enabled) {
        etFirstName.setEnabled(enabled);
        etLastName.setEnabled(enabled);
        etUsername.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        etVerifyPassword.setEnabled(enabled);
        tvLoginLink.setEnabled(enabled);
    }

    private void handleError(Exception e) {
        showLoading(false);
        Log.e(TAG, "Registration error: " + e.getMessage());
        showErrorMessage(e.getMessage());
    }

    private void showSuccessMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showErrorMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToLogin() {
        if (getView() != null) {
            Navigation.findNavController(getView())
                    .navigate(R.id.action_registerFragment_to_loginFragment);
        }
    }
}
