package com.demo.donation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.demo.donation.api.ApiClient;
import com.demo.donation.api.DonationApi;
import com.demo.donation.api.DonationHistory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DonationHistoryActivity extends AppCompatActivity {
    private static final String TAG = "DonationHistory";

    // UI Components
    private RecyclerView recyclerView;
    private DonationHistoryAdapter adapter;
    private TextView tvEmpty;
    private TextView tvTitle;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;

    // Background Threading
    private ExecutorService executor;
    private Handler mainHandler;

    // Data Sources
    private AppDataBase db;
    private DonationApi donationApi;

    // Date Formatter
    private final SimpleDateFormat apiDateFormat;

    public DonationHistoryActivity() {
        apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        apiDateFormat.setTimeZone(TimeZone.getDefault());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_history);

        verifyUserData();
        initializeComponents();
        setupViews();
        setupRecyclerView();
        loadDonations();
    }

    private void initializeComponents() {
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        db = AppDataBase.getInstance(getApplicationContext());
        donationApi = ApiClient.getClient().create(DonationApi.class);
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTitle = findViewById(R.id.tvTitle);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        swipeRefresh.setOnRefreshListener(this::loadDonations);
        tvTitle.setText("My Donations");
    }

    private void setupRecyclerView() {
        adapter = new DonationHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    private void loadDonations() {
        showLoading();

        SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
        String userPhone = prefs.getString("userPhone", "");
        String userEmail = prefs.getString("userEmail", "");

        Log.d(TAG, "Loading donations...");
        Log.d(TAG, "SharedPreferences - Phone: " + userPhone);
        Log.d(TAG, "SharedPreferences - Email: " + userEmail);

        if (userPhone.isEmpty()) {
            executor.execute(() -> {
                try {
                    User user = db.userDao().getUserByEmail(userEmail);
                    if (user != null) {
                        String phone = user.getPhone();
                        Log.d(TAG, "Found phone in database: " + phone);

                        String formattedPhone = formatPhoneNumber(phone);
                        prefs.edit().putString("userPhone", formattedPhone).apply();

                        mainHandler.post(() -> fetchDonationsFromApi(formattedPhone));
                    } else {
                        Log.e(TAG, "User not found in database for email: " + userEmail);
                        mainHandler.post(() -> {
                            hideLoading();
                            showError("User data not found");
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Database error", e);
                    mainHandler.post(() -> {
                        hideLoading();
                        showError("Error loading user data");
                    });
                }
            });
        } else {
            String formattedPhone = formatPhoneNumber(userPhone);
            fetchDonationsFromApi(formattedPhone);
        }
    }

    private String formatPhoneNumber(String phone) {
        // Remove leading zero if present and any non-digit characters
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1);
        }
        // Add country code if not present
        if (!cleaned.startsWith("254")) {
            cleaned = "254" + cleaned;
        }
        Log.d(TAG, "Original phone: " + phone + " | Formatted phone: " + cleaned);
        return cleaned;
    }

    private void fetchDonationsFromApi(String phone) {
        if (phone == null || phone.isEmpty()) {
            showError("Invalid phone number");
            return;
        }

        Log.d(TAG, "Fetching donations for phone: " + phone);
        Log.d(TAG, "API URL: " + ApiClient.getClient().baseUrl() + "donations/" + phone);

        donationApi.getUserDonations(phone).enqueue(new Callback<List<DonationHistory>>() {
            @Override
            public void onResponse(Call<List<DonationHistory>> call, Response<List<DonationHistory>> response) {
                Log.d(TAG, "API Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<DonationHistory> donations = response.body();
                    Log.d(TAG, "API Success: Retrieved " + donations.size() + " donations");

                    for (DonationHistory donation : donations) {
                        Log.d(TAG, String.format("API Donation - ID: %d, Type: %s, Status: %s",
                                donation.getId(),
                                donation.getDonationType(),
                                donation.getStatus()));
                    }

                    updateLocalDatabase(donations);
                } else {
                    Log.e(TAG, "API Error: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    loadFromLocalDatabase();
                    showError("Couldn't fetch latest donations");
                }
            }

            @Override
            public void onFailure(Call<List<DonationHistory>> call, Throwable t) {
                Log.e(TAG, "API Call Failed", t);
                Log.e(TAG, "Failed URL: " + call.request().url());
                loadFromLocalDatabase();
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void updateLocalDatabase(List<DonationHistory> apiDonations) {
        executor.execute(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
                String userEmail = prefs.getString("userEmail", "");
                Log.d(TAG, "Updating local database for user: " + userEmail);

                User user = db.userDao().getUserByEmail(userEmail);
                if (user != null) {
                    // Clear existing donations for this user first
                    db.donationDao().deleteUserDonations(user.getId());
                    Log.d(TAG, "Cleared existing donations for user ID: " + user.getId());

                    for (DonationHistory apiDonation : apiDonations) {
                        try {
                            long timestamp = apiDateFormat.parse(apiDonation.getDate()).getTime();

                            Donation newDonation = new Donation(
                                    user.getId(),
                                    apiDonation.getDonationType(),
                                    apiDonation.getAmount(),
                                    apiDonation.getDescription(),
                                    apiDonation.getStatus().toLowerCase(),
                                    timestamp
                            );
                            newDonation.setId(apiDonation.getId());
                            db.donationDao().insert(newDonation);

                            Log.d(TAG, String.format("Synced donation - ID: %d, Type: %s, Status: %s",
                                    apiDonation.getId(),
                                    apiDonation.getDonationType(),
                                    apiDonation.getStatus()));
                        } catch (ParseException e) {
                            Log.e(TAG, "Date parsing error", e);
                        }
                    }

                    // Load updated donations
                    List<Donation> updatedDonations = db.donationDao().getUserDonations(user.getId());
                    Log.d(TAG, "Total synced donations: " + updatedDonations.size());

                    mainHandler.post(() -> {
                        hideLoading();
                        if (updatedDonations.isEmpty()) {
                            showEmpty();
                        } else {
                            showDonations(updatedDonations);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        hideLoading();
                        showError("User not found");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Database error", e);
                mainHandler.post(() -> {
                    hideLoading();
                    showError("Error updating local database");
                });
            }
        });
    }



    // Only call loadFromLocalDatabase when API fails
    private void loadFromLocalDatabase() {
        executor.execute(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
                String userEmail = prefs.getString("userEmail", "");
                User user = db.userDao().getUserByEmail(userEmail);

                if (user != null) {
                    List<Donation> donations = db.donationDao().getUserDonations(user.getId());
                    Log.d(TAG, "Loaded " + donations.size() + " donations from local DB");

                    mainHandler.post(() -> {
                        hideLoading();
                        if (donations.isEmpty()) {
                            showEmpty();
                        } else {
                            showDonations(donations);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        hideLoading();
                        showEmpty();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Database error", e);
                mainHandler.post(() -> {
                    hideLoading();
                    showError("Error loading donations");
                });
            }
        });
    }



    private void verifyUserData() {
        SharedPreferences prefs = getSharedPreferences("DonationApp", MODE_PRIVATE);
        Log.d(TAG, "Verifying user data in SharedPreferences:");
        Log.d(TAG, "Phone: " + prefs.getString("userPhone", "not found"));
        Log.d(TAG, "Email: " + prefs.getString("userEmail", "not found"));
        Log.d(TAG, "Name: " + prefs.getString("userName", "not found"));
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showDonations(List<Donation> donations) {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        Log.d(TAG, "Displaying donations: " + donations.size());
        for (Donation d : donations) {
            Log.d(TAG, String.format("Display - ID: %d, Type: %s, Status: %s",
                    d.getId(), d.getDonationType(), d.getStatus()));
        }

        adapter.submitList(donations);
        Toast.makeText(this, "Loaded " + donations.size() + " donations", Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}