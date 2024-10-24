package com.demo.donation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignIn extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnSignUp;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private TextView tvForgotPassword;
    private TextView tvSignUp;
    private static final String TAG = "SignIn";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignIn);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
    }

    private void setupListeners() {
        btnSignUp.setOnClickListener(v -> validateAndSignIn());
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        tvSignUp.setOnClickListener(v -> navigateToSignUp());
    }

    private void validateAndSignIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password cannot be empty");
            return;
        }

        // Proceed with sign in
        performSignIn(email, password);
    }

    private void performSignIn(String email, String password) {
        showLoading();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        new Handler().postDelayed(() -> {
            executor.execute(() -> {
                try {
                    // Get database instance
                    AppDataBase db = AppDataBase.getInstance(getApplicationContext());
                    UserDao userDao = db.userDao();

                    // Check if user exists
                    User user = userDao.getUserByEmail(email);

                    handler.post(() -> {
                        hideLoading();

                        if (user == null) {
                            showError("No account found with this email");
                        } else if (!user.getPassword().equals(password)) {
                            showError("Incorrect password");
                        } else {
                            // Login successful
                            saveLoginState(user);
                            showSuccessMessage();
                            navigateToMain();

                            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                            prefs.edit().putBoolean("isFirstTime", false).apply();
                        }
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        hideLoading();
                        showError("Login error: " + e.getMessage());
                        Log.e(TAG, "Login error", e);
                    });
                }
            });
        }, 3000);
    }

    private void saveLoginState(User user) {
        try {
            SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save all user information
            editor.putBoolean("isLoggedIn", true);
            editor.putInt("userId", user.getId());
            editor.putString("userEmail", user.getEmail());
            editor.putString("userPhone", user.getPhone());
            editor.putString("userName", user.getName());
            editor.putString("userImage", user.getProfileImagePath());

            editor.apply();

            // Log the saved data for debugging
            Log.d(TAG, "Saved user data:");
            Log.d(TAG, "ID: " + user.getId());
            Log.d(TAG, "Email: " + user.getEmail());
            Log.d(TAG, "Phone: " + user.getPhone());
            Log.d(TAG, "Name: " + user.getName());

            // Verify the saved data
            verifyUserData();

        } catch (Exception e) {
            Log.e(TAG, "Error saving user data", e);
            showError("Error saving user data");
        }
    }

    private void verifyUserData() {
        SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
        Log.d(TAG, "Verifying saved data:");
        Log.d(TAG, "IsLoggedIn: " + prefs.getBoolean("isLoggedIn", false));
        Log.d(TAG, "UserId: " + prefs.getInt("userId", -1));
        Log.d(TAG, "Email: " + prefs.getString("userEmail", "not found"));
        Log.d(TAG, "Phone: " + prefs.getString("userPhone", "not found"));
        Log.d(TAG, "Name: " + prefs.getString("userName", "not found"));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    private void showSuccessMessage() {
        Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Sign in successful");
    }

    private void navigateToMain() {
        // Verify data before navigation
        verifyUserData();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }



    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnSignUp.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnSignUp.setEnabled(true);
    }



    private void navigateToSignUp() {
        Intent intent = new Intent(this, SignUp.class);
        startActivity(intent);
    }


    private void handleForgotPassword() {
        // Implement forgot password functionality
        Toast.makeText(this, "Forgot password functionality coming soon!", Toast.LENGTH_SHORT).show();
    }
}