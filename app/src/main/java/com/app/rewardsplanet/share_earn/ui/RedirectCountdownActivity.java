package com.app.rewardsplanet.share_earn.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

public class RedirectCountdownActivity extends AppCompatActivity {

    public static final String EXTRA_REDIRECT_URL = "extra_redirect_url";
    public static final String EXTRA_OFFER_TITLE = "extra_offer_title";
    public static final String EXTRA_OFFER_LOGO = "extra_offer_logo";
    public static final String EXTRA_REWARD_COINS = "extra_reward_coins";
    public static final String EXTRA_CLICK_ID = "extra_click_id";

    private String redirectUrl;
    private String offerTitle;
    private String offerLogo;
    private long rewardCoins;
    private String clickId;

    private TextView txtCountdownNumber;
    private ProgressBar progressBarCountdown;
    private TextView txtStatusSubtitle;
    private CountDownTimer countDownTimer;
    private boolean hasRedirected = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redirect_countdown);
        makeFullScreen();

        redirectUrl = getIntent().getStringExtra(EXTRA_REDIRECT_URL);
        offerTitle = getIntent().getStringExtra(EXTRA_OFFER_TITLE);
        offerLogo = getIntent().getStringExtra(EXTRA_OFFER_LOGO);
        rewardCoins = getIntent().getLongExtra(EXTRA_REWARD_COINS, 0);
        clickId = getIntent().getStringExtra(EXTRA_CLICK_ID);

        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            Toast.makeText(this, "Destination URL unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        startCountdown();
    }

    private void initViews() {
        ImageView imgOfferLogo = findViewById(R.id.imgOfferLogo);
        TextView txtOfferTitle = findViewById(R.id.txtOfferTitle);
        TextView txtRewardBadge = findViewById(R.id.txtRewardBadge);
        txtCountdownNumber = findViewById(R.id.txtCountdownNumber);
        progressBarCountdown = findViewById(R.id.progressBarCountdown);
        txtStatusSubtitle = findViewById(R.id.txtStatusSubtitle);
        MaterialButton btnProceedNow = findViewById(R.id.btnProceedNow);

        findViewById(R.id.btnClose).setOnClickListener(v -> cancelAndFinish());

        if (offerTitle != null && !offerTitle.isEmpty()) {
            txtOfferTitle.setText(offerTitle);
        } else {
            txtOfferTitle.setText("Special Offer");
        }

        if (rewardCoins > 0) {
            txtRewardBadge.setText("Earn +" + String.format("%,d", rewardCoins) + " Coins");
            txtRewardBadge.setVisibility(View.VISIBLE);
        } else {
            txtRewardBadge.setVisibility(View.GONE);
        }

        if (offerLogo != null && !offerLogo.isEmpty()) {
            Glide.with(this)
                    .load(offerLogo)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(imgOfferLogo);
        }

        btnProceedNow.setOnClickListener(v -> proceedImmediately());
    }

    private void startCountdown() {
        progressBarCountdown.setMax(300);
        progressBarCountdown.setProgress(300);

        countDownTimer = new CountDownTimer(3100, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) Math.ceil((double) millisUntilFinished / 1000.0);
                if (secondsLeft < 1) secondsLeft = 1;

                String currentText = txtCountdownNumber.getText().toString();
                String newText = String.valueOf(secondsLeft);
                if (!newText.equals(currentText)) {
                    animateCountdownText(newText);
                }

                int progress = (int) ((millisUntilFinished / 3000.0) * 300.0);
                progressBarCountdown.setProgress(progress);

                if (txtStatusSubtitle != null) {
                    txtStatusSubtitle.setText("Redirecting to " + (offerTitle != null ? offerTitle : "partner offer") + " in " + secondsLeft + "s...");
                }
            }

            @Override
            public void onFinish() {
                progressBarCountdown.setProgress(0);
                proceedImmediately();
            }
        };

        countDownTimer.start();
    }

    private void animateCountdownText(String text) {
        txtCountdownNumber.setText(text);
        txtCountdownNumber.setScaleX(0.5f);
        txtCountdownNumber.setScaleY(0.5f);
        txtCountdownNumber.setAlpha(0.4f);
        txtCountdownNumber.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    txtCountdownNumber.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private synchronized void proceedImmediately() {
        if (hasRedirected) return;
        hasRedirected = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        try {
            // Save clickId locally for install attribution if available
            if (clickId != null && !clickId.isEmpty()) {
                InstallAttributionHelper.storeClickId(this, clickId);
            }

            // Launch browser to tracked URL
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(browserIntent);

            Toast.makeText(this, "🚀 Session activated for " + (offerTitle != null ? offerTitle : "Offer") + "! Complete task to earn coins.", Toast.LENGTH_LONG).show();

            // Finish countdown activity with slight delay so smooth transition occurs
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 500);

        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void cancelAndFinish() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void makeFullScreen() {
        try {
            Window window = getWindow();
            if (window == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            } else {
                View decor = window.getDecorView();
                if (decor != null) {
                    decor.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    );
                }
            }

            window.setStatusBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }
        } catch (Exception e) {
            android.util.Log.e("RedirectCountdown", "makeFullScreen error", e);
        }
    }
}
