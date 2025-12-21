package com.example.rgamer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RewardFragment extends Fragment {

    private TextView txtCoins;

    // 🔹 Local cache
    private UserPref userPref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reward, container, false);

        txtCoins = view.findViewById(R.id.txtCoins);

        // ✅ INIT USER PREF
        userPref = new UserPref(requireContext());

        loadCoins();
        setupOptions(view);
        setupHistory(view);

        return view;
    }

    /* ================= LOAD COINS (FROM USERPREF) ================= */
    private void loadCoins() {
        long coins = userPref.getCoins();
        txtCoins.setText(String.valueOf(coins));
    }

    /* ================= OPTIONS ================= */
    private void setupOptions(View view) {

        // 🔹 CASH OPTIONS
        setupItem(view, R.id.upiOption,
                R.drawable.ic_upi,
                "UPI Withdraw",
                "Cash",
                RedeemFragment.UPI);

        setupItem(view, R.id.bankOption,
                R.drawable.ic_bank,
                "Bank Withdraw",
                "Cash",
                RedeemFragment.BANK);

        // 🔹 VOUCHER OPTIONS
        setupItem(view, R.id.googleOption,
                R.drawable.ic_google_play,
                "Google Play Voucher",
                "Voucher Code",
                RedeemFragment.GOOGLE);

        setupItem(view, R.id.amazonOption,
                R.drawable.ic_amazon,
                "Amazon Gift Voucher",
                "Voucher Code",
                RedeemFragment.AMAZON);

        setupItem(view, R.id.phonepeOption,
                R.drawable.ic_phonepe,
                "PhonePe Gift Voucher",
                "Voucher Code",
                RedeemFragment.PHONEPE);
    }

    /* ================= HISTORY ================= */
    private void setupHistory(View view) {
        LinearLayout btnHistory = view.findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        TransactionHistoryFragment.class))
        );
    }

    /* ================= SINGLE ITEM (WITH SUBTITLE) ================= */
    private void setupItem(View root, int id, int icon,
                           String title, String subtitle, String type) {

        LinearLayout layout = root.findViewById(id);
        if (layout == null) return;

        ImageView img = layout.findViewById(R.id.icon);
        TextView txtTitle = layout.findViewById(R.id.title);
        TextView txtSubtitle = layout.findViewById(R.id.subtitle);

        img.setImageResource(icon);
        txtTitle.setText(title);
        txtSubtitle.setText(subtitle);

        layout.setOnClickListener(v -> openRedeem(type));
    }

    /* ================= OPEN REDEEM ================= */
    private void openRedeem(String type) {

        // ❌ NO MINIMUM COIN CHECK

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        RedeemFragment.newInstance(type)
                )
                .addToBackStack(null)
                .commit();
    }
}
