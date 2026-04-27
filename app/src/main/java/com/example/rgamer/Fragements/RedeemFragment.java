package com.example.rgamer.Fragements;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.rgamer.R;
import com.example.rgamer.RedeemResponse;
import com.example.rgamer.UserPref;
import com.example.rgamer.network.ApiClient;
import com.example.rgamer.network.ApiService;
import com.example.rgamer.withdraws.activity_withdraw_success;
import com.example.rgamer.withdraws.bottomsheet_withdraw_details;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    private UserPref userPref;
    private long userCoins = 0;

    private long pendingCoins = 0;
    private long pendingAmount = 0;
    private String withdrawDetails = "";

    private boolean isSubmitting = false;
    private AlertDialog loadingDialog;

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
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_redeem_options, container, false);

        txtTitle = view.findViewById(R.id.txtTitle);
        txtCoins = view.findViewById(R.id.txtCoins);
        gridLayout = view.findViewById(R.id.gridLayout);

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
            case GOOGLE: txtTitle.setText("Google Play Voucher"); break;
            case AMAZON: txtTitle.setText("Amazon Gift Voucher"); break;
            case PHONEPE: txtTitle.setText("PhonePe Gift Voucher"); break;
            case UPI: txtTitle.setText("UPI Withdraw"); break;
            case BANK: txtTitle.setText("Bank Withdraw"); break;
        }
    }

    private void loadCoins() {
        userCoins = userPref.getCoins();
        txtCoins.setText(String.valueOf(userCoins));
    }

    private void setupCards() {

        gridLayout.removeAllViews();

        if (UPI.equals(redeemType)) {
            addCard(R.drawable.ic_upi, 200, 2);
            addCard(R.drawable.ic_upi, 500, 5);
            addCard(R.drawable.ic_upi, 1174, 10);
            addCard(R.drawable.ic_upi, 2674, 25);
            addCard(R.drawable.ic_upi, 10000, 100);

        } else if (BANK.equals(redeemType)) {
            addCard(R.drawable.ic_bank, 5000, 50);
            addCard(R.drawable.ic_bank, 10000, 100);
            addCard(R.drawable.ic_bank, 20000, 200);

        } else {
            int icon = AMAZON.equals(redeemType)
                    ? R.drawable.ic_amazon
                    : PHONEPE.equals(redeemType)
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

            if (UPI.equals(redeemType) || BANK.equals(redeemType)) {

                pendingCoins = cost;
                pendingAmount = amount;

                bottomsheet_withdraw_details
                        .newInstance(redeemType)
                        .show(getParentFragmentManager(), "withdraw_sheet");

            } else {
                showConfirmDialog(cost, amount);
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

                            withdrawDetails = bundle.getString(
                                    bottomsheet_withdraw_details.KEY_RESULT, ""
                            );

                            showConfirmDialog(pendingCoins, pendingAmount);
                        });
    }

    private void showConfirmDialog(long coins, long amount) {

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_confirm_redeem, null, false);

        TextView txtAmount = view.findViewById(R.id.txtConfirmAmount);
        TextView txtCoins = view.findViewById(R.id.txtConfirmCoins);
        TextView txtDetails = view.findViewById(R.id.txtConfirmDetails);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnConfirm = view.findViewById(R.id.btnConfirm);

        txtAmount.setText("Amount: ₹" + amount);
        txtCoins.setText("Coins: " + coins);

        if (UPI.equals(redeemType)) {
            txtDetails.setVisibility(View.VISIBLE);
            txtDetails.setText("UPI ID:\n" + withdrawDetails);
        } else if (BANK.equals(redeemType)) {
            txtDetails.setVisibility(View.VISIBLE);
            txtDetails.setText("Bank Details:\n" + withdrawDetails);
        } else {
            txtDetails.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(false)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);
            dialog.dismiss();
            submitRedeem(coins, amount);
        });

        dialog.show();
    }

    /* ================= UPDATED TOKEN LOGIC ================= */
    private void submitRedeem(long coinsUsed, long amount) {
        showLoading();

        if (isSubmitting) {
            toast("Please wait...");
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            toast("Session expired");
            hideLoading();
            return;
        }

        isSubmitting = true;
        toast("Processing...");


        FirebaseAuth.getInstance().getCurrentUser()
                .getIdToken(true)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        isSubmitting = false;
                        toast("Token error");
                        hideLoading();
                        return;
                    }

                    String idToken = task.getResult().getToken();
                    String token = "Bearer " + idToken;

                    callRedeemApi(token, coinsUsed, amount);
                });
    }

    //
    private void showLoading() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);

        loadingDialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(false)
                .create();

        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void callRedeemApi(String token, long coinsUsed, long amount) {

        ApiService api = ApiClient.getClient().create(ApiService.class);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("coins", coinsUsed);
        body.put("type", redeemType);
        body.put("details", withdrawDetails);

        api.redeemRequest(token, body).enqueue(new Callback<RedeemResponse>() {

            @Override
            public void onResponse(Call<RedeemResponse> call, Response<RedeemResponse> response) {

                isSubmitting = false;

                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {

                    RedeemResponse res = response.body();

                    if (res.status) {

                        userPref.setCoins(res.updatedCoins);
                        txtCoins.setText(String.valueOf(res.updatedCoins));
                        hideLoading();
                        toast("Success ✅");

                        Intent i = new Intent(requireActivity(), activity_withdraw_success.class);
                        i.putExtra(activity_withdraw_success.EXTRA_TYPE, redeemType);
                        i.putExtra(activity_withdraw_success.EXTRA_AMOUNT, "₹" + amount);
                        i.putExtra(activity_withdraw_success.EXTRA_REQUEST_ID, res.requestId);

                        startActivity(i);

                    } else {
                        hideLoading();
                        toast(res.message);
                    }

                } else {
                    hideLoading();
                    toast("Server error ❌");
                }
            }

            @Override
            public void onFailure(Call<RedeemResponse> call, Throwable t) {
                isSubmitting = false;
                hideLoading();
                toast("API Failed: " + t.getMessage());
            }
        });
    }

    private String getMethodDetailText() {
        return UPI.equals(redeemType)
                ? "Cash (UPI)"
                : BANK.equals(redeemType)
                ? "Cash (Bank)"
                : "Voucher Code";
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}