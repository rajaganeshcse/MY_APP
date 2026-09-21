package com.app.rewardsplanet.Fragements;

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

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.withdraws.TransactionHistoryActivity;
import com.app.rewardsplanet.UserPref;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class RewardFragment extends Fragment {

    private TextView txtCoins;
    private UserPref userPref;
    private ListenerRegistration configListener;

    private View upiOption;
    private View bankOption;
    private View googleOption;
    private View amazonOption;
    private View phonepeOption;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_reward, container, false);

        txtCoins = view.findViewById(R.id.txtCoins);
        upiOption = view.findViewById(R.id.upiOption);
        bankOption = view.findViewById(R.id.bankOption);
        googleOption = view.findViewById(R.id.googleOption);
        amazonOption = view.findViewById(R.id.amazonOption);
        phonepeOption = view.findViewById(R.id.phonepeOption);

        if (getContext() != null) {
            userPref = new UserPref(getContext());
        }

        loadCoins();
        setupOptions(view);
        setupHistory(view);
        listenToRewardConfig();
        loadBannerAd(view);

        return view;
    }

    private void loadBannerAd(View view) {
        try {
            AdView adView = view.findViewById(R.id.adViewReward);
            if (adView != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
            }
        } catch (Exception ignored) {}
    }

    /* ================= LOAD COINS ================= */
    private void loadCoins() {
        if (userPref != null && txtCoins != null) {
            long coins = userPref.getCoins();
            txtCoins.setText(String.valueOf(coins));
        }
    }

    /* ================= DYNAMIC FIRESTORE REWARD CONFIG LISTENER ================= */
    private void listenToRewardConfig() {
        try {
            configListener = FirebaseFirestore.getInstance()
                    .collection("settings")
                    .document("reward_config")
                    .addSnapshotListener((snapshot, error) -> {
                        if (!isAdded() || error != null || snapshot == null || !snapshot.exists()) return;

                        Boolean upiEnabled = snapshot.getBoolean("upiEnabled");
                        Boolean bankEnabled = snapshot.getBoolean("bankEnabled");
                        Boolean googleEnabled = snapshot.getBoolean("googleEnabled");
                        Boolean amazonEnabled = snapshot.getBoolean("amazonEnabled");
                        Boolean phonepeEnabled = snapshot.getBoolean("phonepeEnabled");

                        if (upiOption != null) upiOption.setVisibility(Boolean.FALSE.equals(upiEnabled) ? View.GONE : View.VISIBLE);
                        if (bankOption != null) bankOption.setVisibility(Boolean.FALSE.equals(bankEnabled) ? View.GONE : View.VISIBLE);
                        if (googleOption != null) googleOption.setVisibility(Boolean.FALSE.equals(googleEnabled) ? View.GONE : View.VISIBLE);
                        if (amazonOption != null) amazonOption.setVisibility(Boolean.FALSE.equals(amazonEnabled) ? View.GONE : View.VISIBLE);
                        if (phonepeOption != null) phonepeOption.setVisibility(Boolean.FALSE.equals(phonepeEnabled) ? View.GONE : View.VISIBLE);
                    });
        } catch (Exception ignored) {}
    }

    /* ================= OPTIONS ================= */
    private void setupOptions(View view) {

        setupItem(view, R.id.upiOption,
                R.drawable.ic_upi,
                "UPI Withdraw",
                "Instant Cash Payout",
                RedeemFragment.UPI);

        setupItem(view, R.id.bankOption,
                R.drawable.ic_bank,
                "Bank Transfer",
                "Direct Bank Account Payout",
                RedeemFragment.BANK);

        setupItem(view, R.id.googleOption,
                R.drawable.ic_google_play,
                "Google Play Code",
                "Instant Voucher Code",
                RedeemFragment.GOOGLE);

        setupItem(view, R.id.amazonOption,
                R.drawable.ic_amazon,
                "Amazon Gift Card",
                "Instant Gift Voucher",
                RedeemFragment.AMAZON);

        setupItem(view, R.id.phonepeOption,
                R.drawable.ic_phonepe,
                "PhonePe Gift Card",
                "Instant PhonePe Code",
                RedeemFragment.PHONEPE);
    }

    /* ================= HISTORY (ACTIVITY) ================= */
    private void setupHistory(View view) {

        View btnHistory = view.findViewById(R.id.btnHistory);

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                if (!isAdded() || getContext() == null) return;
                Intent intent = new Intent(
                        getContext(),
                        TransactionHistoryActivity.class
                );
                startActivity(intent);
            });
        }
    }

    /* ================= SINGLE ITEM ================= */
    private void setupItem(View root, int id, int icon,
                           String title, String subtitle, String type) {

        View layout = root.findViewById(id);
        if (layout == null) return;

        ImageView img = layout.findViewById(R.id.icon);
        TextView txtTitle = layout.findViewById(R.id.title);
        TextView txtSubtitle = layout.findViewById(R.id.subtitle);

        if (img != null) img.setImageResource(icon);
        if (txtTitle != null) txtTitle.setText(title);
        if (txtSubtitle != null) txtSubtitle.setText(subtitle);

        layout.setOnClickListener(v -> openRedeem(type));
    }

    /* ================= OPEN REDEEM (FRAGMENT) ================= */
    private void openRedeem(String type) {
        if (!isAdded() || getActivity() == null) return;

        getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        RedeemFragment.newInstance(type)
                )
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (configListener != null) {
            configListener.remove();
            configListener = null;
        }
    }
}

