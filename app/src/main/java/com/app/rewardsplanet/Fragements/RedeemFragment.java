package com.app.rewardsplanet.Fragements;

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

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.RedeemResponse;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.app.rewardsplanet.withdraws.activity_withdraw_success;
import com.app.rewardsplanet.withdraws.bottomsheet_withdraw_details;
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
    )
    {

        View view = inflater.inflate(R.layout.fragment_redeem_options, container, false);

        txtTitle = view.findViewById(R.id.txtTitle);
        txtCoins = view.findViewById(R.id.txtCoins);
        gridLayout = view.findViewById(R.id.gridLayout);

        if (getContext() != null) {
            userPref = new UserPref(getContext());
        }

        if (getArguments() != null) {
            redeemType = getArguments().getString(TYPE, GOOGLE);
        }

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (!isAdded()) return;
                if (getParentFragmentManager() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        setupHeader();
        loadCoins();
        setupCards();
        setupBottomSheetResult();

        return view;
    }

    private void setupHeader() {
        if (txtTitle == null) return;
        switch (redeemType) {
            case GOOGLE: txtTitle.setText("Google Play Voucher"); break;
            case AMAZON: txtTitle.setText("Amazon Gift Voucher"); break;
            case PHONEPE: txtTitle.setText("PhonePe Gift Voucher"); break;
            case UPI: txtTitle.setText("UPI Withdraw"); break;
            case BANK: txtTitle.setText("Bank Withdraw"); break;
        }
    }

    private void loadCoins() {
        if (userPref != null && txtCoins != null) {
            userCoins = userPref.getCoins();
            txtCoins.setText(String.valueOf(userCoins));
        }
    }

    private void setupCards() {
        if (gridLayout == null) return;
        gridLayout.removeAllViews();

        int defaultIcon = UPI.equals(redeemType)
                ? R.drawable.ic_upi
                : BANK.equals(redeemType)
                ? R.drawable.ic_bank
                : AMAZON.equals(redeemType)
                ? R.drawable.ic_amazon
                : PHONEPE.equals(redeemType)
                ? R.drawable.ic_phonepe
                : R.drawable.ic_google_play;

        // Fetch dynamic reward options from Firestore settings/reward_config
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("settings")
                .document("reward_config")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || gridLayout == null) return;

                    boolean addedDynamic = false;
                    if (documentSnapshot.exists()) {
                        java.util.List<java.util.Map<String, Object>> tiers =
                                (java.util.List<java.util.Map<String, Object>>) documentSnapshot.get("tiers");

                        if (tiers != null && !tiers.isEmpty()) {
                            gridLayout.removeAllViews();
                            for (java.util.Map<String, Object> tier : tiers) {
                                String type = (String) tier.get("type");
                                Boolean enabled = (Boolean) tier.get("enabled");

                                if (redeemType.equalsIgnoreCase(type) && !Boolean.FALSE.equals(enabled)) {
                                    Number coinsNum = (Number) tier.get("coins");
                                    Number amountNum = (Number) tier.get("amount");

                                    if (coinsNum != null && amountNum != null) {
                                        addCard(defaultIcon, coinsNum.longValue(), amountNum.longValue());
                                        addedDynamic = true;
                                    }
                                }
                            }
                        }
                    }

                    if (!addedDynamic) {
                        populateDefaultCards(defaultIcon);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && gridLayout != null) {
                        populateDefaultCards(defaultIcon);
                    }
                });
    }

    private void populateDefaultCards(int icon) {
        if (gridLayout == null || !isAdded()) return;
        gridLayout.removeAllViews();

        if (UPI.equals(redeemType)) {
            addCard(icon, 200, 2);
            addCard(icon, 500, 5);
            addCard(icon, 1174, 10);
            addCard(icon, 2674, 25);
            addCard(icon, 10000, 100);
        } else if (BANK.equals(redeemType)) {
            addCard(icon, 5000, 50);
            addCard(icon, 10000, 100);
            addCard(icon, 20000, 200);
        } else {
            addCard(icon, 1000, 10);
            addCard(icon, 3500, 35);
            addCard(icon, 5000, 50);
            addCard(icon, 10000, 100);
        }
    }

    private void addCard(int icon, long cost, long amount) {
        if (!isAdded() || getContext() == null || gridLayout == null) return;

        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_redeem_card, gridLayout, false);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

        int marginPx = (int) (5 * getContext().getResources().getDisplayMetrics().density);
        params.setMargins(marginPx, marginPx, marginPx, marginPx);
        card.setLayoutParams(params);

        ImageView imgIcon = card.findViewById(R.id.imgIcon);
        TextView txtCoinCost = card.findViewById(R.id.txtCoinCost);
        TextView txtAmount = card.findViewById(R.id.txtAmount);
        TextView txtMethod = card.findViewById(R.id.methoddetail);

        if (imgIcon != null) imgIcon.setImageResource(icon);
        if (txtCoinCost != null) txtCoinCost.setText(String.valueOf(cost));
        if (txtAmount != null) txtAmount.setText("₹" + amount);
        if (txtMethod != null) txtMethod.setText(getMethodDetailText());

        card.setOnClickListener(v -> {

            if (userCoins < cost) {
                toast("Not enough coins");
                return;
            }

            if (UPI.equals(redeemType) || BANK.equals(redeemType)) {

                pendingCoins = cost;
                pendingAmount = amount;

                if (isAdded() && getParentFragmentManager() != null && !isStateSaved()) {
                    bottomsheet_withdraw_details
                            .newInstance(redeemType)
                            .show(getParentFragmentManager(), "withdraw_sheet");
                }

            } else {
                showConfirmDialog(cost, amount);
            }
        });

        gridLayout.addView(card);
    }

    private void setupBottomSheetResult() {
        if (!isAdded() || getParentFragmentManager() == null) return;

        getParentFragmentManager()
                .setFragmentResultListener(
                        bottomsheet_withdraw_details.KEY_RESULT,
                        getViewLifecycleOwner(),
                        (requestKey, bundle) -> {
                            if (!isAdded()) return;

                            withdrawDetails = bundle.getString(
                                    bottomsheet_withdraw_details.KEY_RESULT, ""
                            );

                            showConfirmDialog(pendingCoins, pendingAmount);
                        });
    }

    private void showConfirmDialog(long coins, long amount) {
        if (!isAdded() || getContext() == null || getActivity() == null || getActivity().isFinishing()) return;

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.layout_confirm_redeem, null, false);

        TextView txtAmount = view.findViewById(R.id.txtConfirmAmount);
        TextView txtCoins = view.findViewById(R.id.txtConfirmCoins);
        TextView txtDetails = view.findViewById(R.id.txtConfirmDetails);
        TextView btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnConfirm = view.findViewById(R.id.btnConfirm);

        if (txtAmount != null) txtAmount.setText("Amount: ₹" + amount);
        if (txtCoins != null) txtCoins.setText("Coins: " + coins);

        if (txtDetails != null) {
            if (UPI.equals(redeemType)) {
                txtDetails.setVisibility(View.VISIBLE);
                txtDetails.setText("UPI ID:\n" + withdrawDetails);
            } else if (BANK.equals(redeemType)) {
                txtDetails.setVisibility(View.VISIBLE);
                txtDetails.setText("Bank Details:\n" + withdrawDetails);
            } else {
                txtDetails.setVisibility(View.GONE);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .setCancelable(false)
                .create();

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                btnConfirm.setEnabled(false);
                dialog.dismiss();
                submitRedeem(coins, amount);
            });
        }
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        if (!getActivity().isFinishing() && !getActivity().isDestroyed()) {
            dialog.show();
        }
    }

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
                    if (!isAdded()) {
                        isSubmitting = false;
                        return;
                    }

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

    private void showLoading() {
        if (!isAdded() || getContext() == null || getActivity() == null || getActivity().isFinishing()) return;

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loading, null);

        loadingDialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .setCancelable(false)
                .create();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        if (!getActivity().isFinishing() && !getActivity().isDestroyed()) {
            loadingDialog.show();
        }
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception ignored) {}
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

                if (!isAdded() || getActivity() == null) return;

                if (response.isSuccessful() && response.body() != null) {

                    RedeemResponse res = response.body();

                    if (res.status) {

                        if (userPref != null) {
                            userPref.setCoins(res.updatedCoins);
                        }
                        if (txtCoins != null) {
                            txtCoins.setText(String.valueOf(res.updatedCoins));
                        }
                        hideLoading();
                        toast("Success ✅");

                        Intent i = new Intent(getActivity(), activity_withdraw_success.class);
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
                if (isAdded()) {
                    toast("API Failed: " + t.getMessage());
                }
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
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}