package com.app.rewardsplanet.Fragements;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
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
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // =========================================================
    // UI
    // =========================================================

    private CardView card_spinner;
    private CardView card_lucky_draw;
    private CardView card_tasks;
    private CardView card_surveys;
    private CardView cardInvite;

    private TextView txtToken;
    private TextView txtAdCount;

    private ImageView imgRewardCoin;
    private ImageView strikeIcon;

    // MENU ICON
    private ImageView menuIcon;

    private MaterialButton btnWatchNow;

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

        listenUserRealtime();

        loadRewardAd();

        loadNativeAd(view);

        setupClickListeners();

        return view;
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

        // -----------------------------------------------------
        // MENU
        // -----------------------------------------------------

        menuIcon = view.findViewById(
                R.id.menuIcon
        );

        // -----------------------------------------------------
        // TEXT
        // -----------------------------------------------------

        txtToken = view.findViewById(
                R.id.txtToken
        );

        txtAdCount = view.findViewById(
                R.id.txtAdCount
        );

        // -----------------------------------------------------
        // CARDS
        // -----------------------------------------------------

        card_lucky_draw = view.findViewById(
                R.id.card_lucky_draw
        );

        card_spinner = view.findViewById(
                R.id.card_spinner
        );

        cardInvite = view.findViewById(
                R.id.card_invite
        );

        card_tasks = view.findViewById(
                R.id.card_tasks
        );

        card_surveys = view.findViewById(
                R.id.card_surveys
        );

        // -----------------------------------------------------
        // OTHER UI
        // -----------------------------------------------------

        btnWatchNow = view.findViewById(
                R.id.btnWatchNow
        );

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

        // -----------------------------------------------------
        // MENU DRAWER
        // -----------------------------------------------------

        setupMenuDrawer();

        // -----------------------------------------------------
        // SPIN
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // LUCKY DRAW
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // INVITE
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // SURVEYS
        // -----------------------------------------------------

        if (card_surveys != null) {

            card_surveys.setOnClickListener(v -> {

                Toast.makeText(
                        getContext(),
                        "Coming Soon",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        // -----------------------------------------------------
        // TASKS
        // -----------------------------------------------------

        if (card_tasks != null) {

            card_tasks.setOnClickListener(v -> {

                Toast.makeText(
                        getContext(),
                        "Coming Soon",
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        // -----------------------------------------------------
        // WATCH AD
        // -----------------------------------------------------

        if (btnWatchNow != null) {

            btnWatchNow.setOnClickListener(
                    v -> watchAd()
            );
        }

        // -----------------------------------------------------
        // STREAK
        // -----------------------------------------------------

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
    // REWARD DIALOG
    // =========================================================

    private void showRewardDialogOnly(int reward) {

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

        TextView txt =
                dialog.findViewById(
                        R.id.txtWinAmount
                );

        MaterialButton ok =
                dialog.findViewById(
                        R.id.btnOk
                );

        if (txt != null) {

            txt.setText(
                    "+" + reward + " Coins"
            );
        }

        if (ok != null) {

            ok.setOnClickListener(
                    v -> dialog.dismiss()
            );
        }

        if (dialog.getWindow() != null) {

            dialog.getWindow()
                    .setBackgroundDrawableResource(
                            android.R.color.transparent
                    );
        }

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

        if (userPref == null
                || db == null) {
            return;
        }

        String uid = userPref.getUid();

        if (uid == null
                || uid.isEmpty()) {

            return;
        }

        db.collection("users")
                .document(uid)
                .addSnapshotListener(
                        (value, error) -> {

                            if (!isAdded()) {
                                return;
                            }

                            if (value == null
                                    || !value.exists()) {

                                return;
                            }

                            // -------------------------------------------------
                            // COINS
                            // -------------------------------------------------

                            Long coins =
                                    value.getLong(
                                            "coins"
                                    );

                            // -------------------------------------------------
                            // TICKETS
                            // -------------------------------------------------

                            Long tickets =
                                    value.getLong(
                                            "tickets"
                                    );

                            // -------------------------------------------------
                            // DAILY ADS
                            // -------------------------------------------------

                            Long ads =
                                    value.getLong(
                                            "daily_ads_count"
                                    );

                            // -------------------------------------------------
                            // REFERRAL CODE
                            // -------------------------------------------------

                            String referralCode =
                                    value.getString(
                                            "referralCode"
                                    );

                            userPref.setReferralCode(
                                    referralCode
                            );

                            // -------------------------------------------------
                            // COINS
                            // -------------------------------------------------

                            if (coins != null) {

                                /*
                                 * If your TextView displays coins,
                                 * update it here.
                                 */
                            }

                            // -------------------------------------------------
                            // TICKETS
                            // -------------------------------------------------

                            if (tickets != null) {

                                if (txtToken != null) {

                                    txtToken.setText(
                                            String.valueOf(
                                                    tickets
                                            )
                                    );
                                }

                                userPref.setWalletToken(
                                        tickets.intValue()
                                );
                            }

                            // -------------------------------------------------
                            // DAILY ADS
                            // -------------------------------------------------

                            currentAds =
                                    ads != null
                                            ? ads.intValue()
                                            : 0;

                            if (txtAdCount != null) {

                                txtAdCount.setText(
                                        currentAds
                                                + "/"
                                                + DAILY_LIMIT
                                );
                            }

                            updateButtonState();
                        }
                );
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
    // REWARD API
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

                        Toast.makeText(
                                getContext(),
                                "Token error",
                                Toast.LENGTH_SHORT
                        ).show();

                        if (btnWatchNow != null) {
                            btnWatchNow.setEnabled(true);
                        }

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
                                                Response<ResponseBody> response
                                        ) {

                                            if (btnWatchNow != null) {
                                                btnWatchNow.setEnabled(
                                                        true
                                                );
                                            }

                                            if (response.isSuccessful()) {

                                                try {

                                                    hideLoading();

                                                    String res =
                                                            response.body()
                                                                    .string();

                                                    Toast.makeText(
                                                            getContext(),
                                                            res,
                                                            Toast.LENGTH_SHORT
                                                    ).show();

                                                    showRewardDialogOnly(
                                                            10
                                                    );

                                                } catch (Exception e) {

                                                    hideLoading();

                                                    Toast.makeText(
                                                            getContext(),
                                                            "Reward added",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }

                                            } else {

                                                try {

                                                    hideLoading();

                                                    String err =
                                                            response.errorBody()
                                                                    .string();

                                                    Toast.makeText(
                                                            getContext(),
                                                            err,
                                                            Toast.LENGTH_LONG
                                                    ).show();

                                                } catch (Exception e) {

                                                    hideLoading();

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
                                                Throwable t
                                        ) {

                                            if (btnWatchNow != null) {
                                                btnWatchNow.setEnabled(
                                                        true
                                                );
                                            }

                                            hideLoading();

                                            Toast.makeText(
                                                    getContext(),
                                                    "Server error: "
                                                            + t.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            );
                });
    }

    // =========================================================
    // REWARD ANIMATION
    // =========================================================

    private void playRewardAnimation() {

        if (imgRewardCoin == null) {
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
                            @NonNull RewardedAd ad
                    ) {

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
                            @NonNull LoadAdError error
                    ) {

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
            View rootView
    ) {

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

        menuIcon = null;

        txtToken = null;

        txtAdCount = null;

        imgRewardCoin = null;

        strikeIcon = null;

        btnWatchNow = null;

        card_spinner = null;

        card_lucky_draw = null;

        card_tasks = null;

        card_surveys = null;

        cardInvite = null;

        super.onDestroyView();
    }
}