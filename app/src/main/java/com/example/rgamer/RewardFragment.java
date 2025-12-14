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

import com.example.rgamer.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class RewardFragment extends Fragment {

    // UI
    private TextView txtCoins;
    private LinearLayout btnHistory;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public RewardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reward, container, false);

        // Init UI
        txtCoins = view.findViewById(R.id.txtCoins);
        btnHistory = view.findViewById(R.id.btnHistory);

        // Init Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load coins from Firebase
        loadCoinsFromFirebase();

        // History click
        btnHistory.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Open Rewards History",
                        Toast.LENGTH_SHORT).show()
        );

        // Setup redeem options
        setupOptions(view);

        return view;
    }

    // 🔥 Load coins from Firestore
    private void loadCoinsFromFirebase() {

        if (auth.getCurrentUser() == null) {
            txtCoins.setText("0");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {
                        Long coins = documentSnapshot.getLong("coins");

                        if (coins != null) {
                            txtCoins.setText(String.valueOf(coins));
                        } else {
                            txtCoins.setText("0");
                        }
                    } else {
                        txtCoins.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    txtCoins.setText("0");
                    Toast.makeText(getContext(),
                            "Failed to load coins",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Redeem options
    private void setupOptions(View view) {

        setupItem(view, R.id.upiOption,
                R.drawable.ic_upi,
                "UPI Cash",
                "Redeem Money in UPI");

        setupItem(view, R.id.googleOption,
                R.drawable.ic_google_play,
                "Google Play Voucher",
                "Get redeem code using coins");

        setupItem(view, R.id.mlOption,
                R.drawable.ic_ff,
                "Free Fire",
                "Redeem Diamonds using coins");

        setupItem(view, R.id.lordsOption,
                R.drawable.ic_lords,
                "Lords Mobile Diamonds",
                "Redeem Diamonds using coins");
    }

    // Single item setup
    private void setupItem(View root,
                           int layoutId,
                           int icon,
                           String title,
                           String subtitle) {

        LinearLayout layout = root.findViewById(layoutId);
        ImageView img = layout.findViewById(R.id.icon);
        TextView t1 = layout.findViewById(R.id.title);
        TextView t2 = layout.findViewById(R.id.subtitle);

        img.setImageResource(icon);
        t1.setText(title);
        t2.setText(subtitle);

        layout.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        title + " clicked",
                        Toast.LENGTH_SHORT).show()
        );
    }
}
