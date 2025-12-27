package com.example.rgamer;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.button.MaterialButton;

import java.util.Random;

public class activity_daily_spin extends AppCompatActivity {

    /* ================= UI ================= */
    private ImageView btnBack, imgWheel;
    private TextView txtCoins, txtSpinsLeft;
    private MaterialButton btnSpin;
    private AdView adView;

    /* ================= PREF ================= */
    private UserPref userPref;

    /* ================= SPIN ================= */
    private static final int[] REWARDS = {10, 20, 50, 100, 200, 500};
    private boolean isSpinning = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_spin);

        /* ================= PREF ================= */
        userPref = new UserPref(this);

        /* ================= UI INIT ================= */
        btnBack = findViewById(R.id.btnBack);
        imgWheel = findViewById(R.id.imgWheel);
        txtCoins = findViewById(R.id.txtCoins);
        txtSpinsLeft = findViewById(R.id.txtSpinsLeft);
        btnSpin = findViewById(R.id.btnSpin);
        adView = findViewById(R.id.adView);

        /* ================= ADS (SAFE) ================= */
        MobileAds.initialize(this, initializationStatus -> {});

        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        /* ================= UI ================= */
        txtSpinsLeft.setText("Unlimited Spins");
        updateCoinsUI();

        /* ================= BACK ================= */
        btnBack.setOnClickListener(v -> finish());

        /* ================= SPIN ================= */
        btnSpin.setOnClickListener(v -> {
            if (!isSpinning) {
                startSpin();
            }
        });
    }

    /* ==================================================
       SPIN LOGIC
       ================================================== */

    private void startSpin() {
        isSpinning = true;
        btnSpin.setEnabled(false);

        int reward = REWARDS[new Random().nextInt(REWARDS.length)];

        int rotation = (360 * (5 + new Random().nextInt(4)))
                + new Random().nextInt(360);

        ObjectAnimator animator = ObjectAnimator.ofFloat(
                imgWheel,
                "rotation",
                imgWheel.getRotation(),
                imgWheel.getRotation() + rotation
        );

        animator.setDuration(4000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                onSpinComplete(reward);
            }
        });
    }

    private void onSpinComplete(int reward) {
        // Add coins using UserPref
        userPref.addCoins(reward);

        updateCoinsUI();

        Toast.makeText(
                this,
                "🎉 You won " + reward + " coins!",
                Toast.LENGTH_SHORT
        ).show();

        isSpinning = false;
        btnSpin.setEnabled(true);
    }

    /* ==================================================
       UI
       ================================================== */

    private void updateCoinsUI() {
        txtCoins.setText(String.valueOf(userPref.getCoins()));
    }

    /* ================= AD LIFECYCLE ================= */

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}
