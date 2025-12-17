package com.example.rgamer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rgamer.models.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MyEarningsFragment extends Fragment {

    TextView txtCoins, txtTickets;
    Button btnClaim;

    FirebaseFirestore db;
    String uid;

    long referralCoins = 0;
    long referralTickets = 0;

    ListenerRegistration listener;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_layout_my_earnings,
                container,
                false
        );

        txtCoins = view.findViewById(R.id.txtCoins);
        txtTickets = view.findViewById(R.id.txtTickets);
        btnClaim = view.findViewById(R.id.btnClaim);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            toast("User not logged in");
            return view;
        }

        listenEarningsRealtime();

        btnClaim.setOnClickListener(v -> claimReward());

        return view;
    }

    // ================= REAL-TIME FIREBASE =================

    private void listenEarningsRealtime() {

        listener = db.collection("users")
                .document(uid)
                .addSnapshotListener((snapshot, error) -> {

                    if (!isAdded() || error != null || snapshot == null || !snapshot.exists()) {
                        setZero();
                        return;
                    }

                    UserModel user = snapshot.toObject(UserModel.class);
                    if (user == null) {
                        setZero();
                        return;
                    }

                    referralCoins = user.getTotalReferralCoins();
                    referralTickets = user.getTotalReferralTickets();

                    txtCoins.setText(String.valueOf(referralCoins));
                    txtTickets.setText(String.valueOf(referralTickets));

                    btnClaim.setEnabled(referralCoins > 0 || referralTickets > 0);
                });
    }

    // ================= CLAIM =================

    private void claimReward() {

        if (referralCoins <= 0 && referralTickets <= 0) {
            toast("Nothing to claim");
            return;
        }

        btnClaim.setEnabled(false);

        db.collection("users")
                .document(uid)
                .update(
                        "coins", FieldValue.increment(referralCoins),
                        "tickets", FieldValue.increment(referralTickets),
                        "totalReferralCoins", 0,
                        "totalReferralTickets", 0
                )
                .addOnSuccessListener(unused ->
                        toast("Reward claimed 🎉"))
                .addOnFailureListener(e -> {
                    btnClaim.setEnabled(true);
                    toast("Claim failed");
                });
    }

    // ================= HELPERS =================

    private void setZero() {
        if (!isAdded()) return;
        txtCoins.setText("0");
        txtTickets.setText("0");
        btnClaim.setEnabled(false);
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }
}
