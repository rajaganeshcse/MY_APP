package com.app.rewardsplanet.lucky_draw;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.lucky_draw.ScratchView;
import com.app.rewardsplanet.network.ApiService;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.models.ScratchResponse;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScratchActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView txtCoins;
    private TextView txtScratchCount;
    private ScratchView scratchView;

    private ApiService apiService;

    private String userId;

    private int coins = 0;
    private int remaining = 0;

    private boolean requestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_card_scratch);

        // Firebase user
        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        userId = currentUser.getUid();

        // API
        apiService = ApiClient
                .getClient()
                .create(ApiService.class);

        // Views
        btnBack = findViewById(R.id.btnBack);
        txtCoins = findViewById(R.id.txtCoins);
        txtScratchCount = findViewById(R.id.txtScratchCount);
        scratchView = findViewById(R.id.scratchView);

        // Ads
        MobileAds.initialize(this, initializationStatus -> {
        });

        AdView adView = findViewById(R.id.adView);

        if (adView != null) {
            AdRequest adRequest =
                    new AdRequest.Builder().build();

            adView.loadAd(adRequest);
        }

        // Back
        btnBack.setOnClickListener(v -> finish());

        // Scratch listener
        scratchView.setScratchListener(
                this::onScratchCompleted
        );

        enableFullscreen();

        // Get today's status from backend
        loadScratchStatus();
    }

    // ============================================================
    // LOAD STATUS
    // ============================================================

    private void loadScratchStatus() {

        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                apiService
                        .getScratchStatus(bearerToken)
                        .enqueue(new Callback<ScratchResponse>() {

                            @Override
                            public void onResponse(
                                    Call<ScratchResponse> call,
                                    Response<ScratchResponse> response) {

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    ScratchResponse data =
                                            response.body();

                                    coins = data.getCoins();
                                    remaining = data.getRemaining();

                                    updateUI();

                                    scratchView.setScratchEnabled(
                                            remaining > 0
                                    );

                                } else {

                                    Toast.makeText(
                                            ScratchActivity.this,
                                            "Unable to load scratch status",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<ScratchResponse> call,
                                    Throwable t) {

                                Toast.makeText(
                                        ScratchActivity.this,
                                        "Server connection failed",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ScratchActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================================
    // SCRATCH COMPLETED
    // ============================================================

    private void onScratchCompleted() {

        if (requestInProgress) {
            return;
        }

        if (remaining <= 0) {

            scratchView.setScratchEnabled(false);

            Toast.makeText(
                    this,
                    "Today's scratch limit reached",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        requestInProgress = true;

        // Prevent another scratch while API request is running
        scratchView.setScratchEnabled(false);

        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                // Backend decides the reward
                apiService
                        .playScratch(bearerToken)
                        .enqueue(new Callback<ScratchResponse>() {

                            @Override
                            public void onResponse(
                                    Call<ScratchResponse> call,
                                    Response<ScratchResponse> response) {

                                requestInProgress = false;

                                if (response.isSuccessful()
                                        && response.body() != null) {

                                    ScratchResponse data =
                                            response.body();

                            // Server rejected the scratch
                            if (!data.isAllowed()) {

                                coins = data.getCoins();
                                remaining = data.getRemaining();

                                updateUI();

                                scratchView.resetScratch();

                                scratchView.setScratchEnabled(
                                        remaining > 0
                                );

                                Toast.makeText(
                                        ScratchActivity.this,
                                        data.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            // Server reward
                            int reward =
                                    data.getReward();

                            coins =
                                    data.getCoins();

                            remaining =
                                    data.getRemaining();

                            updateUI();

                            // Give server reward to ScratchView
                            scratchView.setReward(reward);

                            // Reveal card using server reward
                            scratchView.revealCardFromBackend();

                            // Show result
                            showWinDialog(
                                    reward,
                                    coins
                            );

                        } else {

                            // API failed
                            scratchView.setScratchEnabled(true);

                            Toast.makeText(
                                    ScratchActivity.this,
                                    "Scratch failed. Please try again.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<ScratchResponse> call,
                            Throwable t) {

                        requestInProgress = false;

                        // Don't consume scratch on network failure
                        scratchView.setScratchEnabled(true);

                        Toast.makeText(
                                ScratchActivity.this,
                                "Network error. Please try again.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                requestInProgress = false;
                scratchView.setScratchEnabled(true);
                Toast.makeText(ScratchActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ============================================================
    // UPDATE UI
    // ============================================================

    private void updateUI() {

        if (txtCoins != null) {

            txtCoins.setText(
                    String.valueOf(coins)
            );
        }

        if (txtScratchCount != null) {

            txtScratchCount.setText(
                    "Your Today Scratch Count left = "
                            + remaining
            );
        }
    }

    // ============================================================
    // WIN DIALOG
    // ============================================================

    private void showWinDialog(
            int reward,
            int currentBalance) {

        Dialog dialog =
                new Dialog(this);

        dialog.setContentView(
                R.layout.dialog_spin_result
        );

        Window window =
                dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawable(
                    new ColorDrawable(
                            Color.TRANSPARENT
                    )
            );
        }

        TextView txtTitle =
                dialog.findViewById(
                        R.id.txtTitle
                );

        TextView txtWinAmount =
                dialog.findViewById(
                        R.id.txtWinAmount
                );

        TextView txtCurrentBalance =
                dialog.findViewById(
                        R.id.txtCurrentBalance
                );

        MaterialButton btnOk =
                dialog.findViewById(
                        R.id.btnOk
                );

        if (txtTitle != null) {

            txtTitle.setText(
                    "YOU WON!"
            );
        }

        if (txtWinAmount != null) {

            txtWinAmount.setText(
                    "+" + reward + " COINS"
            );
        }

        if (txtCurrentBalance != null) {

            txtCurrentBalance.setText(
                    "Current Balance: "
                            + currentBalance
            );
        }

        if (btnOk != null) {

            btnOk.setOnClickListener(v -> {

                dialog.dismiss();

                // More scratches available
                if (remaining > 0) {

                    scratchView.resetScratch();

                    scratchView.setScratchEnabled(
                            true
                    );

                } else {

                    scratchView.setScratchEnabled(
                            false
                    );
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
    // FULLSCREEN
    // ============================================================

    private void enableFullscreen() {

        Window window =
                getWindow();

        if (window == null) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.R) {

            WindowInsetsController controller =
                    window.getInsetsController();

            if (controller != null) {

                controller.hide(
                        android.view.WindowInsets.Type.statusBars()
                                | android.view.WindowInsets.Type.navigationBars()
                );
            }

        } else {

            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        enableFullscreen();
    }
}

