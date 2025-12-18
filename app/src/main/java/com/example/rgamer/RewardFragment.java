package com.example.rgamer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RewardFragment extends Fragment {

    private TextView txtCoins;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reward, container, false);

        txtCoins = view.findViewById(R.id.txtCoins);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadCoins();
        setupOptions(view);
        setupHistory(view);

        return view;
    }

    // ================= LOAD COINS =================
    private void loadCoins() {
        if (auth.getCurrentUser() == null) {
            txtCoins.setText("0");
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    Long coins = doc.getLong("coins");
                    txtCoins.setText(coins == null ? "0" : String.valueOf(coins));
                })
                .addOnFailureListener(e -> txtCoins.setText("0"));
    }

    // ================= OPTIONS =================
    private void setupOptions(View view) {

        setupItem(view, R.id.googleOption,
                R.drawable.ic_google_play,
                "Google Play Voucher",
                RedeemFragment.GOOGLE);

        setupItem(view, R.id.amazonOption,
                R.drawable.ic_amazon,
                "Amazon Gift Voucher",
                RedeemFragment.AMAZON);

        setupItem(view, R.id.phonepeOption,
                R.drawable.ic_phonepe,
                "PhonePe Gift Voucher",
                RedeemFragment.PHONEPE);

        setupItem(view, R.id.upiOption,
                R.drawable.ic_upi,
                "UPI Withdraw",
                RedeemFragment.UPI);

        setupItem(view, R.id.bankOption,
                R.drawable.ic_bank,
                "Bank Withdraw",
                RedeemFragment.BANK);
    }

    // ================= HISTORY =================
    private void setupHistory(View view) {
        LinearLayout btnHistory = view.findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        TransactionHistoryFragment.class))
        );
    }

    // ================= SINGLE ITEM =================
    private void setupItem(View root, int id, int icon,
                           String title, String type) {

        LinearLayout layout = root.findViewById(id);
        if (layout == null) return;

        ImageView img = layout.findViewById(R.id.icon);
        TextView t1 = layout.findViewById(R.id.title);

        img.setImageResource(icon);
        t1.setText(title);

        layout.setOnClickListener(v -> openRedeem(type));
    }

    // ================= OPEN REDEEM =================
    private void openRedeem(String type) {

        long coins;
        try {
            coins = Long.parseLong(txtCoins.getText().toString());
        } catch (Exception e) {
            coins = 0;
        }

        if (coins < 100) {
            Toast.makeText(getContext(),
                    "Minimum 100 coins required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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
