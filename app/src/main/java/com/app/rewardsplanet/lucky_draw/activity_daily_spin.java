package com.app.rewardsplanet.lucky_draw;

import android.animation.*;
import android.app.Dialog;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.models.SpinResponse;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.google.android.material.button.MaterialButton;

// Ads
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.rewarded.*;

import retrofit2.*;

public class activity_daily_spin extends AppCompatActivity {

    /* ================= UI ================= */
    private ImageView imgWheel;
    private MaterialButton btnSpin;
    private TextView txtCoins, txtSpinsLeft;
    private AdView adView;

    /* ================= STATE ================= */
    private boolean isSpinning = false;
    private ObjectAnimator infiniteAnimator;

    private UserPref userPref;
    private String uid;

    private int remainingSpins = -1;

    /* ================= ADS ================= */
    private RewardedAd rewardedAd;
    private int spinCounterForAd = 0;

    /* ================= SOUND ================= */
    private MediaPlayer winSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_spin);

        imgWheel = findViewById(R.id.imgWheel);
        btnSpin = findViewById(R.id.btnSpin);
        txtCoins = findViewById(R.id.txtCoins);
        txtSpinsLeft = findViewById(R.id.txtSpinsLeft);
        adView = findViewById(R.id.adView);

        userPref = new UserPref(this);
        uid = userPref.getUid();
        findViewById(com.app.rewardsplanet.R.id.btnBack).setOnClickListener(v -> finish());

        updateUI();
        loadSpinStatus();
        makeFullScreen();


        // 🔥 Ads init
        MobileAds.initialize(this, status -> {});
        adView.loadAd(new AdRequest.Builder().build());
        loadAd();

        btnSpin.setOnClickListener(v -> {
            if (isSpinning) return;

            if (remainingSpins <= 0) {
                Toast.makeText(this, "Daily limit reached", Toast.LENGTH_SHORT).show();
                return;
            }

            startSpin();
        });
    }

    /* ================= UI ================= */


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

    private void updateUI() {

        txtCoins.setText(String.valueOf(userPref.getCoins()));

        if (remainingSpins == -1) {
            txtSpinsLeft.setText("Loading...");
            btnSpin.setEnabled(false);
        } else {
            txtSpinsLeft.setText("Spins Left: " + remainingSpins);
            btnSpin.setEnabled(remainingSpins > 0);
        }
    }

    /* ================= LOAD STATUS ================= */

    private void loadSpinStatus() {

        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                ApiService api = ApiClient.getClient().create(ApiService.class);

                api.spinStatus(bearerToken).enqueue(new Callback<SpinResponse>() {
                    @Override
                    public void onResponse(Call<SpinResponse> call, Response<SpinResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            remainingSpins = response.body().remainingSpins;
                        } else {
                            int localUsed = userPref.getTodaySpinCount();
                            remainingSpins = Math.max(0, 10 - localUsed);
                        }
                        updateUI();
                    }

                    @Override
                    public void onFailure(Call<SpinResponse> call, Throwable t) {
                        int localUsed = userPref.getTodaySpinCount();
                        remainingSpins = Math.max(0, 10 - localUsed);
                        updateUI();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                int localUsed = userPref.getTodaySpinCount();
                remainingSpins = Math.max(0, 10 - localUsed);
                updateUI();
            }
        });
    }

    /* ================= SPIN ================= */

    private void startSpin() {

        isSpinning = true;
        btnSpin.setEnabled(false);

        infiniteAnimator = ObjectAnimator.ofFloat(imgWheel, "rotation", 0f, 360f);
        infiniteAnimator.setDuration(500);
        infiniteAnimator.setRepeatCount(ValueAnimator.INFINITE);
        infiniteAnimator.setInterpolator(new LinearInterpolator());
        infiniteAnimator.start();

        callSpinApi();
    }

    private void callSpinApi() {

        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                ApiService api = ApiClient.getClient().create(ApiService.class);

                api.spin(bearerToken).enqueue(new Callback<SpinResponse>() {

                    @Override
                    public void onResponse(Call<SpinResponse> call, Response<SpinResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {

                            int reward = response.body().reward;
                            remainingSpins = response.body().remainingSpins;
                            userPref.increaseSpinCount();

                            stopSpin(reward);

                        } else {

                            if (response.code() == 400) {

                                if (infiniteAnimator != null) infiniteAnimator.cancel();

                                isSpinning = false;
                                remainingSpins = 0;
                                updateUI();

                                Toast.makeText(activity_daily_spin.this,
                                        "Daily limit reached",
                                        Toast.LENGTH_SHORT).show();

                            } else {
                                performLocalSpin();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<SpinResponse> call, Throwable t) {
                        performLocalSpin();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                performLocalSpin();
            }
        });
    }

    private void performLocalSpin() {
        int localUsed = userPref.getTodaySpinCount();
        if (localUsed >= 10) {
            if (infiniteAnimator != null) infiniteAnimator.cancel();
            isSpinning = false;
            remainingSpins = 0;
            updateUI();
            Toast.makeText(this, "Daily limit reached", Toast.LENGTH_SHORT).show();
            return;
        }

        userPref.increaseSpinCount();
        remainingSpins = Math.max(0, 10 - userPref.getTodaySpinCount());
        int[] rewards = {0, 5, 6, 7, 10};
        int reward = rewards[new java.util.Random().nextInt(rewards.length)];
        stopSpin(reward);
    }

    private void stopSpin(int reward) {

        if (infiniteAnimator != null) infiniteAnimator.cancel();

        float target = getAngle(reward);
        float current = imgWheel.getRotation();

        ObjectAnimator finalSpin = ObjectAnimator.ofFloat(
                imgWheel,
                "rotation",
                current,
                current + 360 * 5 + target
        );

        finalSpin.setDuration(3000);
        finalSpin.setInterpolator(new DecelerateInterpolator());

        finalSpin.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

                userPref.addCoins(reward);
                updateUI();
                com.app.rewardsplanet.repository.UserRepository.getInstance(activity_daily_spin.this).refreshCurrentUser();

                playEffects();

                spinCounterForAd++;

                if (spinCounterForAd >= 2) {
                    spinCounterForAd = 0;
                    showRewardDialogWithAd(reward);
                } else {
                    showRewardDialogOnly(reward);
                }

                isSpinning = false;
            }
        });

        finalSpin.start();
    }

    private float getAngle(int reward) {

        int index;

        switch (reward) {
            case 0:  index = 0; break;
            case 2:  index = 1; break;
            case 4:  index = 2; break;
            case 6:  index = 3; break;
            case 7:  index = 4; break;
            case 8:  index = 5; break;
            case 10: index = 6; break;
            default: index = 0;
        }

        float section = 360f / 7f;

        // 🎯 center of slice
        float angle = index * section + (section / 2f);

        // 🔥 pointer is at TOP → invert
        float finalAngle = 360f - angle;

        // 🎲 small randomness (optional but recommended)
        float random = (float)(Math.random() * 6f - 3f);

        return finalAngle + random;
    }

    /* ================= EFFECTS ================= */

    private void playEffects() {

        FrameLayout root = findViewById(android.R.id.content);

        if (root != null && imgWheel != null) {
            // 💰 coins
            for (int i = 0; i < 5; i++) {

                ImageView coin = new ImageView(this);
                coin.setImageResource(R.drawable.ic_coin);
                root.addView(coin);

                coin.setX(imgWheel.getX() + imgWheel.getWidth() / 2f);
                coin.setY(imgWheel.getY() + imgWheel.getHeight() / 2f);

                ObjectAnimator moveY = ObjectAnimator.ofFloat(coin, "translationY", 0, -600f);
                ObjectAnimator fade = ObjectAnimator.ofFloat(coin, "alpha", 1f, 0f);

                AnimatorSet set = new AnimatorSet();
                set.setDuration(800);
                set.setStartDelay(i * 100L);
                set.playTogether(moveY, fade);

                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        root.removeView(coin);
                    }
                });

                set.start();
            }

            // 🎆 fireworks
            TextView fire = new TextView(this);
            fire.setText("🎆✨🎉");
            fire.setTextSize(30);
            root.addView(fire);

            ObjectAnimator fade = ObjectAnimator.ofFloat(fire, "alpha", 1f, 0f);
            fade.setDuration(1000);

            fade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    root.removeView(fire);
                }
            });

            fade.start();
        }

        // 🔊 SOUND (SAFE)
        try {
            stopSound();
            winSound = MediaPlayer.create(this, R.raw.win_sound);
            if (winSound != null) {
                winSound.setOnCompletionListener(mp -> stopSound());
                winSound.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 📳 vibration
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(150);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= STOP SOUND ================= */

    private void stopSound() {
        if (winSound != null) {
            try {
                if (winSound.isPlaying()) {
                    winSound.stop();
                }
                winSound.release();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                winSound = null;
            }
        }
    }

    /* ================= DIALOG ================= */

    private void showRewardDialogOnly(int reward) {

        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_spin_result);

        TextView txtWinAmount = d.findViewById(R.id.txtWinAmount);
        TextView txtCurrentBalance = d.findViewById(R.id.txtCurrentBalance);
        MaterialButton ok = d.findViewById(R.id.btnOk);

        if (txtWinAmount != null) {
            txtWinAmount.setText("+" + reward + " Coins");
        }
        if (txtCurrentBalance != null) {
            txtCurrentBalance.setText("Current Balance: " + userPref.getCoins() + " Coins");
        }

        if (ok != null) {
            ok.setOnClickListener(v -> {
                stopSound();
                d.dismiss();
            });
        }

        d.setOnDismissListener(dialog -> stopSound());

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        d.show();
    }

    private void showRewardDialogWithAd(int reward) {

        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_spin_result);

        TextView txtWinAmount = d.findViewById(R.id.txtWinAmount);
        TextView txtCurrentBalance = d.findViewById(R.id.txtCurrentBalance);
        MaterialButton ok = d.findViewById(R.id.btnOk);

        if (txtWinAmount != null) {
            txtWinAmount.setText("+" + reward + " Coins");
        }
        if (txtCurrentBalance != null) {
            txtCurrentBalance.setText("Current Balance: " + userPref.getCoins() + " Coins");
        }

        if (ok != null) {
            ok.setOnClickListener(v -> {
                stopSound();
                d.dismiss();
                showAd();
            });
        }

        d.setOnDismissListener(dialog -> stopSound());

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        d.show();
    }

    /* ================= ADS ================= */

    private void loadAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this,
                AdsManager.REWARDED_AD_ID,
                adRequest,
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        rewardedAd = null;
                    }
                });
    }

    private void showAd() {

        if (rewardedAd != null) {

            rewardedAd.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            rewardedAd = null;
                            loadAd();
                        }
                    });

            rewardedAd.show(this, rewardItem -> {});

        } else {
            loadAd();
        }
    }

    /* ================= ERROR ================= */

    private void error() {

        if (infiniteAnimator != null) infiniteAnimator.cancel();

        isSpinning = false;
        btnSpin.setEnabled(true);

        Toast.makeText(this, "Server error", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSound();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSound();
    }
}