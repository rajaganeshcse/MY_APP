package com.example.rgamer.Activitys;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import com.example.rgamer.R;
import com.example.rgamer.UserPref;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    UserPref userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Status bar customization
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#5D6A73"));
        window.setNavigationBarColor(Color.parseColor("#5D6A73"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        userPref = new UserPref(this);

        // Force Firestore online on app start (fix offline error)
        FirebaseFirestore.getInstance().enableNetwork();

        // 2-second splash delay
        new Handler().postDelayed(() -> {

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            String savedUid = userPref.getUid();

            if (firebaseUser != null && savedUid != null && !savedUid.isEmpty()) {
                // Fully logged in
                startActivity(new Intent(SplashActivity.this, MainActivity.class));

            } else {
                // New user → Onboarding screen
                startActivity(new Intent(SplashActivity.this, OnBoardingActivity.class));
            }

            finish();

        }, 1000);
    }
}
