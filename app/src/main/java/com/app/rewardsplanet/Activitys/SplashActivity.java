package com.app.rewardsplanet.Activitys;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    UserPref userPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        userPref = new UserPref(this);
        makeFullScreen();

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

    private void makeFullScreen() {
        Window window = getWindow();

        // 🔥 Make content go behind system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );

                // Optional: hide bars (remove if you only want transparent top)
                // controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }

        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // 🔥 Make status bar transparent (TOP FIX)
        window.setStatusBarColor(Color.TRANSPARENT);

        // 🔥 Optional: make navigation bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }
}
