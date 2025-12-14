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

import com.example.rgamer.R;
import com.example.rgamer.TournamentActivity;
import com.example.rgamer.UserPref;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class GameFragment extends Fragment {

    UserPref userPref;
    FirebaseFirestore db;
    RewardedAd rewardedAd;

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

        /* HOW TO WIN */
        view.findViewById(R.id.lytHowToWin).setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Complete tasks & watch videos to earn coins",
                        Toast.LENGTH_SHORT).show()
        );

        /* TOURNAMENTS */
        view.findViewById(R.id.cardFreeFire).setOnClickListener(v ->
                openTournament("FreeFire")
        );

        view.findViewById(R.id.cardPubg).setOnClickListener(v ->
                openTournament("PUBG")
        );

        /* DAILY BONUS */
        View daily = view.findViewById(R.id.taskDailyBonus);
        TextView dailyTitle = daily.findViewById(R.id.txtTitle);
        Button dailyBtn = daily.findViewById(R.id.btnAction);

        dailyTitle.setText("Daily Bonus");
        dailyBtn.setText("Get");

        daily.setOnClickListener(v -> {
            if (userPref.canClaimDaily()) {
                addCoins(50);
                userPref.setDailyClaimed();
                Toast.makeText(getContext(),
                        "+50 Coins Added",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),
                        "Already claimed today",
                        Toast.LENGTH_SHORT).show();
            }
        });

        /* VIDEO TASK */
        View video = view.findViewById(R.id.taskVideo);
        TextView videoTitle = video.findViewById(R.id.txtTitle);
        Button videoBtn = video.findViewById(R.id.btnAction);
        ImageView videoIcon = video.findViewById(R.id.imgIcon);

        videoTitle.setText("Video Task");
        videoBtn.setText("Watch");
        videoIcon.setImageResource(R.drawable.ic_play);

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

    /* ADD COINS + UPDATE UI + FIREBASE */
    private void addCoins(int coins) {
        int total = userPref.getCoins() + coins;
        userPref.setCoins(total);

        if (getActivity() != null) {
            TextView txtCoins = getActivity().findViewById(R.id.txtCoins);
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

    /* REWARDED AD */
    private void loadRewardAd() {
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(
                requireContext(),
                "ca-app-pub-3940256099942544/5224354917", // TEST ID
                request,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                    }
                });
    }

    /* TOURNAMENT */
    private void openTournament(String game) {
        Intent i = new Intent(getContext(), TournamentActivity.class);
        i.putExtra("game", game);
        startActivity(i);
    }
}
