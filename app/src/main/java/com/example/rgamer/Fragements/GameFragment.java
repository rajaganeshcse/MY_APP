package com.example.rgamer.Fragements;

import android.app.AlertDialog;
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
import androidx.fragment.app.Fragment;

import com.example.rgamer.R;
import com.example.rgamer.Game.TournamentActivity;
import com.example.rgamer.UserPref;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
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

        /* ================= ADS INIT ================= */
        MobileAds.initialize(requireContext());
        loadRewardAd();

        /* ================= HOW TO WIN ================= */
        view.findViewById(R.id.lytHowToWin)
                .setOnClickListener(v -> showHowToWinPopup());

        /* ================= TOURNAMENT ================= */
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

        ImageView dailyImg = daily.findViewById(R.id.imgIcon);
        TextView dailyTitle = daily.findViewById(R.id.txtTitle);
        MaterialButton dailyBtn = daily.findViewById(R.id.btnAction);

        dailyImg.setImageResource(R.drawable.ic_money);
        dailyTitle.setText("Daily Bonus");

        checkDailyBonus(dailyBtn);
        daily.setOnClickListener(v -> claimDailyBonus(dailyBtn));

        /* ================= VIDEO TASK ================= */
        View video = view.findViewById(R.id.taskVideo);

        ImageView videoImg = video.findViewById(R.id.imgIcon);
        TextView videoTitle = video.findViewById(R.id.txtTitle);
        MaterialButton videoBtn = video.findViewById(R.id.btnAction);

        videoImg.setImageResource(R.drawable.ic_watch);
        videoTitle.setText("Watch Video & Earn");
        videoBtn.setText("Watch");

        video.setOnClickListener(v -> {
            if (rewardedAd != null) {
                rewardedAd.show(requireActivity(), rewardItem -> {
                    addCoins(25);
                    loadRewardAd();
                });
            } else {
                Toast.makeText(
                        getContext(),
                        "Ad not ready, try again",
                        Toast.LENGTH_SHORT
                ).show();
                loadRewardAd();
            }
        });

        return view;
    }

    /* ================= TOURNAMENT ================= */
    private void openTournament(String gameId, String title, int banner) {
        Intent i = new Intent(getContext(), TournamentActivity.class);
        i.putExtra("game", gameId);
        i.putExtra("title", title);
        i.putExtra("banner", banner);
        startActivity(i);
    }

    /* ================= DAILY BONUS CHECK ================= */
    private void checkDailyBonus(MaterialButton dailyBtn) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = getTodayDate();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    String lastClaim =
                            doc.getString("daily_bonus.claimed_date");

                    if (today.equals(lastClaim)) {
                        dailyBtn.setText("Claimed");
                        dailyBtn.setEnabled(false);
                    } else {
                        dailyBtn.setText("Get");
                        dailyBtn.setEnabled(true);
                    }
                });
    }

    /* ================= DAILY BONUS CLAIM (ONCE PER DAY) ================= */
    private void claimDailyBonus(MaterialButton dailyBtn) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String today = getTodayDate();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    String lastClaim =
                            doc.getString("daily_bonus.claimed_date");

                    // 🔒 BLOCK MULTIPLE CLAIMS
                    if (today.equals(lastClaim)) {
                        Toast.makeText(
                                getContext(),
                                "Daily bonus already claimed",
                                Toast.LENGTH_SHORT
                        ).show();
                        dailyBtn.setText("Claimed");
                        dailyBtn.setEnabled(false);
                        return;
                    }

                    // ✅ ALLOW CLAIM
                    addCoins(50);

                    db.collection("users")
                            .document(uid)
                            .update("daily_bonus.claimed_date", today);

                    dailyBtn.setText("Claimed");
                    dailyBtn.setEnabled(false);
                });
    }

    private String getTodayDate() {
        return new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
        ).format(new Date());
    }

    /* ================= COINS ================= */
    private void addCoins(int coins) {
        long total = userPref.getCoins() + coins;
        userPref.setCoins(total);

        db.collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .update("coins", total);

        showSpinDialog(coins);
    }

    /* ================= SPIN / COIN DIALOG ================= */
    private void showSpinDialog(int coins) {

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_spin_result, null);

        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        TextView txtWinAmount =
                dialogView.findViewById(R.id.txtWinAmount);
        MaterialButton btnOk =
                dialogView.findViewById(R.id.btnOk);

        txtWinAmount.setText("+" + coins + " Coins");

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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

    /* ================= HOW TO WIN ================= */
    private void showHowToWinPopup() {
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());
        dialog.setContentView(
                LayoutInflater.from(getContext())
                        .inflate(R.layout.bottomsheet_how_to_win, null));
        dialog.show();
    }
}
