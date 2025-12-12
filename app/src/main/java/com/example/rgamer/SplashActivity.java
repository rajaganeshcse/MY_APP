package com.example.rgamer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.Primary));
        }

        setContentView(R.layout.activity_splash);

        // Splash 2-sec delay
        new Handler().postDelayed(() -> {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                // User already logged → go to home
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // New user → go to onboarding
                startActivity(new Intent(SplashActivity.this, OnBoardingActivity.class));
            }

            finish();

        }, 2000); // 2 seconds
    }
}
