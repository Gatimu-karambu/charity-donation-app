package com.demo.donation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private CardView cardDonate, cardAboutUs, cardContactUs, cardLogOut,cardDonationHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
    }

    private void initViews() {
        cardDonate = findViewById(R.id.cardDonate);
        cardAboutUs = findViewById(R.id.cardAboutUs);
        cardContactUs = findViewById(R.id.cardContactUs);
        cardLogOut = findViewById(R.id.cardLogOut);
        cardDonationHistory = findViewById(R.id.cardDonationHistory);
    }

    private void setupListeners() {
        cardDonate.setOnClickListener(v -> handleDonate());
        cardAboutUs.setOnClickListener(v -> handleAboutUs());
        cardContactUs.setOnClickListener(v -> handleContactUs());
        cardLogOut.setOnClickListener(v -> handleLogOut());
        cardDonationHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, DonationHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void handleDonate() {
        // TODO: Implement donation flow
        Intent intent = new Intent(this, Donate.class);
        startActivity(intent);
    }

    private void handleAboutUs() {
        // TODO: Implement about us screen
       aboutUs();
    }

    private void handleContactUs() {
        // TODO: Implement contact us screen
        contacUs();
    }

    private void handleLogOut() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Set isFirstTime to true
        prefs.edit().putBoolean("isFirstTime", true).apply();

        // Navigate to login screen
        Intent intent = new Intent(this, SplashScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void aboutUs() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")));
        }
    }

    private void contacUs() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:mahlontowett@gmail.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Donation App");
        startActivity(Intent.createChooser(intent, "Send Email"));
    }
}