package com.example.rgamer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class layout_invite extends Fragment {

    TextView txtCode;
    ImageView btnCopy;
    EditText edtReferral;

    FirebaseFirestore db;
    String uid;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_layout_invite, container, false);

        txtCode = view.findViewById(R.id.txtReferralCode);
        btnCopy = view.findViewById(R.id.btnCopy);
        edtReferral = view.findViewById(R.id.edtReferral);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        loadReferralCode();

        btnCopy.setOnClickListener(v -> copyCode());

        view.findViewById(R.id.btnValidate)
                .setOnClickListener(v -> validateReferral());

        return view;
    }

    private void loadReferralCode() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.contains("referralCode")) {
                        String code = ReferralUtil.generateCode();
                        db.collection("users").document(uid)
                                .update("referralCode", code);
                        txtCode.setText(code);
                    } else {
                        txtCode.setText(doc.getString("referralCode"));
                    }
                });
    }

    private void copyCode() {
        ClipboardManager cm =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(
                ClipData.newPlainText("referral", txtCode.getText()));
        toast("Code copied");
    }

    private void validateReferral() {

        String code = edtReferral.getText().toString().trim();
        if (code.isEmpty()) return;

        db.collection("users")
                .whereEqualTo("referralCode", code)
                .get()
                .addOnSuccessListener(qs -> {

                    if (qs.isEmpty()) {
                        toast("Invalid referral code");
                        return;
                    }

                    DocumentSnapshot referrer = qs.getDocuments().get(0);
                    String refUid = referrer.getId();

                    if (refUid.equals(uid)) {
                        toast("You can't use your own code");
                        return;
                    }

                    applyReferral(refUid);
                });
    }

    private void applyReferral(String refUid) {

        DocumentReference userRef =
                db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {

            if (doc.getBoolean("referralUsed")) {
                toast("Referral already used");
                return;
            }

            WriteBatch batch = db.batch();

            // New user reward
            batch.update(userRef,
                    "referredBy", refUid,
                    "referralUsed", true,
                    "coins", FieldValue.increment(500),
                    "tickets", FieldValue.increment(10)
            );

            // Referrer reward
            batch.update(db.collection("users").document(refUid),
                    "coins", FieldValue.increment(500),
                    "tickets", FieldValue.increment(10)
            );

            // Referral record
            Map<String, Object> map = new HashMap<>();
            map.put("userId", uid);
            map.put("earnedCoins", 0);
            map.put("earnedTickets", 0);
            map.put("joinedAt", System.currentTimeMillis());

            batch.set(
                    db.collection("referrals")
                            .document(refUid)
                            .collection("users")
                            .document(uid),
                    map
            );

            batch.commit().addOnSuccessListener(aVoid ->
                    toast("Referral applied 🎉"));
        });
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
