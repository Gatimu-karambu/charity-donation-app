package com.demo.donation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.demo.donation.AppDataBase;
import com.demo.donation.R;
import com.demo.donation.User;
import com.demo.donation.api.ApiClient;
import com.demo.donation.api.DonationApi;
import com.demo.donation.api.DonationRequest;
import com.demo.donation.api.DonationResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Donate extends AppCompatActivity {
    private CardView cardMoney, cardFood, cardClothes, cardFurniture, cardOthers;
    private TextInputEditText etOtherItem;
    private DonationApi donationApi;
    private AppDataBase db;
    private User currentUser;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        // Initialize API and database
        donationApi = ApiClient.getClient().create(DonationApi.class);
        db = AppDataBase.getInstance(getApplicationContext());
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupListeners();
        loadUserData();
    }

    private void loadUserData() {
        executor.execute(() -> {
            // Get logged in user's email from SharedPreferences
            String userEmail = getSharedPreferences("DonationApp", MODE_PRIVATE)
                    .getString("userEmail", "");

            if (!userEmail.isEmpty()) {
                currentUser = db.userDao().getUserByEmail(userEmail);
                mainHandler.post(() -> {
                    if (currentUser == null) {
                        Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } else {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void initViews() {
        cardMoney = findViewById(R.id.cardMoney);
        cardFood = findViewById(R.id.cardFood);
        cardClothes = findViewById(R.id.cardClothes);
        cardFurniture = findViewById(R.id.cardFurniture);
        cardOthers = findViewById(R.id.cardOthers);
        etOtherItem = findViewById(R.id.etOtherItem);
    }

    private void setupListeners() {
        cardMoney.setOnClickListener(v -> handleMoneyDonation());
        cardFood.setOnClickListener(v -> handleFoodDonation());
        cardClothes.setOnClickListener(v -> handleClothesDonation());
        cardFurniture.setOnClickListener(v -> handleFurnitureDonation());
        cardOthers.setOnClickListener(v -> handleOthersDonation());
    }

    private void handleMoneyDonation() {
        if (currentUser != null) {
            showMoneyDialog("Money");
        } else {
            Toast.makeText(this, "Please wait while loading user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFoodDonation() {
        if (currentUser != null) {
            showDonationDialog("Food");
        } else {
            Toast.makeText(this, "Please wait while loading user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleClothesDonation() {
        if (currentUser != null) {
            showDonationDialog("Clothes");
        } else {
            Toast.makeText(this, "Please wait while loading user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFurnitureDonation() {
        if (currentUser != null) {
            showDonationDialog("Furniture");
        } else {
            Toast.makeText(this, "Please wait while loading user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleOthersDonation() {
        if (currentUser == null) {
            Toast.makeText(this, "Please wait while loading user data", Toast.LENGTH_SHORT).show();
            return;
        }

        String otherItem = etOtherItem.getText().toString().trim();
        if (otherItem.isEmpty()) {
            etOtherItem.setError("Please specify the item you want to donate");
            return;
        }
        showDonationDialog("Other: " + otherItem);
    }




    private void showSuccessDialog(String itemType, Double amount) {
        String message;
        if (amount != null) {
            message = String.format("Thank you for donating KES %.2f!\nYour contribution will help make a difference. Business number: 999333.", amount);
        } else {
            message = String.format("Thank you for donating %s!\nWe will contact you soon to arrange collection.", itemType);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Donation Successful!")
                .setMessage(message)
                .setIcon(R.drawable.success) // Add a success icon drawable
                .setPositiveButton("View My Donations", (dialog, which) -> {
                    // Navigate to donation history
                    startActivity(new Intent(this, DonationHistoryActivity.class));
                    finish();
                })
                .setNegativeButton("Done", (dialog, which) -> finish())
                .show();
    }




    private void submitDonation(String itemType, String description, Double amount) {
        if (currentUser == null) {
            Toast.makeText(this, "User data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log the data being sent
        Log.d("DONATION_DEBUG", "Submitting donation: " +
                "Type: " + itemType +
                ", Description: " + description +
                ", Amount: " + amount +
                ", Phone: " + currentUser.getPhone() +
                ", Name: " + currentUser.getName());

        DonationRequest request = new DonationRequest(
                currentUser.getName(),
                currentUser.getPhone(),
                itemType,
                amount,
                description
        );








        donationApi.addDonation(request).enqueue(new Callback<DonationResponse>() {
            @Override
            public void onResponse(Call<DonationResponse> call, Response<DonationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("DONATION_DEBUG", "Success: " + response.body().getMessage());
                    saveDonationLocally(itemType, description, amount);
                    showSuccessDialog(itemType, amount);
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("DONATION_DEBUG", "Error: " + errorBody);
                        Toast.makeText(Donate.this,
                                "Failed: " + errorBody,
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<DonationResponse> call, Throwable t) {
                Log.e("DONATION_DEBUG", "Error: " + t.getMessage(), t);
                Toast.makeText(Donate.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDonationLocally(String itemType, String description, Double amount) {
        executor.execute(() -> {
            try {
                Donation donation = new Donation(
                        currentUser.getId(),
                        itemType,
                        amount,
                        description,
                        "Pending",
                        System.currentTimeMillis()
                );
                db.donationDao().insert(donation);
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Log.e("DONATION_DEBUG", "Error saving locally: " + e.getMessage());
                    finish();
                });
            }
        });
    }

    private void showDonationDialog(String itemType) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_donation_details, null);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Donation")
                .setView(dialogView)
                .setMessage("Thank you for choosing to donate " + itemType)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    submitDonation(itemType, description, null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMoneyDialog(String itemType) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_money_donation, null);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Donate Money")
                .setView(dialogView)
                .setMessage("Please enter the amount you wish to donate")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        try {
                            double amount = Double.parseDouble(amountStr);
                            submitDonation(itemType, "Money donation", amount);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Please enter a valid amount",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Please enter an amount",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}