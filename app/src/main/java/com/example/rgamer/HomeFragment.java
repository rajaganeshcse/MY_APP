package com.example.rgamer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    // Fragment Header
    TextView txtCoins, txtToken;
    ImageView imgProfile;

    // Cards
    CardView cardInvite;
    TextView txtLuckyDraw;

    // Watch & Earn
    MaterialButton btnWatchNow;
    TextView txtAdCount;

    // Reward animation
    ImageView imgRewardCoin;

    // Ads
    private RewardedAd rewardedAd;
    private NativeAd nativeAd;

    // Firebase & Pref
    FirebaseFirestore db;
    UserPref userPref;

    // Constants
    private static final int DAILY_LIMIT = 20;
    private static final int AD_REWARD_COINS = 10;
    private static final int AD_REWARD_TOKENS = 2;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        userPref = new UserPref(requireContext());
        db = FirebaseFirestore.getInstance();

        // Fragment Header
        txtCoins = view.findViewById(R.id.txtCoins);
        txtToken = view.findViewById(R.id.txtToken);
        imgProfile = view.findViewById(R.id.imgProfile);

        // Cards
        txtLuckyDraw = view.findViewById(R.id.txtLuckyDraw);
        cardInvite = view.findViewById(R.id.card_invite);

        // Watch & Earn
        btnWatchNow = view.findViewById(R.id.btnWatchNow);
        txtAdCount = view.findViewById(R.id.txtAdCount);

        imgRewardCoin = view.findViewById(R.id.imgRewardCoin);

        loadUserData();
        updateAdUI();
        loadRewardAd();
        loadNativeAd(view);

        btnWatchNow.setOnClickListener(v -> watchAd());

        txtLuckyDraw.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), activity_lucky_draw.class))
        );

        cardInvite.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), activity_refer_earn.class))
        );

        return view;
    }

    /* ================= USER DATA (GOOGLE PROFILE – FIXED) ================= */
    private void loadUserData() {

        txtCoins.setText(String.valueOf(userPref.getCoins()));
        txtToken.setText(String.valueOf(userPref.getWalletToken()));

        String profileUrl = userPref.getProfileImage();

        // 🔥 FirebaseAuth fallback (REAL SOURCE)
        if (profileUrl == null || profileUrl.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getPhotoUrl() != null) {
                profileUrl = user.getPhotoUrl().toString();
                userPref.setProfileImage(profileUrl);
            }
        }

        // Fragment profile image
        Glide.with(this)
                .load(profileUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgProfile);

        // ✅ MainActivity header profile image (NO ID CONFLICT)
        if (getActivity() != null) {
            ImageView headerProfile =
                    getActivity().findViewById(R.id.imgProfile);

            if (headerProfile != null) {
                Glide.with(this)
                        .load(profileUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(headerProfile);
            }
        }
    }

    /* ================= WATCH AD ================= */
    private void watchAd() {

        int watched = userPref.getTodayAdCount();

        if (watched >= DAILY_LIMIT) {
            Toast.makeText(getContext(),
                    "Daily ad limit reached",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (rewardedAd == null) {
            Toast.makeText(getContext(),
                    "Ad not ready",
                    Toast.LENGTH_SHORT).show();
            loadRewardAd();
            return;
        }

        btnWatchNow.setEnabled(false);

        rewardedAd.show(requireActivity(), rewardItem -> {
            rewardUser();
            playRewardAnimation();
            userPref.increaseAdCount();
            syncAdCountToFirebase();
            updateAdUI();
        });
    }

    /* ================= REWARD ================= */
    private void rewardUser() {
        addCoins(AD_REWARD_COINS);
        addTokens(AD_REWARD_TOKENS);
    }

    /* ================= ADD COINS ================= */
    private void addCoins(int coins) {

        int total = userPref.getCoins() + coins;
        userPref.setCoins(total);

        if (getActivity() != null) {
            TextView txt = getActivity().findViewById(R.id.txtCoins);
            if (txt != null) txt.setText(String.valueOf(total));
        }

        syncCoins();
    }

    private void syncCoins() {
        String uid = userPref.getUid();
        if (uid == null || uid.isEmpty()) return;

        db.collection("users")
                .document(uid)
                .update("coins", userPref.getCoins());
    }

    /* ================= ADD TOKENS ================= */
    private void addTokens(int tokens) {

        int total = userPref.getWalletToken() + tokens;
        userPref.setWalletToken(total);

        if (getActivity() != null) {
            TextView txt = getActivity().findViewById(R.id.txtToken);
            if (txt != null) txt.setText(String.valueOf(total));
        }

        syncTokens();
    }

    private void syncTokens() {
        String uid = userPref.getUid();
        if (uid == null || uid.isEmpty()) return;

        db.collection("users")
                .document(uid)
                .update("walletToken", userPref.getWalletToken());
    }

    /* ================= REWARD ANIMATION ================= */
    private void playRewardAnimation() {

        if (imgRewardCoin == null) return;

        imgRewardCoin.setVisibility(View.VISIBLE);
        imgRewardCoin.setScaleX(0f);
        imgRewardCoin.setScaleY(0f);
        imgRewardCoin.setAlpha(1f);

        imgRewardCoin.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(300)
                .withEndAction(() ->
                        imgRewardCoin.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction(() -> {
                                    imgRewardCoin.setVisibility(View.GONE);
                                    imgRewardCoin.setAlpha(1f);
                                })
                                .start()
                )
                .start();
    }

    /* ================= ADS ================= */
    private void loadRewardAd() {

        AdRequest request = new AdRequest.Builder().build();

        RewardedAd.load(
                requireContext(),
                "ca-app-pub-3940256099942544/5224354917",
                request,
                new RewardedAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;

                        rewardedAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        rewardedAd = null;
                                        btnWatchNow.setEnabled(true);
                                        loadRewardAd();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(
                                            @NonNull AdError adError) {
                                        rewardedAd = null;
                                        btnWatchNow.setEnabled(true);
                                        loadRewardAd();
                                    }
                                }
                        );
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                    }
                }
        );
    }

    private void loadNativeAd(View rootView) {

        AdLoader adLoader = new AdLoader.Builder(
                requireContext(),
                "ca-app-pub-3940256099942544/2247696110"
        )
                .forNativeAd(ad -> {

                    NativeAdView adView =
                            rootView.findViewById(R.id.nativeAdView);

                    if (adView == null) return;

                    nativeAd = ad;

                    TextView headline = adView.findViewById(R.id.ad_headline);
                    Button cta = adView.findViewById(R.id.ad_call_to_action);
                    MediaView media = adView.findViewById(R.id.ad_media);

                    headline.setText(ad.getHeadline());
                    adView.setHeadlineView(headline);

                    if (ad.getCallToAction() != null) {
                        cta.setText(ad.getCallToAction());
                        adView.setCallToActionView(cta);
                    } else {
                        cta.setVisibility(View.GONE);
                    }

                    adView.setMediaView(media);
                    adView.setNativeAd(ad);
                })
                .withAdListener(new AdListener() {})
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    /* ================= UI ================= */
    private void updateAdUI() {
        int watched = userPref.getTodayAdCount();
        txtAdCount.setText(watched + "/" + DAILY_LIMIT + " ads watched");
        btnWatchNow.setEnabled(watched < DAILY_LIMIT);
    }

    /* ================= FIREBASE (ADS COUNT) ================= */
    private void syncAdCountToFirebase() {

        String uid = userPref.getUid();
        if (uid == null || uid.isEmpty()) return;

        DocumentReference ref =
                db.collection("users").document(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("daily_ads.count", userPref.getTodayAdCount());
        data.put("daily_ads.updated_at", System.currentTimeMillis());

        ref.update(data);
    }

    /* ================= LIFECYCLE ================= */
    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (nativeAd != null) nativeAd.destroy();
    }
}
