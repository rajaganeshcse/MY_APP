package com.example.rgamer;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class activity_daily_spin extends AppCompatActivity {

    /* ================= CONFIG ================= */

    private static final int FREE_SPINS_PER_DAY = 5;
    private static final int[] WHEEL_VALUES = {0, 2, 4, 6, 7, 8, 10};

    /* ================= UI ================= */

    TextView txtSpinsLeft, txtCoins;
    MaterialButton btnSpin;
    ImageView imgWheel, btnBack;

    /* ================= FIREBASE ================= */

    FirebaseFirestore db;
    String uid;

    /* ================= LOCAL ================= */

    UserPref userPref;
    int spinsUsedToday = 0;
    float currentRotation = 0f;
    int pendingReward = 0;
    boolean isSpinning = false;

    /* ================= ADS ================= */

    RewardedAd rewardedAd;
    boolean adLoading = false;

    /* ================= LIFECYCLE ================= */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStatusBar();
        setContentView(R.layout.activity_daily_spin);

        /* ---------- UI ---------- */
        txtSpinsLeft = findViewById(R.id.txtSpinsLeft);
        txtCoins = findViewById(R.id.txtCoins);
        btnSpin = findViewById(R.id.btnSpin);
        imgWheel = findViewById(R.id.imgWheel);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        /* ---------- INIT ---------- */
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        userPref = new UserPref(this);

        updateCoinsUI();      // ✅ show coins immediately
        loadRewardedAd();
        checkDailySpin();

        btnSpin.setOnClickListener(v -> handleSpinClick());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCoinsUI(); // ✅ keep header synced
    }

    /* ================= SPIN LOGIC ================= */

    private void handleSpinClick() {
        if (isSpinning) return;

        if (spinsUsedToday < FREE_SPINS_PER_DAY) {
            startSpin();
            return;
        }

        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> startSpin());
            rewardedAd = null;
            loadRewardedAd();
        }
    }

    private void startSpin() {
        isSpinning = true;
        btnSpin.setEnabled(false);

        int index = new Random().nextInt(WHEEL_VALUES.length);
        pendingReward = WHEEL_VALUES[index];

        float slice = 360f / WHEEL_VALUES.length;
        float rotateBy = 720 + index * slice;

        RotateAnimation rotate = new RotateAnimation(
                currentRotation,
                currentRotation + rotateBy,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
        );

        rotate.setDuration(2500);
        rotate.setFillAfter(true);
        rotate.setInterpolator(new DecelerateInterpolator());

        rotate.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation animation) {}

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                currentRotation = (currentRotation + rotateBy) % 360;
                spinsUsedToday++;

                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> showWinDialog(pendingReward), 300
                );
            }

            @Override public void onAnimationRepeat(android.view.animation.Animation animation) {}
        });

        imgWheel.startAnimation(rotate);
    }

    /* ================= RESULT DIALOG ================= */

    private void showWinDialog(int reward) {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_spin_result);
        dialog.setCancelable(false);

        TextView txtWinAmount = dialog.findViewById(R.id.txtWinAmount);
        MaterialButton btnOk = dialog.findViewById(R.id.btnOk);

        txtWinAmount.setText("+" + reward + " Coins");

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            saveSpinResult(reward);
            isSpinning = false;
            btnSpin.setEnabled(true);
        });

        dialog.show();
    }

    /* ================= SAVE RESULT ================= */

    private void saveSpinResult(int reward) {

        if (uid == null) return;

        /* ✅ LOCAL UPDATE (INSTANT UI) */
        userPref.addCoins(reward);
        updateCoinsUI();

        /* 🔄 FIRESTORE SYNC */
        db.runTransaction(transaction -> {
            DocumentReference userRef = db.collection("users").document(uid);
            transaction.update(userRef,
                    "coins", FieldValue.increment(reward),
                    "dailySpinCount", spinsUsedToday,
                    "dailySpinDate", today()
            );
            return null;
        }).addOnSuccessListener(v -> updateUI());
    }

    /* ================= DAILY CHECK ================= */

    private void checkDailySpin() {
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String saved
