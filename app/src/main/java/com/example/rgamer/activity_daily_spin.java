package com.example.rgamer;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class activity_daily_spin extends AppCompatActivity {

    /* ================= UI ================= */
    private ImageView btnBack, imgWheel;
    private TextView txtCoins, txtSpinsLeft;
    private MaterialButton btnSpin;
    private AdView adView;

    /* ================= FIREBASE ================= */
    private FirebaseFirestore db;
    private String uid;

    /* ================= PREF ================= */
    private UserPref userPref;

    /* ================= SPIN ================= */
    private static final int MAX_DAILY_SPINS = 10;
    private boolean isSpinning = false;

    /* ================= REWARDED AD ================= */
    private RewardedAd rewardedAd;
    private int pendingReward = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_spin);

        enableFullScreen(); // 🔥 FULL SCREEN

        userPref = new UserPref(this);
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        btnBack = findViewById(R.id.btnBack);
        imgWheel = findViewById(R.id.imgWheel);
        txtCoins = findViewById(R.id.txtCoins);
        txtSpinsLeft = findViewById(R.id.txtSpinsLeft);
        btnSpin = findViewById(R.id.btnSpin);
        adView = findViewById(R.id.adView);

        /* ================= ADS ================= */
        MobileAds.initialize(this, status -> {});
        adView.loadAd(new AdRequest.Builder().build());

        updateCoinsUI();
        updateSpinUI();

        loadRewardedAd();

        btnBack.setOnClickListener(v -> finish());

        btnSpin.setOnClickListener(v -> {
            if (isSpinning) return;

            if (!canSpinToday()) {
                Toast.makeText(this,
                        "Daily spin limit reached. Come back tomorrow!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            startSpin();
        });
    }

    /* ==================================================
       FULL SCREEN MODE
       ================================================== */

    private void enableFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller =
                    getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableFullScreen(); // 🔁 re-apply
    }

    /* ==================================================
       SPIN LOGIC
       ================================================== */

    private void startSpin() {
        isSpinning = true;
        btnSpin.setEnabled(false);

        int rotation = (360 * 6) + new Random().nextInt(360);

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
                float angle = imgWheel.getRotation() % 360;
                pendingReward = getRewardFromAngle(angle);
                showResultDialog(pendingReward);
            }
        });
    }

    private int getRewardFromAngle(float angle) {
        if (angle < 51) return 0;
        else if (angle < 103) return 2;
        else if (angle < 154) return 4;
        else if (angle < 206) return 6;
        else if (angle < 257) return 7;
        else if (angle < 309) return 8;
        else if (angle < 335) return 10;
        else return 0;
    }

    /* ==================================================
       RESULT DIALOG + REWARDED AD
       ================================================== */

    private void showResultDialog(int reward) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_spin_result);
        dialog.setCancelable(false);

        TextView txtWinAmount = dialog.findViewById(R.id.txtWinAmount);
        MaterialButton btnOk = dialog.findViewById(R.id.btnOk);

        txtWinAmount.setText("+" + reward + " Coins");

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            showRewardedAd();
        });

        dialog.show();
    }

    private void showRewardedAd() {
        if (rewardedAd == null) {
            applyReward();
            return;
        }

        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedAd = null;
                        loadRewardedAd();
                        applyReward();
                    }
                }
        );

        rewardedAd.show(this, rewardItem -> {});
    }

    /* ==================================================
       APPLY REWARD
       ================================================== */

    private void applyReward() {
        incrementDailySpin();

        userPref.addCoins(pendingReward);
        updateCoinsUI();
        updateSpinUI();

        if (uid != null) {
            db.collection("users")
                    .document(uid)
                    .update("coins", userPref.getCoins());
        }

        isSpinning = false;
        btnSpin.setEnabled(true);
    }

    /* ==================================================
       DAILY SPIN LIMIT
       ================================================== */

    private boolean canSpinToday() {
        return userPref.getTodaySpinCount() < MAX_DAILY_SPINS;
    }

    private void incrementDailySpin() {
        userPref.increaseSpinCount();
    }

    private void updateSpinUI() {
        txtSpinsLeft.setText(
                "Spins Left: " +
                        (MAX_DAILY_SPINS - userPref.getTodaySpinCount())
        );
    }

    /* ==================================================
       REWARDED AD LOAD
       ================================================== */

    private void loadRewardedAd() {
        RewardedAd.load(
                this,
                "ca-app-pub-3940256099942544/5224354917",
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }
                }
        );
    }

    /* ==================================================
       UI
       ================================================== */

    private void updateCoinsUI() {
        txtCoins.setText(String.valueOf(userPref.getCoins()));
    }
}
