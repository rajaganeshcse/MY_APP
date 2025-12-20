package com.example.rgamer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
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

    /* ================= TYPES ================= */
    public static final String TYPE = "type";
    public static final String GOOGLE = "google";
    public static final String AMAZON = "amazon";
    public static final String PHONEPE = "phonepe";
    public static final String UPI = "upi";
    public static final String BANK = "bank";

    private String redeemType = GOOGLE;

    /* ================= UI ================= */
    private TextView txtTitle, txtCoins;
    private GridLayout gridLayout;

    /* ================= FIREBASE ================= */
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    /* ================= LOCAL ================= */
    private UserPref userPref;
    private long userCoins = 0;

    /* ================= INSTANCE ================= */
    public static RedeemFragment newInstance(String type) {
        RedeemFragment f = new RedeemFragment();
        Bundle b = new Bundle();
        b.putString(TYPE, type);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_redeem_options, container, false);

        txtTitle = view.findViewById(R.id.txtTitle);
        txtCoins = view.findViewById(R.id.txtCoins);
        gridLayout = view.findViewById(R.id.gridLayout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userPref = new UserPref(requireContext());

        if (getArguments() != null) {
            redeemType = getArguments().getString(TYPE, GOOGLE);
        }

        setupHeader();
        loadCoins();      // ✅ FROM USERPREF
        setupCards();

        return view;
    }

    /* ================= HEADER ================= */
    private void setupHeader() {
        switch (redeemType) {
            case GOOGLE:
                txtTitle.setText("Google Play Voucher");
                break;
            case AMAZON:
                txtTitle.setText("Amazon Gift Voucher");
                break;
            case PHONEPE:
                txtTitle.setText("PhonePe Gift Voucher");
                break;
            case UPI:
                txtTitle.setText("UPI Withdraw");
                break;
            case BANK:
                txtTitle.setText("Bank Withdraw");
                break;
        }
    }

    /* ================= LOAD COINS (USERPREF) ================= */
    private void loadCoins() {
        userCoins = userPref.getCoins();   // ✅ LOCAL CACHE
        txtCoins.setText(String.valueOf(userCoins));
    }

    /* ================= CARDS ================= */
    private void setupCards() {

        gridLayout.removeAllViews();

        if (redeemType.equals(GOOGLE) ||
                redeemType.equals(AMAZON) ||
                redeemType.equals(PHONEPE)) {

            int icon =
                    redeemType.equals(AMAZON) ? R.drawable.ic_amazon :
                            redeemType.equals(PHONEPE) ? R.drawable.ic_phonepe :
                                    R.drawable.ic_google_play;

            addCard(icon, 1000, "₹10");
            addCard(icon, 3500, "₹35");
            addCard(icon, 5000, "₹50");
            addCard(icon, 10000, "₹100");

        } else if (redeemType.equals(UPI)) {

            addCard(R.drawable.ic_upi, 1174, "₹10");
            addCard(R.drawable.ic_upi, 2674, "₹25");
            addCard(R.drawable.ic_upi, 10000, "₹100");

        } else if (redeemType.equals(BANK)) {

            addCard(R.drawable.ic_bank, 10000, "₹100");
            addCard(R.drawable.ic_bank, 20000, "₹200");
        }
    }

    private void addCard(int icon, long cost, String amount) {

        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_redeem_card, gridLayout, false);

        ((ImageView) card.findViewById(R.id.imgIcon)).setImageResource(icon);
        ((TextView) card.findViewById(R.id.txtCoinCost))
                .setText(String.valueOf(cost));
        ((TextView) card.findViewById(R.id.txtAmount))
                .setText(amount);

        card.setOnClickListener(v -> {
            if (userCoins < cost) {
                toast("Not enough coins");
            } else {
                submitRedeem(cost, amount);
            }
        });

        gridLayout.addView(card);
    }

    /* ================= SUBMIT REDEEM ================= */
    private void submitRedeem(long coinsUsed, String amount) {

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        db.runTransaction(transaction -> {

            var userRef = db.collection("users").document(uid);
            var snap = transaction.get(userRef);

            Long current = snap.getLong("coins");
            if (current == null || current < coinsUsed)
                throw new RuntimeException("Insufficient coins");

            String name = snap.getString("name");

            long updated = current - coinsUsed;

            // 🔹 UPDATE FIRESTORE
            transaction.update(userRef, "coins", updated);

            // 🔹 CREATE REQUEST
            var requestRef = db.collection("redeem_requests").document();

            Map<String, Object> req = new HashMap<>();
            req.put("uid", uid);
            req.put("name", name);
            req.put("email", email);
            req.put("type", redeemType);
            req.put("type_label", getRewardName(redeemType));
            req.put("amount", amount);
            req.put("coins", coinsUsed);
            req.put("status", "pending");
            req.put("voucher_code", "");
            req.put("created_at", FieldValue.serverTimestamp());

            transaction.set(requestRef, req);

            return new Object[]{updated, requestRef.getId()};

        }).addOnSuccessListener(result -> {

            long updatedCoins = (long) ((Object[]) result)[0];
            String requestId = (String) ((Object[]) result)[1];

            // ✅ UPDATE USERPREF IMMEDIATELY
            userPref.setCoins(updatedCoins);
            userCoins = updatedCoins;
            txtCoins.setText(String.valueOf(updatedCoins));

            startActivity(
                    new Intent(getContext(), activity_withdraw_success.class)
                            .putExtra(activity_withdraw_success.EXTRA_REQUEST_ID, requestId)
                            .putExtra(activity_withdraw_success.EXTRA_TYPE, redeemType)
                            .putExtra(activity_withdraw_success.EXTRA_AMOUNT, amount)
            );

            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();

        }).addOnFailureListener(e -> toast(e.getMessage()));
    }

    /* ================= HELPERS ================= */
    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private String getRewardName(String type) {
        switch (type) {
            case GOOGLE:
                return "Google Play Voucher";
            case AMAZON:
                return "Amazon Gift Voucher";
            case PHONEPE:
                return "PhonePe Gift Voucher";
            case UPI:
                return "UPI Withdraw";
            case BANK:
                return "Bank Withdraw";
            default:
                return "Reward";
        }
    }
}
