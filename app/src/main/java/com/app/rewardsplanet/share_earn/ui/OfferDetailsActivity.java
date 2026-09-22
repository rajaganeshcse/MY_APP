package com.app.rewardsplanet.share_earn.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
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
    private Button btnShareEarnNow, btnClaimReward;
    private View cardReferralCode;
    private TextView txtReferralCode, btnCopyReferralCode;
    private String lastClickId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_details);
        makeFullScreen();

        apiService = ApiClient.getClient().create(ShareEarnApiService.class);

        offer = (ShareEarnOffer) getIntent().getSerializableExtra(EXTRA_OFFER);
        if (offer == null) {
            Toast.makeText(this, "Offer details unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
        btnClaimReward = findViewById(R.id.btnClaimReward);

        cardReferralCode = findViewById(R.id.cardReferralCode);
        txtReferralCode = findViewById(R.id.txtReferralCode);
        btnCopyReferralCode = findViewById(R.id.btnCopyReferralCode);

        ImageView btnShareTop = findViewById(R.id.btnShareTop);
        btnShareTop.setOnClickListener(v -> generateClickAndShare());
        btnShareEarnNow.setOnClickListener(v -> generateClickAndOpen());

        Button btnShareOffer = findViewById(R.id.btnShareOffer);
        if (btnShareOffer != null) {
            btnShareOffer.setOnClickListener(v -> generateClickAndShare());
        }

        btnClaimReward.setOnClickListener(v -> {
            ClaimRewardBottomSheetFragment sheet = ClaimRewardBottomSheetFragment.newInstance(offer, lastClickId);
            sheet.show(getSupportFragmentManager(), "ClaimRewardBottomSheet");
        });

        btnCopyReferralCode.setOnClickListener(v -> {
            if (offer != null && offer.getReferralCode() != null) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Referral Code", offer.getReferralCode());
                cm.setPrimaryClip(clip);
                Toast.makeText(this, "Referral Code Copied: " + offer.getReferralCode(), Toast.LENGTH_SHORT).show();
            }
        });

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

        // Referral Code Card
        if (offer.getReferralCode() != null && !offer.getReferralCode().trim().isEmpty()) {
            cardReferralCode.setVisibility(View.VISIBLE);
            txtReferralCode.setText(offer.getReferralCode().trim());
        } else {
            cardReferralCode.setVisibility(View.GONE);
        }

        // Claim Reward Button
        boolean isReferral = "REFERRAL_TASK".equalsIgnoreCase(offer.getOfferType()) || offer.isProofRequired();
        if (isReferral) {
            btnClaimReward.setVisibility(View.VISIBLE);
            btnShareEarnNow.setText("🌐 Visit & Complete");
        } else {
            btnClaimReward.setVisibility(View.GONE);
            btnShareEarnNow.setText("🌐 Visit & Earn");
        }

        // How it works items
        containerHowItWorks.removeAllViews();
        List<String> steps = offer.getHowItWorks();
        if (steps != null && !steps.isEmpty()) {
            int num = 1;
            for (String step : steps) {
                containerHowItWorks.addView(createStepView(num, step));
                num++;
            }
        } else {
            String[] defaultSteps = {
                    "Click on 'Visit & Earn' to open the offer",
                    "Complete registration or required actions",
                    "Submit verification proof via Claim Reward button",
                    "Admin verifies and approves your claim",
                    "Coins are credited directly to your wallet!"
            };
            int num = 1;
            for (String s : defaultSteps) {
                containerHowItWorks.addView(createStepView(num, s));
                num++;
            }
        }

        // Terms items
        containerTerms.removeAllViews();
        List<String> terms = offer.getTermsAndConditions();
        if (terms != null && !terms.isEmpty()) {
            for (String term : terms) {
                containerTerms.addView(createTermView(term));
            }
        } else {
            String[] defaultTerms = {
                    "Valid once per user and device",
                    "Complete required action within 7 days of clicking link",
                    "Duplicate or fraudulent claims will be rejected automatically"
            };
            for (String t : defaultTerms) {
                containerTerms.addView(createTermView(t));
            }
        }
    }

    private View createStepView(int stepNumber, String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        row.setLayoutParams(rowParams);

        TextView badge = new TextView(this);
        int badgeSize = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(badgeSize, badgeSize);
        badgeParams.setMargins(0, (int) (2 * getResources().getDisplayMetrics().density), 0, 0);
        badge.setLayoutParams(badgeParams);
        badge.setBackgroundResource(R.drawable.bg_step_circle);
        badge.setText(String.valueOf(stepNumber));
        badge.setTextSize(12);
        badge.setTextColor(Color.parseColor("#4F46E5"));
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setGravity(android.view.Gravity.CENTER);
        row.addView(badge);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textParams.setMargins((int) (10 * getResources().getDisplayMetrics().density), 0, 0, 0);
        tv.setLayoutParams(textParams);
        String cleanText = text.replaceFirst("^[0-9]+[.)]\\s*", "");
        tv.setText(cleanText);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#1E293B"));
        tv.setLineSpacing(4, 1.2f);
        row.addView(tv);

        return row;
    }

    private View createTermView(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        row.setLayoutParams(rowParams);

        TextView bullet = new TextView(this);
        bullet.setText("•");
        bullet.setTextSize(15);
        bullet.setTextColor(Color.parseColor("#6366F1"));
        bullet.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(bullet);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textParams.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, 0, 0);
        tv.setLayoutParams(textParams);
        String cleanText = text.replaceFirst("^[•\\-*]\\s*", "");
        tv.setText(cleanText);
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setLineSpacing(3, 1.2f);
        row.addView(tv);

        return row;
    }

    private void generateClickAndOpen() {
        if (offer == null || offer.getOfferId() == null) return;

        boolean isReferral = "REFERRAL_TASK".equalsIgnoreCase(offer.getOfferType()) || offer.isProofRequired();
        String defaultBtnText = isReferral ? "🌐 Visit & Complete" : "🌐 Visit & Earn";

        btnShareEarnNow.setEnabled(false);
        btnShareEarnNow.setText("Opening Offer...");

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                Map<String, Object> req = new HashMap<>();
                req.put("offerId", offer.getOfferId());

                apiService.createTrackingClick(bearerToken, req).enqueue(new Callback<ClickTrackingResponse>() {
                    @Override
                    public void onResponse(Call<ClickTrackingResponse> call, Response<ClickTrackingResponse> response) {
                        btnShareEarnNow.setEnabled(true);
                        btnShareEarnNow.setText(defaultBtnText);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ClickTrackingResponse.ClickData data = response.body().getData();
                            if (data != null && data.getTrackingUrl() != null) {
                                lastClickId = data.getClickId();
                                String trackingUrl = data.getTrackingUrl();

                                Intent countdownIntent = new Intent(OfferDetailsActivity.this, RedirectCountdownActivity.class);
                                countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_REDIRECT_URL, trackingUrl);
                                countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_OFFER_TITLE, offer.getTitle());
                                countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_OFFER_LOGO, offer.getLogoUrl());
                                countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_REWARD_COINS, offer.getRewardCoins());
                                countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_CLICK_ID, data.getClickId());
                                startActivity(countdownIntent);

                                if (isReferral) {
                                    btnClaimReward.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            // Fallback to direct /r/{offerId} tracking URL
                            String fallbackUrl = "https://app-backend-lutn.onrender.com/r/" + offer.getOfferId();
                            Intent countdownIntent = new Intent(OfferDetailsActivity.this, RedirectCountdownActivity.class);
                            countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_REDIRECT_URL, fallbackUrl);
                            countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_OFFER_TITLE, offer.getTitle());
                            countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_OFFER_LOGO, offer.getLogoUrl());
                            countdownIntent.putExtra(RedirectCountdownActivity.EXTRA_REWARD_COINS, offer.getRewardCoins());
                            startActivity(countdownIntent);

                            if (isReferral) {
                                btnClaimReward.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ClickTrackingResponse> call, Throwable t) {
                        btnShareEarnNow.setEnabled(true);
                        btnShareEarnNow.setText(defaultBtnText);
                        Toast.makeText(OfferDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                btnShareEarnNow.setEnabled(true);
                btnShareEarnNow.setText(defaultBtnText);
                Toast.makeText(OfferDetailsActivity.this, "Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateClickAndShare() {
        if (offer == null || offer.getOfferId() == null) return;

        boolean isReferral = "REFERRAL_TASK".equalsIgnoreCase(offer.getOfferType()) || offer.isProofRequired();
        String defaultBtnText = isReferral ? "Visit & Share" : "Share & Earn Now";

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
                        btnShareEarnNow.setText(defaultBtnText);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ClickTrackingResponse.ClickData data = response.body().getData();
                            if (data != null && data.getTrackingUrl() != null) {
                                lastClickId = data.getClickId();
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
                        btnShareEarnNow.setText(defaultBtnText);
                        Toast.makeText(OfferDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                btnShareEarnNow.setEnabled(true);
                btnShareEarnNow.setText(defaultBtnText);
                Toast.makeText(OfferDetailsActivity.this, "Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullScreen();
    }

    private void makeFullScreen() {
        try {
            Window window = getWindow();
            if (window == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                    controller.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            } else {
                View decor = window.getDecorView();
                if (decor != null) {
                    decor.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    );
                }
            }

            window.setStatusBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }
        } catch (Exception e) {
            android.util.Log.e("OfferDetailsActivity", "makeFullScreen error", e);
        }
    }
}
