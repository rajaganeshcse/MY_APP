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

    // ================= TYPES =================
    public static final String TYPE = "type";
    public static final String GOOGLE = "google";
    public static final String AMAZON = "amazon";
    public static final String PHONEPE = "phonepe";
    public static final String UPI = "upi";
    public static final String BANK = "bank";

    private String redeemType = GOOGLE;

    private TextView txtTitle, txtCoins;
    private GridLayout gridLayout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private long userCoins = 0;

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

        if (getArguments() != null) {
            redeemType = getArguments().getString(TYPE, GOOGLE);
        }

        setupHeader();
        loadCoins();
        setupCards();

        return view;
    }

    // ================= HEADER =================
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

    // ================= LOAD COINS =================
    private void loadCoins() {
        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    Long c = doc.getLong("coins");
                    userCoins = c == null ? 0 : c;
                    txtCoins.setText(String.valueOf(userCoins));
                });
    }

    // ================= CARDS =================
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

        } else if (redeemType.equals(UPI)) {
            addCard(R.drawable.ic_upi, 1174, "₹10");

        } else if (redeemType.equals(BANK)) {
            addCard(R.drawable.ic_bank, 10000, "₹100");
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

    // ================= ATOMIC REDEEM =================
    private void submitRedeem(long coinsUsed, String amount) {

        String uid = auth.getCurrentUser().getUid();
        UserPref userPref = new UserPref(requireContext());

        db.runTransaction(transaction -> {

            var userRef = db.collection("users").document(uid);
            var snap = transaction.get(userRef);

            Long current = snap.getLong("coins");
            if (current == null || current < coinsUsed)
                throw new RuntimeException("Insufficient coins");

            long updated = current - coinsUsed;

            transaction.update(userRef, "coins", updated);

            // 🔑 Create redeem request with known ID
            var requestRef =
                    db.collection("redeem_requests").document();

            Map<String, Object> req = new HashMap<>();
            req.put("uid", uid);
            req.put("type", redeemType);
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

            // Save locally
            userPref.setCoins((int) updatedCoins);

            // Open success screen
            startActivity(
                    new Intent(getContext(), activity_withdraw_success.class)
                            .putExtra("request_id", requestId)
                            .putExtra("type", redeemType)
                            .putExtra("amount", amount)
            );

            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();

        }).addOnFailureListener(e -> toast(e.getMessage()));
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
