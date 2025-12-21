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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RedeemFragment extends Fragment {

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

    private UserPref userPref;
    private long userCoins = 0;

    private long pendingCoins = 0;
    private long pendingAmount = 0;
    private String withdrawDetails = "";

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

        View view = inflater.inflate(
                R.layout.fragment_redeem_options,
                container,
                false
        );

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
        loadCoins();
        setupCards();
        setupBottomSheetResult();

        return view;
    }

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

    private void loadCoins() {
        userCoins = userPref.getCoins();
        txtCoins.setText(String.valueOf(userCoins));
    }

    private void setupCards() {
        gridLayout.removeAllViews();

        if (redeemType.equals(UPI)) {
            addCard(R.drawable.ic_upi, 1174, 10);
            addCard(R.drawable.ic_upi, 2674, 25);
            addCard(R.drawable.ic_upi, 10000, 100);

        } else if (redeemType.equals(BANK)) {
            addCard(R.drawable.ic_bank, 10000, 100);
            addCard(R.drawable.ic_bank, 20000, 200);

        } else {
            int icon = redeemType.equals(AMAZON)
                    ? R.drawable.ic_amazon
                    : redeemType.equals(PHONEPE)
                    ? R.drawable.ic_phonepe
                    : R.drawable.ic_google_play;

            addCard(icon, 1000, 10);
            addCard(icon, 3500, 35);
            addCard(icon, 5000, 50);
            addCard(icon, 10000, 100);
        }
    }

    private void addCard(int icon, long cost, long amount) {

        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_redeem_card, gridLayout, false);

        ImageView imgIcon = card.findViewById(R.id.imgIcon);
        TextView txtCoinCost = card.findViewById(R.id.txtCoinCost);
        TextView txtAmount = card.findViewById(R.id.txtAmount);
        TextView txtMethod = card.findViewById(R.id.methoddetail);

        imgIcon.setImageResource(icon);
        txtCoinCost.setText(String.valueOf(cost));
        txtAmount.setText("₹" + amount);
        txtMethod.setText(getMethodDetailText());

        card.setOnClickListener(v -> {

            if (userCoins < cost) {
                toast("Not enough coins");
                return;
            }

            if (redeemType.equals(UPI) || redeemType.equals(BANK)) {
                pendingCoins = cost;
                pendingAmount = amount;

                bottomsheet_withdraw_details
                        .newInstance(redeemType)
                        .show(getParentFragmentManager(), "withdraw_sheet");
            } else {
                submitRedeem(cost, amount);
            }
        });

        gridLayout.addView(card);
    }

    private void setupBottomSheetResult() {
        getParentFragmentManager()
                .setFragmentResultListener(
                        bottomsheet_withdraw_details.KEY_RESULT,
                        this,
                        (requestKey, bundle) -> {

                            withdrawDetails =
                                    bundle.getString(
                                            bottomsheet_withdraw_details.KEY_RESULT,
                                            ""
                                    );

                            submitRedeem(pendingCoins, pendingAmount);
                        });
    }

    private void submitRedeem(long coinsUsed, long amount) {

        String uid = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        long createdAtMillis = System.currentTimeMillis(); // ✅ MILLIS

        db.runTransaction(transaction -> {

            var userRef = db.collection("users").document(uid);
            var snap = transaction.get(userRef);

            Long current = snap.getLong("coins");
            if (current == null || current < coinsUsed) {
                throw new RuntimeException("Insufficient coins");
            }

            long updated = current - coinsUsed;
            transaction.update(userRef, "coins", updated);

            var reqRef = db.collection("redeem_requests").document();

            Map<String, Object> req = new HashMap<>();
            req.put("uid", uid);
            req.put("email", email);
            req.put("type", redeemType);
            req.put("amount", amount);
            req.put("coins", coinsUsed);
            req.put("withdraw_details", withdrawDetails);
            req.put("status", "pending");
            req.put("created_at", createdAtMillis); // ✅ MILLIS

            transaction.set(reqRef, req);
            return updated;

        }).addOnSuccessListener(updated -> {

            if (!isAdded()) return;

            userPref.setCoins(updated);
            txtCoins.setText(String.valueOf(updated));

            Intent i = new Intent(
                    requireActivity(),
                    activity_withdraw_success.class
            );
            i.putExtra(activity_withdraw_success.EXTRA_TYPE, redeemType);
            i.putExtra(activity_withdraw_success.EXTRA_AMOUNT, amount);
            i.putExtra(
                    activity_withdraw_success.EXTRA_WITHDRAW_DETAILS,
                    withdrawDetails
            );
            startActivity(i);

            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();

        }).addOnFailureListener(e -> toast(e.getMessage()));
    }

    private String getMethodDetailText() {
        return redeemType.equals(UPI)
                ? "Cash (UPI)"
                : redeemType.equals(BANK)
                ? "Cash (Bank)"
                : "Voucher Code";
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
