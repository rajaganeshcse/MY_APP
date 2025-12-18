package com.example.rgamer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RedeemFragment extends Fragment {

    // ================= TYPES =================
    public static final String TYPE = "type";
    public static final String UPI = "upi";
    public static final String BANK = "bank";
    public static final String FREE_FIRE = "free_fire";
    public static final String LORDS = "lords";
    public static final String GOOGLE = "google";

    private String redeemType = UPI;

    // ================= UI =================
    private TextView txtTitle, txtSubtitle, btnSubmit;
    private EditText edtInput, edtAccount, edtIfsc, edtCoins;

    // ================= FIREBASE =================
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // ================= CONSTRUCTOR =================
    public RedeemFragment() {}

    public static RedeemFragment newInstance(String type) {
        RedeemFragment fragment = new RedeemFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TYPE, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    // ================= LIFECYCLE =================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // ✅ CORRECT LAYOUT
        View view = inflater.inflate(R.layout.activity_redeem_fragment, container, false);

        // UI
        txtTitle = view.findViewById(R.id.txtTitle);
        txtSubtitle = view.findViewById(R.id.txtSubtitle);
        edtInput = view.findViewById(R.id.edtInput);
        edtAccount = view.findViewById(R.id.edtAccount);
        edtIfsc = view.findViewById(R.id.edtIfsc);
        edtCoins = view.findViewById(R.id.edtCoins);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ✅ LOGIN SAFETY (NO AUTO CLOSE)
        if (auth.getCurrentUser() == null) {
            toast("Please login again");
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
            return view;
        }

        // Get type
        if (getArguments() != null) {
            redeemType = getArguments().getString(TYPE, UPI);
        }

        setupUI();

        btnSubmit.setOnClickListener(v -> submitRedeem());

        return view;
    }

    // ================= UI SETUP =================
    private void setupUI() {

        // Reset visibility
        edtInput.setVisibility(View.VISIBLE);
        edtAccount.setVisibility(View.GONE);
        edtIfsc.setVisibility(View.GONE);

        switch (redeemType) {

            case UPI:
                txtTitle.setText("UPI Withdraw");
                txtSubtitle.setText("Enter your UPI ID");
                edtInput.setHint("example@upi");
                break;

            case BANK:
                txtTitle.setText("Bank Withdraw");
                txtSubtitle.setText("Enter bank details");
                edtInput.setVisibility(View.GONE);
                edtAccount.setVisibility(View.VISIBLE);
                edtIfsc.setVisibility(View.VISIBLE);
                break;

            case FREE_FIRE:
                txtTitle.setText("Free Fire Diamonds");
                txtSubtitle.setText("Enter Free Fire Player ID");
                edtInput.setHint("Free Fire UID");
                break;

            case LORDS:
                txtTitle.setText("Lords Mobile Diamonds");
                txtSubtitle.setText("Enter Player ID");
                edtInput.setHint("Player ID");
                break;

            case GOOGLE:
                txtTitle.setText("Google Play Redeem");
                txtSubtitle.setText("Enter your Email ID");
                edtInput.setHint("email@example.com");
                break;
        }
    }

    // ================= SUBMIT =================
    private void submitRedeem() {

        String coinStr = edtCoins.getText().toString().trim();

        if (TextUtils.isEmpty(coinStr)) {
            toast("Enter coins");
            return;
        }

        long coins;
        try {
            coins = Long.parseLong(coinStr);
        } catch (Exception e) {
            toast("Invalid coin value");
            return;
        }

        // ✅ MINIMUM 100 COINS
        if (coins < 100) {
            toast("Minimum 100 coins required");
            return;
        }

        String input = edtInput.getText().toString().trim();
        String account = edtAccount.getText().toString().trim();
        String ifsc = edtIfsc.getText().toString().trim();

        if (redeemType.equals(BANK)) {
            if (TextUtils.isEmpty(account) || TextUtils.isEmpty(ifsc)) {
                toast("Enter bank account & IFSC");
                return;
            }
        } else {
            if (TextUtils.isEmpty(input)) {
                toast("Enter required details");
                return;
            }
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("type", redeemType);
        data.put("coins", coins);
        data.put("status", "pending");
        data.put("created_at", FieldValue.serverTimestamp());

        if (redeemType.equals(BANK)) {
            data.put("account", account);
            data.put("ifsc", ifsc);
        } else {
            data.put("input", input);
        }

        db.collection("redeem_requests")
                .add(data)
                .addOnSuccessListener(d -> {
                    toast("Request submitted");
                    requireActivity()
                            .getSupportFragmentManager()
                            .popBackStack();
                })
                .addOnFailureListener(e ->
                        toast("Failed to submit request"));
    }

    // ================= TOAST =================
    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
