package com.demo.donation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUp extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ShapeableImageView ivProfile;
    private ImageView ivCamera;
    private TextInputEditText etName, etPhone, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnSignUp;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        ivCamera = findViewById(R.id.ivCamera);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        ivCamera.setOnClickListener(v -> openImagePicker());
        ivProfile.setOnClickListener(v -> openImagePicker());
        btnSignUp.setOnClickListener(v -> validateAndSignUp());
        tvLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ivProfile.setImageURI(selectedImageUri);
        }
    }

    private void validateAndSignUp() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            return;
        }

        if (!isValidPhone(phone)) {
            etPhone.setError("Please enter a valid phone number");
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            return;
        }

        if (!isValidPassword(password)) {
            etPassword.setError("Password must be at least 8 characters long");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Proceed with sign up
        performSignUp(name, phone, email, password);
    }

    private void performSignUp(String name, String phone, String email, String password) {
        showLoading();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        new Handler().postDelayed(() -> {
            executor.execute(() -> {
                try {
                    // Get database instance
                    AppDataBase db = AppDataBase.getInstance(getApplicationContext());
                    UserDao userDao = db.userDao();

                    // Check if user already exists
                    User existingUser = userDao.getUserByEmail(email);

                    if (existingUser != null) {
                        handler.post(() -> {
                            hideLoading();
                            showError("Email already registered");
                        });
                        return;
                    }

                    // Get image path if image was selected
                    String imagePath = selectedImageUri != null ? selectedImageUri.toString() : "";

                    // Create new user with all fields
                    User newUser = new User(name, phone, email, password, imagePath);
                    long userId = userDao.insert(newUser);

                    handler.post(() -> {
                        hideLoading();
                        if (userId > 0) {
                            // Save all user data
                            saveLoginState(newUser, userId);
                            showSuccessMessage();
                            navigateToMain();
                        } else {
                            showError("Failed to create account");
                        }
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        hideLoading();
                        showError("Error creating account: " + e.getMessage());
                    });
                }
            });
        }, 3000);
    }

    private void saveLoginState(User user, long userId) {
        try {
            SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save all user information
            editor.putBoolean("isLoggedIn", true);
            editor.putLong("userId", userId);
            editor.putString("userEmail", user.getEmail());
            editor.putString("userPhone", user.getPhone());
            editor.putString("userName", user.getName());
            editor.putString("userImage", user.getProfileImagePath());

            // Apply changes
            editor.apply();

            // Log saved data for debugging
            Log.d("SignUp", "Saved user data:");
            Log.d("SignUp", "ID: " + userId);
            Log.d("SignUp", "Email: " + user.getEmail());
            Log.d("SignUp", "Phone: " + user.getPhone());
            Log.d("SignUp", "Name: " + user.getName());
        } catch (Exception e) {
            Log.e("SignUp", "Error saving user data", e);
            showError("Error saving user data");
        }
    }

    // Add this method to verify saved data
    private void verifyUserData() {
        SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
        Log.d("SignUp", "Verifying saved data:");
        Log.d("SignUp", "IsLoggedIn: " + prefs.getBoolean("isLoggedIn", false));
        Log.d("SignUp", "UserId: " + prefs.getLong("userId", -1));
        Log.d("SignUp", "Email: " + prefs.getString("userEmail", "not found"));
        Log.d("SignUp", "Phone: " + prefs.getString("userPhone", "not found"));
        Log.d("SignUp", "Name: " + prefs.getString("userName", "not found"));
    }

    private void navigateToMain() {
        // Verify data before navigation
        verifyUserData();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveLoginState(String email, long userId) {
        SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userEmail", email);
        editor.putLong("userId", userId);
        editor.apply();
    }

    private boolean isValidPhone(String phone) {
        return phone.length() >= 10 && Patterns.PHONE.matcher(phone).matches();
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8;
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnSignUp.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnSignUp.setEnabled(true);
    }

    private void showSuccessMessage() {
        Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, SignIn.class);
        startActivity(intent);
        finish();
    }

}