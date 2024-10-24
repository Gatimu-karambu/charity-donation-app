package com.demo.donation;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// SplashActivity.java
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private static final long SPLASH_DURATION = 3500; // 3 seconds
    private ImageView splashImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        splashImage = findViewById(R.id.splashImage);

        // Create animations
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(splashImage, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(splashImage, "scaleY", 0.5f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(splashImage, "alpha", 0f, 1f);

        // Combine animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(1500); // 1.5 seconds for animation
        animatorSet.start();

        // Handle navigation after splash
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToNextScreen();
            }
        }, SPLASH_DURATION);
    }

    private void navigateToNextScreen() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean("isFirstTime", true);

        Intent intent;
        if (isFirstTime) {
            // First time launch - go to SignUp
            intent = new Intent(SplashScreen.this, SignUp.class);
            // Save that app has been launched
        } else {
            // Subsequent launches - go to SignIn
            intent = new Intent(SplashScreen.this, MainActivity.class);
        }

        startActivity(intent);
        finish(); // Close splash activity
    }
}