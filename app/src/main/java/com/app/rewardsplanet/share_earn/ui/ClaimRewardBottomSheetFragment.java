package com.app.rewardsplanet.share_earn.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.AuthTokenHelper;
import com.app.rewardsplanet.share_earn.model.ClaimOfferResponse;
import com.app.rewardsplanet.share_earn.model.ShareEarnOffer;
import com.app.rewardsplanet.share_earn.network.ShareEarnApiService;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClaimRewardBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_OFFER = "arg_offer";
    private static final String ARG_CLICK_ID = "arg_click_id";

    private ShareEarnOffer offer;
    private String clickId;
    private ShareEarnApiService apiService;

    public static ClaimRewardBottomSheetFragment newInstance(ShareEarnOffer offer, String clickId) {
        ClaimRewardBottomSheetFragment fragment = new ClaimRewardBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OFFER, offer);
        args.putString(ARG_CLICK_ID, clickId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            offer = (ShareEarnOffer) getArguments().getSerializable(ARG_OFFER);
            clickId = getArguments().getString(ARG_CLICK_ID);
        }
        apiService = ApiClient.getClient().create(ShareEarnApiService.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_claim_reward, container, false);

        ImageView imgLogo = v.findViewById(R.id.imgClaimLogo);
        TextView txtTitle = v.findViewById(R.id.txtClaimTitle);
        TextView txtReward = v.findViewById(R.id.txtClaimRewardCoins);
        TextView txtInstructions = v.findViewById(R.id.txtClaimInstructions);
        TextView txtRefReminder = v.findViewById(R.id.txtReferralCodeReminder);
        EditText editProof = v.findViewById(R.id.editClaimProof);
        TextView txtError = v.findViewById(R.id.txtClaimError);
        ProgressBar progressBar = v.findViewById(R.id.progressBarClaim);
        Button btnSubmit = v.findViewById(R.id.btnSubmitClaim);
        Button btnCancel = v.findViewById(R.id.btnCancelClaim);

        if (offer != null) {
            txtTitle.setText("Claim " + (offer.getTitle() != null ? offer.getTitle() : "Offer"));
            txtReward.setText("+" + String.format("%,d", offer.getRewardCoins()) + " Coins");

            if (offer.getProofLabel() != null && !offer.getProofLabel().trim().isEmpty()) {
                txtInstructions.setText(offer.getProofLabel().trim());
                editProof.setHint(offer.getProofLabel().trim());
            }

            if (offer.getReferralCode() != null && !offer.getReferralCode().trim().isEmpty()) {
                txtRefReminder.setVisibility(View.VISIBLE);
                txtRefReminder.setText("Referral Code: " + offer.getReferralCode());
            } else {
                txtRefReminder.setVisibility(View.GONE);
            }

            if (offer.getLogoUrl() != null && !offer.getLogoUrl().trim().isEmpty()) {
                Glide.with(this).load(offer.getLogoUrl()).placeholder(R.drawable.logo).error(R.drawable.logo).into(imgLogo);
            } else {
                imgLogo.setImageResource(R.drawable.logo);
            }
        }

        btnCancel.setOnClickListener(view -> dismiss());

        btnSubmit.setOnClickListener(view -> {
            String proofText = editProof.getText() != null ? editProof.getText().toString().trim() : "";
            if (proofText.isEmpty()) {
                txtError.setVisibility(View.VISIBLE);
                txtError.setText("Please enter your proof details to submit claim.");
                return;
            }

            txtError.setVisibility(View.GONE);
            btnSubmit.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
                @Override
                public void onSuccess(String bearerToken) {
                    Map<String, Object> req = new HashMap<>();
                    req.put("proofText", proofText);
                    if (clickId != null && !clickId.isEmpty()) {
                        req.put("clickId", clickId);
                    }
                    if (offer != null && offer.getReferralCode() != null) {
                        req.put("referralCodeUsed", offer.getReferralCode());
                    }

                    String offerId = offer != null ? offer.getOfferId() : "";
                    apiService.submitOfferClaim(bearerToken, offerId, req).enqueue(new Callback<ClaimOfferResponse>() {
                        @Override
                        public void onResponse(Call<ClaimOfferResponse> call, Response<ClaimOfferResponse> response) {
                            if (!isAdded()) return;
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);

                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Toast.makeText(getContext(), "Claim submitted successfully! Reward will be credited after admin verification.", Toast.LENGTH_LONG).show();
                                dismiss();
                            } else {
                                String msg = response.body() != null && response.body().getMessage() != null 
                                        ? response.body().getMessage() 
                                        : "Failed to submit claim. You may have already submitted.";
                                txtError.setVisibility(View.VISIBLE);
                                txtError.setText(msg);
                            }
                        }

                        @Override
                        public void onFailure(Call<ClaimOfferResponse> call, Throwable t) {
                            if (!isAdded()) return;
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            txtError.setVisibility(View.VISIBLE);
                            txtError.setText("Network error: " + t.getMessage());
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    txtError.setVisibility(View.VISIBLE);
                    txtError.setText("Authentication failed. Please re-login.");
                }
            });
        });

        return v;
    }
}
