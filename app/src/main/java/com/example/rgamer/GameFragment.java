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

import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameFragment extends Fragment {

    private UserPref userPref;
    private FirebaseFirestore db;
    private RewardedAd rewardedAd;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_game, container, false);

        userPref = new UserPref(requireContext());
        db = FirebaseFirestore.getInstance();

        // Initialize AdMob once
        MobileAds.initialize(requireContext());
        loadRewardAd();

        /* ================= HOW TO WIN ================= */
        view.findViewById(R.id.lytHowToWin)
                .setOnClickListener(v -> showHowToWinPopup());

        /* ================= TOURNAMENTS ================= */
        view.findViewById(R.id.cardFreeFire).setOnClickListener(v ->
                openTournament("FreeFire"));

        view.findViewById(R.id.cardPubg).setOnClickListener(v ->
                openTournament("PUBG"));

        view.findViewById(R.id.jackpot).setOnClickListener(v ->
                openTournament("JackPot"));

        view.findViewById(R.id.Ludo).setOnClickListener(v ->
                openTournament("Ludo"));

        /* ================= DAILY BONUS ================= */
        View daily = view.findViewById(R.id.taskDailyBonus);
        TextView dailyTitle = daily.findViewById(R.id.txtTitle);
        Button dailyBtn = daily.findViewById(R.id.btnAction);

        dailyTitle.setText("Daily Bonus");

        // 🔹 Check Firebase daily claim state
        checkDailyBonus(dailyBtn);

        daily.setOnClickListener(v -> claimDailyBonus(dailyBtn));

        /* ================= VIDEO TASK ================= */
        View video = view.findViewById(R.id.taskVideo);
        TextView videoTitle = video.findViewById(R.id.txtTitle);
        Button videoBtn = video.findViewById(R.id.btnAction);
        ImageView videoIcon = video.findViewById(R.id.imgIcon);

        videoTitle.setText("Video Task");
        videoBtn.setText("Watch");
        videoIcon.setImageResource(R.drawable.ic_watch);

        video.setOnClickListener(v -> {
            if (rewardedAd != null) {
                rewardedAd.show(requireActivity(), rewardItem -> {
                    addCoins(25);
                    Toast.makeText(getContext(),
                            "+25 Coins Added",
                            Toast.LENGTH_SHORT).show();
                    loadRewardAd();
                });
            } else {
                Toast.makeText(getContext(),
                        "Ad not ready, try again",
                        Toast.LENGTH_SHORT).show();
                loadRewardAd();
            }
        });

        return view;
    }

    /* ================= DAILY BONUS LOGIC ================= */

    private void checkDailyBonus(Button dailyBtn) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = getTodayDate();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String claimedDate =
                            doc.getString("daily_bonus.claimed_date");

                    if (today.equals(claimedDate)) {
                        dailyBtn.setText("Claimed");
                        dailyBtn.setEnabled(false);
                    } else {
                        dailyBtn.setText("Get");
                        dailyBtn.setEnabled(true);
                    }
                });
    }

    private void claimDailyBonus(Button dailyBtn) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = getTodayDate();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    String claimedDate =
                            doc.getString("daily_bonus.claimed_date");

                    if (today.equals(claimedDate)) {
                        Toast.makeText(getContext(),
                                "Daily bonus already claimed",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ✅ Give reward
                    addCoins(50);

                    // ✅ Save claim date to Firebase
                    db.collection("users")
                            .document(uid)
                            .update("daily_bonus.claimed_date", today);

                    dailyBtn.setText("Claimed");
                    dailyBtn.setEnabled(false);

                    Toast.makeText(getContext(),
                            "+50 Coins Added",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getTodayDate() {
        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());
    }

    /* ================= ADD COINS (UNCHANGED) ================= */
    private void addCoins(int coins) {
        int total = userPref.getCoins() + coins;
        userPref.setCoins(total);

        if (getActivity() != null) {
            TextView txtCoins =
                    getActivity().findViewById(R.id.txtCoins);
            if (txtCoins != null) {
                txtCoins.setText(String.valueOf(total));
            }
        }

        syncCoins();
    }

    private void syncCoins() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .update("coins", userPref.getCoins());
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
                    }

                    @Override
                    public void onAdFailedToLoad(
                            @NonNull com.google.android.gms.ads.LoadAdError error) {
                        rewardedAd = null;
                    }
                });
    }

    /* ================= POPUP ================= */
    private void showHowToWinPopup() {
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottomsheet_how_to_win, null);

        dialog.setContentView(sheetView);

        ImageView btnClose = sheetView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /* ================= TOURNAMENT ================= */
    private void openTournament(String game) {
        Intent i = new Intent(getContext(), TournamentActivity.class);
        i.putExtra("game", game);
        startActivity(i);
    }
}
