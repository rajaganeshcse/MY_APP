package com.app.rewardsplanet.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.models.UserModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MyEarningsFragment extends Fragment {

    TextView txtCoins, txtTickets;
    MaterialButton btnClaim;

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

                    boolean canClaim = referralCoins > 0 || referralTickets > 0;
                    btnClaim.setEnabled(canClaim);
                    if (canClaim) {
                        btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
                        btnClaim.setTextColor(Color.WHITE);
                    } else {
                        btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
                        btnClaim.setTextColor(Color.parseColor("#64748B"));
                    }
                });
    }

    // ================= CLAIM =================

    private void claimReward() {

        if (referralCoins <= 0 && referralTickets <= 0) {
            toast("Nothing to claim");
            return;
        }

        long coinsClaimed = referralCoins;
        long ticketsClaimed = referralTickets;

        btnClaim.setEnabled(false);
        btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));

        db.collection("users")
                .document(uid)
                .update(
                        "coins", FieldValue.increment(referralCoins),
                        "tickets", FieldValue.increment(referralTickets),
                        "totalReferralCoins", 0,
                        "totalReferralTickets", 0
                )
                .addOnSuccessListener(unused ->
                        toast("🎉 Claimed +" + coinsClaimed + " Coins & +" + ticketsClaimed + " Tickets!"))
                .addOnFailureListener(e -> {
                    btnClaim.setEnabled(true);
                    btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
                    toast("Claim failed");
                });
    }

    // ================= HELPERS =================

    private void setZero() {
        if (!isAdded()) return;
        txtCoins.setText("0");
        txtTickets.setText("0");
        btnClaim.setEnabled(false);
        btnClaim.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
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
