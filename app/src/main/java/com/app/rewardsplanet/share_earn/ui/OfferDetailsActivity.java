package com.app.rewardsplanet.share_earn.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.AuthTokenHelper;
import com.app.rewardsplanet.share_earn.model.ClickTrackingResponse;
import com.app.rewardsplanet.share_earn.model.ShareEarnOffer;
import com.app.rewardsplanet.share_earn.network.ShareEarnApiService;
import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfferDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_OFFER = "extra_offer";

    private ShareEarnOffer offer;
    private ShareEarnApiService apiService;

    private ImageView imgBanner, imgLogo;
    private TextView txtTitle, txtCategory, txtStatus, txtReward, txtFullDesc;
    private LinearLayout containerHowItWorks, containerTerms;
    private Button btnShareEarnNow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_details);

        apiService = ApiClient.getClient().create(ShareEarnApiService.class);

        offer = (ShareEarnOffer) getIntent().getSerializableExtra(EXTRA_OFFER);

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

        imgBanner = findViewById(R.id.imgBanner);
        imgLogo = findViewById(R.id.imgDetailLogo);
        txtTitle = findViewById(R.id.txtDetailTitle);
        txtCategory = findViewById(R.id.txtDetailCategory);
        txtStatus = findViewById(R.id.txtDetailStatus);
        txtReward = findViewById(R.id.txtDetailReward);
        txtFullDesc = findViewById(R.id.txtFullDesc);
        containerHowItWorks = findViewById(R.id.containerHowItWorks);
        containerTerms = findViewById(R.id.containerTerms);
        btnShareEarnNow = findViewById(R.id.btnShareEarnNow);

        ImageView btnShareTop = findViewById(R.id.btnShareTop);
        btnShareTop.setOnClickListener(v -> generateClickAndShare());
        btnShareEarnNow.setOnClickListener(v -> generateClickAndShare());

        bindOfferDetails();
    }

    private void bindOfferDetails() {
        if (offer == null) return;

        txtTitle.setText(offer.getTitle() != null ? offer.getTitle() : "Offer");
        txtCategory.setText(offer.getCategory() != null ? offer.getCategory() : "General");
        txtReward.setText(String.format("%,d", offer.getRewardCoins()) + " Coins");
        txtFullDesc.setText(offer.getDescription() != null ? offer.getDescription() : offer.getShortDescription());

        if (offer.getBannerUrl() != null && !offer.getBannerUrl().trim().isEmpty()) {
            Glide.with(this).load(offer.getBannerUrl()).placeholder(R.drawable.logo).error(R.drawable.logo).into(imgBanner);
        } else {
            imgBanner.setImageResource(R.drawable.logo);
        }

        if (offer.getLogoUrl() != null && !offer.getLogoUrl().trim().isEmpty()) {
            Glide.with(this).load(offer.getLogoUrl()).placeholder(R.drawable.logo).error(R.drawable.logo).into(imgLogo);
        } else {
            imgLogo.setImageResource(R.drawable.logo);
        }

        // How it works items
        containerHowItWorks.removeAllViews();
        List<String> steps = offer.getHowItWorks();
        if (steps != null && !steps.isEmpty()) {
            int num = 1;
            for (String step : steps) {
                TextView tv = new TextView(this);
                tv.setText(num + ". " + step);
                tv.setTextSize(14);
                tv.setTextColor(Color.parseColor("#475569"));
                tv.setPadding(0, 6, 0, 6);
                containerHowItWorks.addView(tv);
                num++;
            }
        } else {
            String[] defaultSteps = {
                    "1. Click on Share & Earn Now",
                    "2. User visits destination platform",
                    "3. Complete registration / required action",
                    "4. Conversion is verified",
                    "5. Coins credited to your wallet!"
            };
            for (String s : defaultSteps) {
                TextView tv = new TextView(this);
                tv.setText(s);
                tv.setTextSize(14);
                tv.setTextColor(Color.parseColor("#475569"));
                tv.setPadding(0, 6, 0, 6);
                containerHowItWorks.addView(tv);
            }
        }

        // Terms items
        containerTerms.removeAllViews();
        List<String> terms = offer.getTermsAndConditions();
        if (terms != null && !terms.isEmpty()) {
            for (String term : terms) {
                TextView tv = new TextView(this);
                tv.setText("• " + term);
                tv.setTextSize(13);
                tv.setTextColor(Color.parseColor("#64748B"));
                tv.setPadding(0, 4, 0, 4);
                containerTerms.addView(tv);
            }
        } else {
            String[] defaultTerms = {
                    "• Valid once per user account",
                    "• Complete required action within 7 days",
                    "• Duplicate or fraud activity will result in rejection"
            };
            for (String t : defaultTerms) {
                TextView tv = new TextView(this);
                tv.setText(t);
                tv.setTextSize(13);
                tv.setTextColor(Color.parseColor("#64748B"));
                tv.setPadding(0, 4, 0, 4);
                containerTerms.addView(tv);
            }
        }
    }

    private void generateClickAndShare() {
        if (offer == null || offer.getOfferId() == null) return;

        btnShareEarnNow.setEnabled(false);
        btnShareEarnNow.setText("Generating Link...");

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                Map<String, Object> req = new HashMap<>();
                req.put("offerId", offer.getOfferId());

                apiService.createTrackingClick(bearerToken, req).enqueue(new Callback<ClickTrackingResponse>() {
                    @Override
                    public void onResponse(Call<ClickTrackingResponse> call, Response<ClickTrackingResponse> response) {
                        btnShareEarnNow.setEnabled(true);
                        btnShareEarnNow.setText("Share & Earn Now");

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ClickTrackingResponse.ClickData data = response.body().getData();
                            if (data != null && data.getTrackingUrl() != null) {
                                String trackingUrl = data.getTrackingUrl();

                                ShareOfferBottomSheetFragment sheet = ShareOfferBottomSheetFragment.newInstance(offer, trackingUrl);
                                sheet.show(getSupportFragmentManager(), "ShareOfferBottomSheet");
                            }
                        } else {
                            Toast.makeText(OfferDetailsActivity.this, "Failed to generate tracking link", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ClickTrackingResponse> call, Throwable t) {
                        btnShareEarnNow.setEnabled(true);
                        btnShareEarnNow.setText("Share & Earn Now");
                        Toast.makeText(OfferDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                btnShareEarnNow.setEnabled(true);
                btnShareEarnNow.setText("Share & Earn Now");
                Toast.makeText(OfferDetailsActivity.this, "Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
