package com.app.rewardsplanet.lucky_draw;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.network.ApiService;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.models.ScratchResponse;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScratchActivity extends AppCompatActivity {

    // ── Views ──
    private ImageView   btnBack;
    private TextView    txtCoins;
    private TextView    txtScratchCount;
    private ScratchView scratchView;

    // ── API / prefs ──
    private ApiService apiService;
    private UserPref   userPref;

    // ── State ──
    private String  userId;
    private int     coins             = 0;
    private int     remaining         = 0;
    private boolean requestInProgress = false;

    // ── Rewarded Ad ──
    private RewardedAd rewardedAd         = null;
    private boolean    adLoading          = false;
    private int        scratchCountForAd  = 0;   // show ad every 2 successful scratches
    private static final int AD_EVERY_N   = 2;

    // Pending win-dialog data (held until ad finishes)
    private int     pendingReward  = 0;
    private int     pendingBalance = 0;
    private boolean apiResultReady = false;
    private boolean fingerReleased = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_scratch);

        // Firebase user check
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId   = currentUser.getUid();
        userPref = new UserPref(this);

        // API
        apiService = ApiClient.getClient().create(ApiService.class);

        // Views
        btnBack         = findViewById(R.id.btnBack);
        txtCoins        = findViewById(R.id.txtCoins);
        txtScratchCount = findViewById(R.id.txtScratchCount);
        scratchView     = findViewById(R.id.scratchView);

        // Banner ad
        MobileAds.initialize(this, status -> {});
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.loadAd(new AdRequest.Builder().build());
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Scratch listener
        scratchView.setScratchListener(new ScratchView.ScratchListener() {
            @Override
            public void onScratchStart() {
                startScratchApiCall();
            }

            @Override
            public void onScratchComplete() {
                onScratchReleased();
            }
        });

        makeFullScreen();

        // ── STEP 1: Show local count immediately ──
        loadLocalCountFirst();

        // ── STEP 2: Sync from backend silently ──
        loadScratchStatus();

        // ── Pre-load first rewarded ad ──
        loadRewardedAd();
    }

    // ============================================================
    // STEP 1 — LOCAL FIRST (instant, no flicker)
    // ============================================================

    private void loadLocalCountFirst() {
        coins     = (int) userPref.getCoins();
        remaining = userPref.getTodayScratchRemaining();
        int lastReward = userPref.getLastScratchReward();
        updateUI();
        if (remaining <= 0) {
            if (lastReward > 0) scratchView.setReward(lastReward);
            scratchView.setLimitReached(true);
        } else {
            scratchView.setLimitReached(false);
            scratchView.setScratchEnabled(true);
        }
    }

    // ============================================================
    // STEP 2 — BACKEND SYNC
    // ============================================================

    private void loadScratchStatus() {
        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(
            new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {
                @Override
                public void onSuccess(String bearerToken) {
                    apiService.getScratchStatus(bearerToken)
                        .enqueue(new Callback<ScratchResponse>() {
                            @Override
                            public void onResponse(Call<ScratchResponse> call,
                                                   Response<ScratchResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    ScratchResponse data = response.body();
                                    coins     = data.getCoins();
                                    remaining = data.getRemaining();
                                    userPref.saveScratchRemaining(remaining);
                                    userPref.setCoins(coins);
                                    updateUI();
                                    int lastReward = userPref.getLastScratchReward();
                                    if (remaining <= 0) {
                                        if (lastReward > 0) scratchView.setReward(lastReward);
                                        scratchView.setLimitReached(true);
                                    } else {
                                        scratchView.setLimitReached(false);
                                        scratchView.setScratchEnabled(true);
                                    }
                                }
                            }
                            @Override
                            public void onFailure(Call<ScratchResponse> call, Throwable t) {}
                        });
                }
                @Override
                public void onError(Exception e) {}
            }
        );
    }

    // ============================================================
    // SCRATCH API & FINGER RELEASE FLOW
    // ============================================================

    private void startScratchApiCall() {
        if (requestInProgress || remaining <= 0) return;

        requestInProgress = true;
        apiResultReady    = false;
        fingerReleased    = false;

        // Optimistic local decrement — instant feedback
        remaining = Math.max(0, remaining - 1);
        userPref.saveScratchRemaining(remaining);
        updateUI();

        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(
            new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {
                @Override
                public void onSuccess(String bearerToken) {
                    apiService.playScratch(bearerToken)
                        .enqueue(new Callback<ScratchResponse>() {
                            @Override
                            public void onResponse(Call<ScratchResponse> call,
                                                   Response<ScratchResponse> response) {
                                requestInProgress = false;

                                if (response.isSuccessful() && response.body() != null) {
                                    ScratchResponse data = response.body();

                                    if (!data.isAllowed()) {
                                        // Server overrides — sync authoritative count
                                        coins     = data.getCoins();
                                        remaining = data.getRemaining();
                                        userPref.saveScratchRemaining(remaining);
                                        userPref.setCoins(coins);
                                        updateUI();
                                        scratchView.resetScratch();
                                        scratchView.setScratchEnabled(remaining > 0);
                                        Toast.makeText(ScratchActivity.this,
                                            data.getMessage(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    // ── Success ──
                                    int reward = data.getReward();
                                    coins      = data.getCoins();
                                    remaining  = data.getRemaining();

                                    userPref.saveScratchRemaining(remaining);
                                    userPref.saveLastScratchReward(reward);
                                    userPref.setCoins(coins);
                                    com.app.rewardsplanet.repository.UserRepository
                                            .getInstance(ScratchActivity.this)
                                            .refreshCurrentUser();

                                    updateUI();
                                    scratchView.setReward(reward);
                                    scratchView.invalidate();

                                    scratchCountForAd++;
                                    pendingReward  = reward;
                                    pendingBalance = coins;
                                    apiResultReady = true;

                                    // If user already released finger, show dialog / ad now
                                    if (fingerReleased) {
                                        triggerWinFlow();
                                    }

                                } else {
                                    // API error — roll back optimistic decrement
                                    remaining++;
                                    userPref.saveScratchRemaining(remaining);
                                    updateUI();
                                    scratchView.setScratchEnabled(true);
                                    Toast.makeText(ScratchActivity.this,
                                        "Scratch failed. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ScratchResponse> call, Throwable t) {
                                requestInProgress = false;
                                remaining++;
                                userPref.saveScratchRemaining(remaining);
                                updateUI();
                                scratchView.setScratchEnabled(true);
                                Toast.makeText(ScratchActivity.this,
                                    "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
                @Override
                public void onError(Exception e) {
                    requestInProgress = false;
                    remaining++;
                    userPref.saveScratchRemaining(remaining);
                    updateUI();
                    scratchView.setScratchEnabled(true);
                    Toast.makeText(ScratchActivity.this,
                        "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void onScratchReleased() {
        fingerReleased = true;
        if (apiResultReady) {
            triggerWinFlow();
        }
    }

    private void triggerWinFlow() {
        if (scratchCountForAd % AD_EVERY_N == 0 && rewardedAd != null) {
            showRewardedAd();
        } else {
            showWinDialog(pendingReward, pendingBalance);
        }
    }

    // ============================================================
    // REWARDED AD — LOAD
    // ============================================================

    private void loadRewardedAd() {
        if (adLoading) return;
        adLoading = true;

        String adUnitId;
        try {
            adUnitId = getString(R.string.rewarded_ad_unit_id);
        } catch (Exception e) {
            adUnitId = "ca-app-pub-3940256099942544/5224354917";
        }
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            adUnitId = "ca-app-pub-3940256099942544/5224354917";
        }

        RewardedAd.load(
            this,
            adUnitId,
            new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    rewardedAd = ad;
                    adLoading  = false;
                    setupRewardedAdCallbacks();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    rewardedAd = null;
                    adLoading  = false;
                    // No toast — fail silently, win dialog will show directly
                }
            }
        );
    }

    private void setupRewardedAdCallbacks() {
        if (rewardedAd == null) return;
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                // Ad closed — show the win dialog now
                rewardedAd = null;
                loadRewardedAd(); // pre-load next ad
                showWinDialog(pendingReward, pendingBalance);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                loadRewardedAd();
                // Show win dialog even if ad failed
                showWinDialog(pendingReward, pendingBalance);
            }
        });
    }

    // ============================================================
    // REWARDED AD — SHOW
    // ============================================================

    private void showRewardedAd() {
        if (rewardedAd == null) {
            // Ad not ready — go straight to win dialog
            showWinDialog(pendingReward, pendingBalance);
            return;
        }
        rewardedAd.show(this, rewardItem -> {
            // User earned the reward — nothing extra needed here
            // (our own backend already credited coins from playScratch)
        });
    }

    // ============================================================
    // UPDATE UI
    // ============================================================

    private void updateUI() {
        if (txtCoins != null) {
            txtCoins.setText(String.valueOf(coins));
        }
        if (txtScratchCount != null) {
            txtScratchCount.setText(String.valueOf(remaining));
        }
    }

    // ============================================================
    // WIN DIALOG
    // ============================================================

    private void showWinDialog(int reward, int currentBalance) {
        if (isFinishing() || isDestroyed()) return;

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_spin_result);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView       txtTitle          = dialog.findViewById(R.id.txtTitle);
        TextView       txtWinAmount      = dialog.findViewById(R.id.txtWinAmount);
        TextView       txtCurrentBalance = dialog.findViewById(R.id.txtCurrentBalance);
        MaterialButton btnOk             = dialog.findViewById(R.id.btnOk);

        if (txtTitle != null)          txtTitle.setText("YOU WON! 🎉");
        if (txtWinAmount != null)      txtWinAmount.setText("+" + reward + " COINS");
        if (txtCurrentBalance != null) txtCurrentBalance.setText("Balance: " + currentBalance + " coins");

        if (btnOk != null) {
            btnOk.setOnClickListener(v -> {
                dialog.dismiss();
                if (remaining > 0) {
                    scratchView.setLimitReached(false);
                    scratchView.resetScratch();
                    scratchView.setScratchEnabled(true);
                } else {
                    scratchView.setLimitReached(true);
                }
            });
        }

        dialog.show();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    @Override
    protected void onResume() {
        super.onResume();
        makeFullScreen();

        // If user went to another activity and came back,
        // reset any previously revealed card so old reward isn't shown
        if (scratchView != null) {
            if (remaining <= 0) {
                int lastReward = userPref.getLastScratchReward();
                if (lastReward > 0) scratchView.setReward(lastReward);
                scratchView.setLimitReached(true);
            } else {
                scratchView.setLimitReached(false);
                scratchView.resetIfRevealed();
                scratchView.setScratchEnabled(true);
            }
        }
    }

    // ============================================================
    // FULLSCREEN & TRANSPARENT SYSTEM BARS
    // ============================================================

    private void makeFullScreen() {
        Window window = getWindow();
        if (window == null) return;

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
