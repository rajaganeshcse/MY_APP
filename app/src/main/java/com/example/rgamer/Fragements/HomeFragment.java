package com.example.rgamer.Fragements;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rgamer.R;
import com.example.rgamer.UserPref;
import com.example.rgamer.ads.AdsManager;
import com.example.rgamer.network.ApiClient;
import com.example.rgamer.network.ApiService;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.nativead.*;
import com.google.android.gms.ads.rewarded.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.*;

import okhttp3.ResponseBody;
import retrofit2.*;

public class HomeFragment extends Fragment {

    /* ================= UI ================= */

    CardView card_spinner,card_lucky_draw,card_tasks,card_surveys,cardInvite;

    private TextView txtCoins, txtToken, txtAdCount;
    private ImageView imgProfile, imgRewardCoin;
    private MaterialButton btnWatchNow;


    private AlertDialog loadingDialog;
    /* ================= ADS ================= */

    private RewardedAd rewardedAd;

    /* ================= DATA ================= */

    private FirebaseFirestore db;
    private UserPref userPref;
    private ApiService apiService;

    private static final int DAILY_LIMIT = 10;
    private int currentAds = 0;

    /* ================= LIFECYCLE ================= */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);


        init(view);

        loadUserData();
        listenUserRealtime();

        loadRewardAd();
        loadNativeAd(view);

        setupClickListeners();


        return view;
    }


    //progreess waitind daolod
    private void showLoading() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);

        loadingDialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(false) // user can't cancel
                .create();

        loadingDialog.show();
    }
    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    /* ================= INIT ================= */

    private void init(View view) {

        userPref = new UserPref(requireContext());
        db = FirebaseFirestore.getInstance();
        apiService = ApiClient.getClient().create(ApiService.class);

        txtCoins = view.findViewById(R.id.txtCoins);
        txtToken = view.findViewById(R.id.txtToken);
        txtAdCount = view.findViewById(R.id.txtAdCount);
        card_lucky_draw = view.findViewById(R.id.card_lucky_draw);

        imgProfile = view.findViewById(R.id.imgProfile);
        imgRewardCoin = view.findViewById(R.id.imgRewardCoin);

        btnWatchNow = view.findViewById(R.id.btnWatchNow);
        card_spinner = view.findViewById(R.id.card_spinner);

        cardInvite = view.findViewById(R.id.card_invite);
        card_tasks=view.findViewById(R.id.card_tasks);
        card_surveys=view.findViewById(R.id.card_surveys);
        card_surveys.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Comming Soon", Toast.LENGTH_SHORT).show();
        });

        card_tasks.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Comming Soon", Toast.LENGTH_SHORT).show();
        });
    }

    /// ads dailog
    private void showRewardDialogOnly(int reward) {
        Dialog d = new Dialog(requireContext());
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_spin_result);

        TextView txt = d.findViewById(R.id.txtWinAmount);
        MaterialButton ok = d.findViewById(R.id.btnOk);

        txt.setText("+" + reward + " Coins");

        ok.setOnClickListener(v -> d.dismiss());

        d.show();
    }

    /* ================= USER ================= */

    private void loadUserData() {

        txtCoins.setText(String.valueOf(userPref.getCoins()));
        txtToken.setText(String.valueOf(userPref.getWalletToken()));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(imgProfile);

        }
    }

    private void listenUserRealtime() {

        String uid = userPref.getUid();
        if (uid == null || uid.isEmpty()) return;

        db.collection("users").document(uid)
                .addSnapshotListener((value, error) -> {

                    if (value == null || !value.exists()) return;

                    Long coins = value.getLong("coins");
                    Long tickets = value.getLong("tickets");
                    Long ads = value.getLong("daily_ads_count");
                    String referalcode=value.getString("referralCode");
                    userPref.setReferralCode(referalcode);

                    if (coins != null) {
                        txtCoins.setText(String.valueOf(coins));
                        userPref.setCoins(coins);
                    }

                    if (tickets != null) {
                        txtToken.setText(String.valueOf(tickets));
                        userPref.setWalletToken(tickets.intValue());
                    }

                    currentAds = (ads != null) ? ads.intValue() : 0;

                    txtAdCount.setText(currentAds + "/" + DAILY_LIMIT);

                    updateButtonState();
                });
    }

    private void updateButtonState() {

        if (currentAds >= DAILY_LIMIT) {
            btnWatchNow.setEnabled(false);
            btnWatchNow.setText("Limit Reached");

        } else if (rewardedAd != null) {
            btnWatchNow.setEnabled(true);
            btnWatchNow.setText("Watch Ad");

        } else {
            btnWatchNow.setEnabled(false);
            btnWatchNow.setText("Loading...");
        }
    }

    /* ================= CLICK LISTENERS ================= */

    private void setupClickListeners() {

        // 🎡 Spin
        if (card_spinner != null) {
            card_spinner.setOnClickListener(v ->
                    startActivity(new Intent(
                            requireContext(),
                            com.example.rgamer.lucky_draw.activity_daily_spin.class
                    ))
            );
        }

        // 🎯 Lucky draw
        if (card_lucky_draw != null) {
            card_lucky_draw.setOnClickListener(v ->
                    startActivity(new Intent(
                            requireContext(),
                            com.example.rgamer.lucky_draw.activity_lucky_draw.class
                    ))
            );
        }

        // 🤝 Invite
        if (cardInvite != null) {
            cardInvite.setOnClickListener(v ->
                    startActivity(new Intent(
                            requireContext(),
                            com.example.rgamer.invite.activity_refer_earn.class
                    ))
            );
        }

        // 📺 Watch Ad
        btnWatchNow.setOnClickListener(v -> watchAd());
    }

    /* ================= WATCH AD ================= */

    private void watchAd() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(getContext(), "Login again", Toast.LENGTH_LONG).show();
            return;
        }

        if (currentAds >= DAILY_LIMIT) {
            Toast.makeText(getContext(), "Daily limit reached", Toast.LENGTH_LONG).show();
            return;
        }

        if (rewardedAd == null) {
            Toast.makeText(getContext(), "Ad not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        btnWatchNow.setEnabled(false);

        rewardedAd.show(requireActivity(), rewardItem -> {

            callRewardAPI();
            playRewardAnimation();
        });
    }

    /* ================= API ================= */

    private void callRewardAPI() {
        showLoading();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(result -> {

            String token = result.getToken();

            if (token == null || token.isEmpty()) {
                Toast.makeText(getContext(), "Token error", Toast.LENGTH_SHORT).show();
                btnWatchNow.setEnabled(true);
                return;
            }

            Map<String, String> body = new HashMap<>();
            body.put("requestId", UUID.randomUUID().toString());

            apiService.rewardAd(token.trim(), body)
                    .enqueue(new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {

                            btnWatchNow.setEnabled(true);

                            if (response.isSuccessful()) {

                                try {
                                    hideLoading();
                                    String res = response.body().string();
                                    Toast.makeText(getContext(), res, Toast.LENGTH_SHORT).show();
                                    showRewardDialogOnly(10);
                                } catch (Exception e) {
                                    hideLoading();
                                    Toast.makeText(getContext(), "Reward added", Toast.LENGTH_SHORT).show();
                                }

                            } else {

                                try {
                                    hideLoading();
                                    String err = response.errorBody().string();
                                    Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    hideLoading();
                                    Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {

                            btnWatchNow.setEnabled(true);
                            hideLoading();

                            Toast.makeText(getContext(),
                                    "Server error: " + t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

        });
    }

    /* ================= ANIMATION ================= */

    private void playRewardAnimation() {

        imgRewardCoin.setVisibility(View.VISIBLE);

        imgRewardCoin.setScaleX(0f);
        imgRewardCoin.setScaleY(0f);

        imgRewardCoin.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction(() ->
                        imgRewardCoin.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction(() ->
                                        imgRewardCoin.setVisibility(View.GONE)
                                )
                                .start()
                ).start();
    }

    /* ================= ADS ================= */

    private void loadRewardAd() {

        RewardedAd.load(
                requireContext(),
                AdsManager.REWARDED_AD_ID,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        updateButtonState();

                        rewardedAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {

                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        rewardedAd = null;
                                        loadRewardAd();
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                        updateButtonState();
                    }
                });
    }

    private void loadNativeAd(View rootView) {

        AdLoader adLoader = new AdLoader.Builder(
                requireContext(),
                AdsManager.NATIVE_AD_ID
        ).forNativeAd(ad -> {

            NativeAdView adView = rootView.findViewById(R.id.nativeAdView);
            if (adView == null) return;

            TextView headline = adView.findViewById(R.id.ad_headline);
            headline.setText(ad.getHeadline());

            adView.setNativeAd(ad);

        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }
}