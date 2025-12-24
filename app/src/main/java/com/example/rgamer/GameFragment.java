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
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

        MobileAds.initialize(requireContext());
        loadRewardAd();

        /* ================= HOW TO WIN ================= */
        view.findViewById(R.id.lytHowToWin)
                .setOnClickListener(v -> showHowToWinPopup());

        /* ================= TOURNAMENT CLICKS ================= */
        view.findViewById(R.id.cardFreeFire).setOnClickListener(v ->
                openTournament("freefire", "Free Fire", R.drawable.img_freefire));

        view.findViewById(R.id.cardPubg).setOnClickListener(v ->
                openTournament("pubg", "PUBG", R.drawable.img_pubg));

        view.findViewById(R.id.jackpot).setOnClickListener(v ->
                openTournament("jackpot", "Jackpot", R.drawable.ic_jackpot));

        view.findViewById(R.id.Ludo).setOnClickListener(v ->
                openTournament("ludo", "Ludo", R.drawable.ic_ludo));

        /* ================= DAILY BONUS ================= */
        View daily = view.findViewById(R.id.taskDailyBonus);
        Button dailyBtn = daily.findViewById(R.id.btnAction);
        checkDailyBonus(dailyBtn);
        daily.setOnClickListener(v -> claimDailyBonus(dailyBtn));

        /* ================= VIDEO TASK ================= */
        View video = view.findViewById(R.id.taskVideo);
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
                loadRewardAd();
            }
        });

        return view;
    }

    /* ================= OPEN TOURNAMENT ================= */
    private void openTournament(String gameId, String title, int banner) {
        Intent i = new Intent(getContext(), TournamentActivity.class);
        i.putExtra("game", gameId);      // Firestore filter
        i.putExtra("title", title);      // Header title
        i.putExtra("banner", banner);    // Header image
        startActivity(i);
    }

    /* ================= DAILY BONUS ================= */
    private void checkDailyBonus(Button dailyBtn) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = getTodayDate();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String claimedDate =
                            doc.getString("daily_bonus.claimed_date");
                    if (today.equals(claimedDate)) {
                        dailyBtn.setText("Claimed");
                        dailyBtn.setEnabled(false);
                    }
                });
    }

    private void claimDailyBonus(Button dailyBtn) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = getTodayDate();
        addCoins(50);
        db.collection("users").document(uid)
                .update("daily_bonus.claimed_date", today);
        dailyBtn.setEnabled(false);
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault()).format(new Date());
    }

    /* ================= COINS ================= */
    private void addCoins(int coins) {
        long total = userPref.getCoins() + coins;
        userPref.setCoins(total);
        db.collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .update("coins", total);
    }

    /* ================= ADS ================= */
    private void loadRewardAd() {
        RewardedAd.load(
                requireContext(),
                "ca-app-pub-3940256099942544/5224354917",
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                    }
                });
    }

    private void showHowToWinPopup() {
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());
        dialog.setContentView(
                LayoutInflater.from(getContext())
                        .inflate(R.layout.bottomsheet_how_to_win, null));
        dialog.show();
    }
}
