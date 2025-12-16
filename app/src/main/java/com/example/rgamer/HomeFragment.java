package com.example.rgamer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    // Header
    TextView txtCoins, txtToken;
    ImageView imgProfile;

    // Cards
    CardView cardWatch, cardInvite;

    // Watch & Earn
    MaterialButton btnWatchNow;
    TextView txtAdCount;

    // Reward animation
    ImageView imgRewardCoin;

    // AdMob
    private RewardedAd rewardedAd;

    // Firebase
    FirebaseFirestore db;

    // Pref
    UserPref userPref;

    private static final int DAILY_LIMIT = 20;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        userPref = new UserPref(requireContext());
        db = FirebaseFirestore.getInstance();

        // Header
        txtCoins = view.findViewById(R.id.txtCoins);
        txtToken = view.findViewById(R.id.txtToken);
        imgProfile = view.findViewById(R.id.imgProfile);

        // Cards
        cardWatch = view.findViewById(R.id.card_watch_earn);
        cardInvite = view.findViewById(R.id.card_invite);

        // Watch card views
        View watchView = cardWatch;
        btnWatchNow = watchView.findViewById(R.id.btnWatchNow);
        txtAdCount = watchView.findViewById(R.id.txtAdCount);

        // Animation
        imgRewardCoin = view.findViewById(R.id.imgRewardCoin);

        loadUserData();
        updateAdUI();
        loadRewardAd();

        // Watch Ad
        btnWatchNow.setOnClickListener(v -> watchAd());

        // Watch card → Lucky Draw
        cardWatch.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), activity_lucky_draw.class));
        });

        // ✅ Invite Card → Refer & Earn
        cardInvite.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), activity_refer_earn.class));
        });

        return view;
    }

    /* ================= USER DATA ================= */
    private void loadUserData() {
        txtCoins.setText(String.valueOf(userPref.getCoins()));
        txtToken.setText(String.valueOf(userPref.getToken()));

        Glide.with(this)
                .load(userPref.getProfileImage())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgProfile);
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
                    "Ad not ready, try again",
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

    /* ================= LOAD AD ================= */
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

    /* ================= REWARD ================= */
    private void rewardUser() {
        int reward = 5;
        userPref.setCoins(userPref.getCoins() + reward);
        txtCoins.setText(String.valueOf(userPref.getCoins()));
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
                .withEndAction(() -> {
                    imgRewardCoin.setVisibility(View.GONE);
                })
                .start();
    }

    /* ================= UI ================= */
    private void updateAdUI() {
        int watched = userPref.getTodayAdCount();
        txtAdCount.setText(watched + "/" + DAILY_LIMIT + " ads watched");
        btnWatchNow.setEnabled(watched < DAILY_LIMIT);
    }

    /* ================= FIREBASE ================= */
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
}
