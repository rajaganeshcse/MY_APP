package com.example.rgamer;

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

        return view;
    }

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
                });
    }

    private void setupOptions(View view) {

        setupItem(view, R.id.upiOption,
                R.drawable.ic_upi,
                "UPI Withdraw",
                RedeemFragment.UPI);

        setupItem(view, R.id.bankOption,
                R.drawable.ic_bank,
                "Bank Withdraw",
                RedeemFragment.BANK);

        setupItem(view, R.id.googleOption,
                R.drawable.ic_google_play,
                "Google Play",
                RedeemFragment.GOOGLE);
    }

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

        // ✅ THIS WILL NOT CRASH NOW
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container,
                        RedeemFragment.newInstance(type))
                .addToBackStack(null)
                .commit();
    }
}
