package com.app.rewardsplanet.Fragements;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.Activitys.MainActivity;
import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import com.google.android.material.button.MaterialButton;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.view.MotionEvent;
import android.widget.ProgressBar;

import com.app.rewardsplanet.utils.SuccessAnimationHelper;

public class HomeFragment extends Fragment {

    // =========================================================
    // UI
    // =========================================================

    private CardView card_spinner;
    private CardView card_lucky_draw;
    private CardView card_scratch;
    private CardView card_daily_quiz;
    private CardView card_hit_rewardz;
    private CardView card_tasks;
    private CardView card_surveys;
    private CardView cardInvite;
    private View card_watch;

    private TextView txtToken;
    private TextView txtAdCount;
    private TextView txtDailyQuizSubtitle;

    private ImageView imgRewardCoin;
    private ImageView strikeIcon;
    private ImageView menuIcon;

    private MaterialButton btnWatchNow;

    // DAILY BONUS HOLD-TO-CLAIM
    private MaterialButton btnClaimBonus;
    private TextView txtBonusInfo;
    private ProgressBar progressHoldBonus;

    private final Handler holdHandler = new Handler(Looper.getMainLooper());
    private long holdStartTime = 0;
    private static final long REQUIRED_HOLD_DURATION_MS = 5000;
    private boolean isHolding = false;
    private boolean isDailyBonusAvailable = false;
    private Runnable holdRunnable;

    private AlertDialog loadingDialog;


    // =========================================================
    // ADS
    // =========================================================

    private RewardedAd rewardedAd;


    // =========================================================
    // DATA
    // =========================================================

    private FirebaseFirestore db;

    private UserPref userPref;

    private ApiService apiService;

    private static final int DAILY_LIMIT = 10;

    private int currentAds = 0;

    // India timezone
    private static final ZoneId APP_ZONE =
            ZoneId.of("Asia/Kolkata");


    // =========================================================
    // CREATE VIEW
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );

        init(view);

        loadUserData();

        // Firebase realtime user data
        listenUserRealtime();

        // Directly check Daily Bonus status
        checkDailyBonusStatus();

        loadRewardAd();

        loadNativeAd(view);

        setupClickListeners();

        return view;
    }


    // =========================================================
    // NETWORK CHECK - ADDED ONLY
    // =========================================================

    private boolean isInternetAvailable() {

        if (!isAdded()) {
            return false;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        requireContext().getSystemService(
                                Context.CONNECTIVITY_SERVICE
                        );

        if (connectivityManager == null) {
            return false;
        }

        Network network =
                connectivityManager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(network);

        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
                && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
        );
    }


    // =========================================================
    // NETWORK ISSUE PAGE - ADDED ONLY
    // =========================================================

    @Override
    public void onResume() {
        super.onResume();

        if (!isAdded()) {
            return;
        }

        if (txtDailyQuizSubtitle != null && userPref != null) {
            if (!userPref.canPlayDailyQuiz()) {
                txtDailyQuizSubtitle.setText("Completed Today ✅");
                txtDailyQuizSubtitle.setTextColor(Color.parseColor("#4ADE80"));
            } else {
                txtDailyQuizSubtitle.setText("Answer & earn rewards");
                txtDailyQuizSubtitle.setTextColor(Color.WHITE);
            }
        }

        if (!isInternetAvailable()) {

            Intent intent = new Intent(
                    requireContext(),
                    com.app.rewardsplanet.NetworkIssueActivity.class
            );

            startActivity(intent);
        }
    }


    // =========================================================
    // INIT
    // =========================================================

    private void init(View view) {

        userPref = new UserPref(requireContext());

        db = FirebaseFirestore.getInstance();

        apiService = ApiClient
                .getClient()
                .create(ApiService.class);


        // =====================================================
        // MENU
        // =====================================================

        menuIcon = view.findViewById(
                R.id.menuIcon
        );


        // =====================================================
        // TEXT
        // =====================================================

        txtToken = view.findViewById(
                R.id.txtToken
        );

        txtAdCount = view.findViewById(
                R.id.txtAdCount
        );

        txtDailyQuizSubtitle = view.findViewById(
                R.id.txtDailyQuizSubtitle
        );


        // =====================================================
        // CARDS
        // =====================================================

        card_lucky_draw = view.findViewById(
                R.id.card_lucky_draw
        );

        card_scratch = view.findViewById(
                R.id.card_scratch
        );

        card_daily_quiz = view.findViewById(
                R.id.card_daily_quiz
        );

        card_spinner = view.findViewById(
                R.id.card_spinner
        );

        cardInvite = view.findViewById(
                R.id.card_invite
        );

        card_tasks = view.findViewById(
                R.id.card_task
        );

        card_surveys = view.findViewById(
                R.id.card_surveys
        );

        card_hit_rewardz = view.findViewById(
                R.id.card_hit_rewardz
        );


        // =====================================================
        // WATCH AD
        // =====================================================

        card_watch = view.findViewById(
                R.id.card_watch
        );

        btnWatchNow = view.findViewById(
                R.id.btnWatchNow
        );


        // =====================================================
        // DAILY BONUS
        // =====================================================

        btnClaimBonus = view.findViewById(
                R.id.btnClaimBonus
        );

        txtBonusInfo = view.findViewById(
                R.id.txtBonusInfo
        );

        progressHoldBonus = view.findViewById(
                R.id.progressHoldBonus
        );


        // =====================================================
        // OTHER UI
        // =====================================================

        imgRewardCoin = view.findViewById(
                R.id.coin
        );

        strikeIcon = view.findViewById(
                R.id.strikeIcon
        );
    }


    // =========================================================
    // CLICK LISTENERS
    // =========================================================

    private void setupClickListeners() {

        // =====================================================
        // MENU DRAWER
        // =====================================================

        setupMenuDrawer();


        // =====================================================
        // SPIN
        // =====================================================

        if (card_spinner != null) {

            card_spinner.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                requireContext(),
                                com.app.rewardsplanet
                                        .lucky_draw
                                        .activity_daily_spin.class
                        )
                );
            });
        }


        // =====================================================
        // SCRATCH
        // =====================================================

        if (card_scratch != null) {

            card_scratch.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                requireContext(),
                                com.app.rewardsplanet
                                        .lucky_draw
                                        .ScratchActivity.class
                        )
                );
            });
        }


        // =====================================================
        // DAILY QUIZ
        // =====================================================

        if (card_daily_quiz != null) {

            card_daily_quiz.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                requireContext(),
                                com.app.rewardsplanet
                                        .lucky_draw
                                        .QuizActivity.class
                        )
                );
            });
        }


        // =====================================================
        // HIT REWARDZ
        // =====================================================

        if (card_hit_rewardz != null) {

            card_hit_rewardz.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                requireContext(),
                                com.app.rewardsplanet
                                        .lucky_draw
                                        .HitRewardzActivity.class
                        )
                );
            });
        }


        // =====================================================
        // LUCKY DRAW
        // =====================================================

        if (card_lucky_draw != null) {

            card_lucky_draw.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                requireContext(),
                                com.app.rewardsplanet
                                        .lucky_draw
                                        .activity_lucky_draw.class
                        )
                );
            });
        }


        // =====================================================
        // INVITE
        // =====================================================

        if (cardInvite != null) {

            cardInvite.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                requireContext(),
                                com.app.rewardsplanet
                                        .invite
                                        .activity_refer_earn.class
                        )
                );
            });
        }


        // =====================================================
        // SURVEYS
        // =====================================================

        if (card_surveys != null) {

            card_surveys.setOnClickListener(v -> {

                Toast.makeText(
                        getContext(),
                        "Coming Soon",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }


        // =====================================================
        // TASKS
        // =====================================================

        if (card_tasks != null) {

            card_tasks.setOnClickListener(v -> {

                Toast.makeText(
                        getContext(),
                        "Coming Soon",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }


        // =====================================================
        // WATCH AD
        // =====================================================

        if (btnWatchNow != null) {
            btnWatchNow.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.app.rewardsplanet.lucky_draw.WatchVideoActivity.class));
            });
        }

        if (card_watch != null) {
            card_watch.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.app.rewardsplanet.lucky_draw.WatchVideoActivity.class));
            });
        }


        // =====================================================
        // DAILY BONUS (5-SECOND HOLD TO CLAIM)
        // =====================================================

        setupHoldToClaimListener();


        // =====================================================
        // STREAK
        // =====================================================

        if (strikeIcon != null) {

            strikeIcon.setOnClickListener(v -> {

                if (!isAdded()) {
                    return;
                }

                MainActivity activity =
                        (MainActivity) requireActivity();

                activity.selectNav(
                        activity.navHome
                );

                activity.loadFragment(
                        new StreakFragment()
                );
            });
        }
    }


    // =========================================================
    // MENU DRAWER
    // =========================================================

    private void setupMenuDrawer() {

        if (menuIcon == null) {
            return;
        }

        menuIcon.setOnClickListener(v -> {

            if (!isAdded()) {
                return;
            }

            MainActivity activity =
                    (MainActivity) requireActivity();

            activity.openDrawer();
        });
    }


    // =========================================================
    // LOADING DIALOG
    // =========================================================

    private void showLoading() {

        if (!isAdded()) {
            return;
        }

        View view = LayoutInflater
                .from(requireContext())
                .inflate(
                        R.layout.dialog_loading,
                        null
                );

        loadingDialog =
                new AlertDialog.Builder(
                        requireContext()
                )
                        .setView(view)
                        .setCancelable(false)
                        .create();

        loadingDialog.show();
    }


    private void hideLoading() {

        if (loadingDialog != null
                && loadingDialog.isShowing()) {

            loadingDialog.dismiss();
        }
    }


    // =========================================================
    // DAILY BONUS STATUS
    // =========================================================

    private void checkDailyBonusStatus() {

        if (!isAdded()) {
            return;
        }

        if (db == null || userPref == null) {
            return;
        }

        String uid = userPref.getUid();

        if (uid == null || uid.isEmpty()) {

            setDailyBonusCheckFailed();

            return;
        }


        if (btnClaimBonus != null) {

            btnClaimBonus.setEnabled(false);

            btnClaimBonus.setText(
                    "CHECKING..."
            );
        }

        if (txtBonusInfo != null) {

            txtBonusInfo.setText(
                    "Checking today's bonus..."
            );
        }


        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!isAdded()) {
                        return;
                    }

                    if (document == null
                            || !document.exists()) {

                        setDailyBonusAvailable();

                        return;
                    }


                    String today =
                            LocalDate
                                    .now(APP_ZONE)
                                    .toString();


                    Object claimDateObject =
                            document.get(
                                    "dailyBonusClaimDate"
                            );


                    String claimDate = null;


                    if (claimDateObject instanceof String) {

                        claimDate =
                                (String) claimDateObject;
                    }


                    else if (
                            claimDateObject
                                    instanceof Timestamp) {

                        Timestamp timestamp =
                                (Timestamp)
                                        claimDateObject;

                        claimDate =
                                timestamp
                                        .toDate()
                                        .toInstant()
                                        .atZone(APP_ZONE)
                                        .toLocalDate()
                                        .toString();
                    }


                    if (today.equals(claimDate)) {

                        setDailyBonusClaimed();

                    } else {

                        setDailyBonusAvailable();
                    }

                })
                .addOnFailureListener(e -> {

                    if (!isAdded()) {
                        return;
                    }

                    setDailyBonusCheckFailed();
                });
    }


    // =========================================================
    // HOLD TO CLAIM LISTENER (5 SECONDS)
    // =========================================================

    @SuppressLint("ClickableViewAccessibility")
    private void setupHoldToClaimListener() {
        if (btnClaimBonus == null) return;

        btnClaimBonus.setOnTouchListener((v, event) -> {
            if (!isDailyBonusAvailable) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startHoldProcess();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelHoldProcess();
                    return true;
            }
            return false;
        });
    }

    private void startHoldProcess() {
        if (isHolding) return;
        isHolding = true;
        holdStartTime = System.currentTimeMillis();

        triggerHaptic(100);

        if (btnClaimBonus != null) {
            btnClaimBonus.setTextColor(Color.parseColor("#FFFFFF"));
        }

        holdRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isHolding || !isAdded()) return;

                long elapsed = System.currentTimeMillis() - holdStartTime;
                float progress = Math.min(1.0f, (float) elapsed / REQUIRED_HOLD_DURATION_MS);
                int percent = Math.min(100, Math.round(progress * 100));
                float remainingSec = Math.max(0f, (REQUIRED_HOLD_DURATION_MS - elapsed) / 1000f);

                if (progressHoldBonus != null) {
                    progressHoldBonus.setProgress(percent);
                }

                if (btnClaimBonus != null) {
                    btnClaimBonus.setText(String.format("HOLDING... %d%%", percent));
                }

                if (txtBonusInfo != null) {
                    txtBonusInfo.setText(String.format("Keep holding... %.1fs left", remainingSec));
                }

                if (elapsed >= REQUIRED_HOLD_DURATION_MS) {
                    isHolding = false;
                    isDailyBonusAvailable = false;
                    triggerHaptic(250);

                    if (btnClaimBonus != null) {
                        btnClaimBonus.setText("CLAIMING...");
                        btnClaimBonus.setEnabled(false);
                    }

                    claimDailyBonus();
                } else {
                    holdHandler.postDelayed(this, 30);
                }
            }
        };

        holdHandler.post(holdRunnable);
    }

    private void cancelHoldProcess() {
        if (!isHolding) return;

        isHolding = false;
        if (holdRunnable != null) {
            holdHandler.removeCallbacks(holdRunnable);
        }

        if (isDailyBonusAvailable) {
            setDailyBonusAvailable();
        }
    }

    private void triggerHaptic(long ms) {
        try {
            if (getContext() != null) {
                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(ms);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================
    // DAILY BONUS AVAILABLE
    // =========================================================

    private void setDailyBonusAvailable() {

        if (!isAdded()) {
            return;
        }

        isDailyBonusAvailable = true;

        if (btnClaimBonus != null) {
            btnClaimBonus.setEnabled(true);
            btnClaimBonus.setText("HOLD TO CLAIM 0%");
            btnClaimBonus.setTextColor(Color.parseColor("#FFFFFF"));
        }

        if (progressHoldBonus != null) {
            progressHoldBonus.setProgress(0);
        }

        if (txtBonusInfo != null) {
            txtBonusInfo.setText("Hold for 5 seconds to claim");
        }
    }


    // =========================================================
    // DAILY BONUS CLAIMED
    // =========================================================

    private void setDailyBonusClaimed() {

        if (!isAdded()) {
            return;
        }

        isDailyBonusAvailable = false;

        if (btnClaimBonus != null) {
            btnClaimBonus.setEnabled(false);
            btnClaimBonus.setText("CLAIMED TODAY");
            btnClaimBonus.setTextColor(Color.parseColor("#000000"));
        }

        if (progressHoldBonus != null) {
            progressHoldBonus.setProgress(100);
        }

        if (txtBonusInfo != null) {
            txtBonusInfo.setText("Come back tomorrow for your next bonus");
        }
    }


    // =========================================================
    // DAILY BONUS CHECK FAILED
    // =========================================================

    private void setDailyBonusCheckFailed() {

        if (!isAdded()) {
            return;
        }

        isDailyBonusAvailable = false;

        if (btnClaimBonus != null) {
            btnClaimBonus.setEnabled(false);
            btnClaimBonus.setText("CHECK FAILED");
            btnClaimBonus.setTextColor(Color.parseColor("#F87171"));
        }

        if (txtBonusInfo != null) {
            txtBonusInfo.setText("Unable to check daily bonus");
        }
    }


    // =========================================================
    // REWARD DIALOG
    // =========================================================

    private void showRewardDialogOnly(
            int reward,
            long currentBalance) {

        if (!isAdded()) {
            return;
        }


        Dialog dialog =
                new Dialog(requireContext());

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );


        dialog.setContentView(
                R.layout.dialog_spin_result
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


        if (txtWinAmount != null) {

            txtWinAmount.setText(
                    "+" + reward + " Coins"
            );
        }


        if (txtCurrentBalance != null) {

            txtCurrentBalance.setText(
                    "Current Balance: "
                            + currentBalance
                            + " Coins"
            );
        }


        if (btnOk != null) {

            btnOk.setOnClickListener(
                    v -> dialog.dismiss()
            );
        }


        if (dialog.getWindow() != null) {

            dialog.getWindow()
                    .setBackgroundDrawable(
                            new ColorDrawable(
                                    Color.TRANSPARENT
                            )
                    );
        }

        SuccessAnimationHelper.animate(dialog);

        dialog.show();
    }


    // =========================================================
    // USER DATA
    // =========================================================

    private void loadUserData() {

        if (userPref == null) {
            return;
        }

        /*
         * Add your local UserPref values here
         * if required by your UI.
         */
    }


    // =========================================================
    // FIREBASE REALTIME
    // =========================================================

    private void listenUserRealtime() {

        if (!isAdded()) {
            return;
        }

        com.app.rewardsplanet.repository.UserRepository.getInstance(requireContext())
                .getUser()
                .observe(getViewLifecycleOwner(), user -> {

                    if (!isAdded() || user == null) {
                        return;
                    }

                    long tickets = user.getTickets();
                    currentAds = (int) user.getDaily_ads_count();

                    if (txtToken != null) {
                        txtToken.setText(String.valueOf(tickets));
                    }

                    if (txtAdCount != null) {
                        txtAdCount.setText(currentAds + "/" + DAILY_LIMIT);
                    }

                    updateButtonState();
                });
    }


    // =========================================================
    // WATCH BUTTON STATE
    // =========================================================

    private void updateButtonState() {

        if (btnWatchNow == null) {
            return;
        }

        if (currentAds >= DAILY_LIMIT) {

            btnWatchNow.setEnabled(false);

            btnWatchNow.setText(
                    "Limit Reached"
            );

        } else if (rewardedAd != null) {

            btnWatchNow.setEnabled(true);

            btnWatchNow.setText(
                    "Watch Ad"
            );

        } else {

            btnWatchNow.setEnabled(false);

            btnWatchNow.setText(
                    "Loading..."
            );
        }
    }


    // =========================================================
    // WATCH AD
    // =========================================================

    private void watchAd() {

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    getContext(),
                    "Login again",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (currentAds >= DAILY_LIMIT) {

            Toast.makeText(
                    getContext(),
                    "Daily limit reached",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (rewardedAd == null) {

            Toast.makeText(
                    getContext(),
                    "Ad not ready",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        btnWatchNow.setEnabled(false);


        rewardedAd.show(
                requireActivity(),
                rewardItem -> {

                    callRewardAPI();

                    playRewardAnimation();
                }
        );
    }


    // =========================================================
    // REWARDED AD API
    // =========================================================

    private void callRewardAPI() {

        showLoading();

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (user == null) {

            hideLoading();

            return;
        }


        user.getIdToken(true)
                .addOnSuccessListener(result -> {

                    String token =
                            result.getToken();


                    if (token == null
                            || token.isEmpty()) {

                        hideLoading();

                        if (btnWatchNow != null) {

                            btnWatchNow.setEnabled(
                                    true
                            );
                        }

                        Toast.makeText(
                                getContext(),
                                "Token error",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    Map<String, String> body =
                            new HashMap<>();

                    body.put(
                            "requestId",
                            UUID.randomUUID()
                                    .toString()
                    );


                    apiService
                            .rewardAd(
                                    token.trim(),
                                    body
                            )
                            .enqueue(
                                    new Callback<ResponseBody>() {

                                        @Override
                                        public void onResponse(
                                                Call<ResponseBody> call,
                                                Response<ResponseBody> response) {

                                            hideLoading();


                                            if (btnWatchNow != null) {

                                                btnWatchNow.setEnabled(
                                                        true
                                                );
                                            }


                                            if (response.isSuccessful()) {

                                                try {

                                                    String res =
                                                            response.body()
                                                                    .string();

                                                    Toast.makeText(
                                                            getContext(),
                                                            res,
                                                            Toast.LENGTH_SHORT
                                                    ).show();

                                                } catch (Exception e) {

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Reward added",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }

                                            } else {

                                                try {

                                                    String err =
                                                            response.errorBody()
                                                                    .string();

                                                    Toast.makeText(
                                                            getContext(),
                                                            err,
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                } catch (Exception e) {

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Error",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                            }
                                        }


                                        @Override
                                        public void onFailure(
                                                Call<ResponseBody> call,
                                                Throwable t) {

                                            hideLoading();

                                            if (btnWatchNow != null) {

                                                btnWatchNow.setEnabled(
                                                        true
                                                );
                                            }

                                            Toast.makeText(
                                                    getContext(),
                                                    "Server error: "
                                                            + t.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            );

                })
                .addOnFailureListener(e -> {

                    hideLoading();

                    if (btnWatchNow != null) {

                        btnWatchNow.setEnabled(
                                true
                        );
                    }

                    Toast.makeText(
                            getContext(),
                            "Authentication error",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =========================================================
    // DAILY BONUS
    // =========================================================

    private void claimDailyBonus() {

        FirebaseUser user =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();


        if (user == null) {

            Toast.makeText(
                    getContext(),
                    "Login again",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (btnClaimBonus != null) {

            btnClaimBonus.setEnabled(false);

            btnClaimBonus.setText(
                    "CLAIMING..."
            );
        }


        showLoading();


        user.getIdToken(true)
                .addOnSuccessListener(result -> {

                    String token =
                            result.getToken();


                    if (token == null
                            || token.isEmpty()) {

                        hideLoading();

                        resetDailyBonusButton();

                        Toast.makeText(
                                getContext(),
                                "Token error",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    Map<String, String> body =
                            new HashMap<>();

                    body.put(
                            "requestId",
                            UUID.randomUUID()
                                    .toString()
                    );


                    apiService
                            .claimDailyBonus(
                                    token.trim(),
                                    body
                            )
                            .enqueue(
                                    new Callback<ResponseBody>() {

                                        @Override
                                        public void onResponse(
                                                Call<ResponseBody> call,
                                                Response<ResponseBody> response) {

                                            hideLoading();


                                            if (response.isSuccessful()) {

                                                try {

                                                    if (response.body() == null) {

                                                        resetDailyBonusButton();

                                                        Toast.makeText(
                                                                getContext(),
                                                                "Empty server response",
                                                                Toast.LENGTH_LONG
                                                        ).show();

                                                        return;
                                                    }


                                                    String responseText =
                                                            response.body()
                                                                    .string();


                                                    JSONObject json =
                                                            new JSONObject(
                                                                    responseText
                                                            );


                                                    boolean success =
                                                            json.optBoolean(
                                                                    "success",
                                                                    false
                                                            );

                                                    int reward =
                                                            json.optInt(
                                                                    "reward",
                                                                    0
                                                            );

                                                    long coins =
                                                            json.optLong(
                                                                    "coins",
                                                                    0
                                                            );

                                                    String message =
                                                            json.optString(
                                                                    "message",
                                                                    "Daily bonus claimed"
                                                            );


                                                    if (success) {

                                                        setDailyBonusClaimed();

                                                        showRewardDialogOnly(
                                                                reward,
                                                                coins
                                                        );


                                                        if (txtBonusInfo != null) {

                                                            txtBonusInfo.setText(
                                                                    "+"
                                                                            + reward
                                                                            + " Coins earned"
                                                            );
                                                        }

                                                    } else {

                                                        resetDailyBonusButton();

                                                        Toast.makeText(
                                                                getContext(),
                                                                message,
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    }


                                                } catch (Exception e) {

                                                    resetDailyBonusButton();

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Invalid server response",
                                                            Toast.LENGTH_LONG
                                                    ).show();
                                                }


                                            } else {

                                                if (response.code() == 409) {

                                                    setDailyBonusClaimed();

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Daily bonus already claimed today",
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                    return;
                                                }


                                                resetDailyBonusButton();

                                                try {

                                                    if (response.errorBody() != null) {

                                                        String error =
                                                                response.errorBody()
                                                                        .string();

                                                        Toast.makeText(
                                                                getContext(),
                                                                error,
                                                                Toast.LENGTH_LONG
                                                        ).show();

                                                    } else {

                                                        Toast.makeText(
                                                                getContext(),
                                                                "Unable to claim bonus",
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    }

                                                } catch (Exception e) {

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Unable to claim bonus",
                                                            Toast.LENGTH_LONG
                                                    ).show();
                                                }
                                            }
                                        }


                                        @Override
                                        public void onFailure(
                                                Call<ResponseBody> call,
                                                Throwable t) {

                                            hideLoading();

                                            resetDailyBonusButton();

                                            Toast.makeText(
                                                    getContext(),
                                                    "Server error: "
                                                            + t.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            );

                })
                .addOnFailureListener(e -> {

                    hideLoading();

                    resetDailyBonusButton();

                    Toast.makeText(
                            getContext(),
                            "Authentication error",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =========================================================
    // RESET DAILY BONUS BUTTON
    // =========================================================

    private void resetDailyBonusButton() {

        if (btnClaimBonus == null) {
            return;
        }

        btnClaimBonus.setEnabled(true);

        btnClaimBonus.setText(
                "CLAIM BONUS NOW"
        );

        if (txtBonusInfo != null) {

            txtBonusInfo.setText(
                    "Your daily bonus is ready!"
            );
        }
    }


    // =========================================================
    // REWARD ANIMATION
    // =========================================================

    private void playRewardAnimation() {

        if (!isAdded() || imgRewardCoin == null) {
            return;
        }

        imgRewardCoin.setVisibility(
                View.VISIBLE
        );

        imgRewardCoin.setScaleX(0f);

        imgRewardCoin.setScaleY(0f);

        imgRewardCoin.setAlpha(1f);


        imgRewardCoin.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction(() ->
                        imgRewardCoin.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction(() ->
                                        imgRewardCoin
                                                .setVisibility(
                                                        View.GONE
                                                )
                                )
                                .start()
                )
                .start();
    }


    // =========================================================
    // LOAD REWARDED AD
    // =========================================================

    private void loadRewardAd() {

        RewardedAd.load(
                requireContext(),

                AdsManager.REWARDED_AD_ID,

                new AdRequest.Builder()
                        .build(),

                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(
                            @NonNull RewardedAd ad) {

                        rewardedAd = ad;

                        updateButtonState();


                        rewardedAd
                                .setFullScreenContentCallback(
                                        new FullScreenContentCallback() {

                                            @Override
                                            public void
                                            onAdDismissedFullScreenContent() {

                                                rewardedAd = null;

                                                loadRewardAd();
                                            }
                                        }
                                );
                    }


                    @Override
                    public void onAdFailedToLoad(
                            @NonNull LoadAdError error) {

                        rewardedAd = null;

                        updateButtonState();
                    }
                }
        );
    }


    // =========================================================
    // LOAD NATIVE AD
    // =========================================================

    private void loadNativeAd(
            View rootView) {

        AdLoader adLoader =
                new AdLoader.Builder(
                        requireContext(),
                        AdsManager.NATIVE_AD_ID
                )
                        .forNativeAd(ad -> {

                            NativeAdView adView =
                                    rootView.findViewById(
                                            R.id.nativeAdView
                                    );

                            if (adView == null) {
                                return;
                            }


                            TextView headline =
                                    adView.findViewById(
                                            R.id.ad_headline
                                    );

                            if (headline != null) {

                                headline.setText(
                                        ad.getHeadline()
                                );
                            }


                            adView.setNativeAd(ad);
                        })
                        .build();


        adLoader.loadAd(
                new AdRequest.Builder()
                        .build()
        );
    }


    // =========================================================
    // DESTROY VIEW
    // =========================================================

    @Override
    public void onDestroyView() {

        hideLoading();

        menuIcon = null;

        txtToken = null;

        txtAdCount = null;

        imgRewardCoin = null;

        strikeIcon = null;

        btnWatchNow = null;

        // DAILY BONUS
        btnClaimBonus = null;

        txtBonusInfo = null;

        card_spinner = null;

        card_lucky_draw = null;

        card_tasks = null;

        card_surveys = null;

        cardInvite = null;

        super.onDestroyView();
    }
}