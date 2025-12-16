package com.example.rgamer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rgamer.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public class MyEarningsFragment extends Fragment {

    TextView txtCoins, txtTickets;

    FirebaseFirestore db;
    String uid;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_layout_my_earnings, container, false);

        txtCoins = view.findViewById(R.id.txtCoins);
        txtTickets = view.findViewById(R.id.txtTickets);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        loadEarnings();

        view.findViewById(R.id.btnClaim)
                .setOnClickListener(v -> claimReward());

        return view;
    }

    private void loadEarnings() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    txtCoins.setText(
                            String.valueOf(doc.getLong("totalReferralCoins")));
                    txtTickets.setText(
                            String.valueOf(doc.getLong("totalReferralTickets")));
                });
    }

    private void claimReward() {

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {

                    int coins =
                            doc.getLong("totalReferralCoins").intValue();
                    int tickets =
                            doc.getLong("totalReferralTickets").intValue();

                    if (coins == 0 && tickets == 0) {
                        toast("Nothing to claim");
                        return;
                    }

                    db.collection("users").document(uid)
                            .update(
                                    "coins", FieldValue.increment(coins),
                                    "tickets", FieldValue.increment(tickets),
                                    "totalReferralCoins", 0,
                                    "totalReferralTickets", 0
                            );

                    toast("Reward claimed 🎉");
                    loadEarnings();
                });
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
