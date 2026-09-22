package com.app.rewardsplanet.share_earn.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.AuthTokenHelper;
import com.app.rewardsplanet.share_earn.adapter.ShareEarnOfferAdapter;
import com.app.rewardsplanet.share_earn.model.ClickTrackingResponse;
import com.app.rewardsplanet.share_earn.model.ShareEarnOffer;
import com.app.rewardsplanet.share_earn.model.ShareEarnOffersResponse;
import com.app.rewardsplanet.share_earn.network.ShareEarnApiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareEarnActivity extends AppCompatActivity {

    private ShareEarnApiService apiService;
    private ShareEarnOfferAdapter adapter;
    private final List<ShareEarnOffer> offerList = new ArrayList<>();

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvOffers;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerViewContainer;
    private LinearLayout layoutEmpty, layoutError, chipGroupCategory;
    private TextView txtErrorMsg;
    private EditText edtSearch;

    private String currentCategory = "All";
    private String currentSearchQuery = "";

    private final String[] categories = {"All", "Demat Account", "Credit Card", "UPI", "Shopping", "Apps", "Games"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_share_earn);

        apiService = ApiClient.getClient().create(ShareEarnApiService.class);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnHistory).setOnClickListener(v -> startActivity(new Intent(this, OfferHistoryActivity.class)));
        findViewById(R.id.btnEarnings).setOnClickListener(v -> startActivity(new Intent(this, ShareEarnEarningsActivity.class)));

        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvOffers = findViewById(R.id.rvOffers);
        shimmerViewContainer = findViewById(R.id.shimmerViewContainer);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutError = findViewById(R.id.layoutError);
        txtErrorMsg = findViewById(R.id.txtErrorMsg);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        edtSearch = findViewById(R.id.edtSearch);
        Button btnRetry = findViewById(R.id.btnRetry);

        rvOffers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShareEarnOfferAdapter(this, offerList, new ShareEarnOfferAdapter.OnOfferClickListener() {
            @Override
            public void onOfferClick(ShareEarnOffer offer) {
                Intent intent = new Intent(ShareEarnActivity.this, OfferDetailsActivity.class);
                intent.putExtra(OfferDetailsActivity.EXTRA_OFFER, offer);
                startActivity(intent);
            }

            @Override
            public void onShareClick(ShareEarnOffer offer) {
                generateClickAndShare(offer);
            }
        });
        rvOffers.setAdapter(adapter);

        setupCategoryChips();

        swipeRefresh.setOnRefreshListener(this::fetchOffers);
        btnRetry.setOnClickListener(v -> fetchOffers());

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                fetchOffers();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fetchOffers();
    }

    private void setupCategoryChips() {
        chipGroupCategory.removeAllViews();
        for (String cat : categories) {
            TextView chip = new TextView(this);
            chip.setText(cat);
            chip.setTextSize(13);
            chip.setPadding(32, 16, 32, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            updateChipStyle(chip, cat.equalsIgnoreCase(currentCategory));

            chip.setOnClickListener(v -> {
                currentCategory = cat;
                setupCategoryChips();
                fetchOffers();
            });

            chipGroupCategory.addView(chip);
        }
    }

    private void updateChipStyle(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_share_earn_chip_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_share_earn_chip_unselected);
            chip.setTextColor(Color.parseColor("#475569"));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeFullScreen();
    }

    private void makeFullScreen() {
        Window window = getWindow();
        if (window == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
                // Ensure dark status bar icons on light theme
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    private void fetchOffers() {
        if (!swipeRefresh.isRefreshing()) {
            if (shimmerViewContainer != null) {
                shimmerViewContainer.setVisibility(View.VISIBLE);
                shimmerViewContainer.startShimmer();
            }
            rvOffers.setVisibility(View.GONE);
        }
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        apiService.getOffers(currentCategory, currentSearchQuery).enqueue(new Callback<ShareEarnOffersResponse>() {
            @Override
            public void onResponse(Call<ShareEarnOffersResponse> call, Response<ShareEarnOffersResponse> response) {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<ShareEarnOffer> data = response.body().getData();
                    offerList.clear();
                    if (data != null && !data.isEmpty()) {
                        offerList.addAll(data);
                        adapter.notifyDataSetChanged();
                        rvOffers.setVisibility(View.VISIBLE);
                    } else {
                        rvOffers.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    showError("Failed to fetch offers from server");
                }
            }

            @Override
            public void onFailure(Call<ShareEarnOffersResponse> call, Throwable t) {
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                swipeRefresh.setRefreshing(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void showError(String msg) {
        if (shimmerViewContainer != null) {
            shimmerViewContainer.stopShimmer();
            shimmerViewContainer.setVisibility(View.GONE);
        }
        rvOffers.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        txtErrorMsg.setText(msg);
    }

    private void generateClickAndShare(ShareEarnOffer offer) {
        if (offer == null || offer.getOfferId() == null) return;

        Toast.makeText(this, "Generating tracking link...", Toast.LENGTH_SHORT).show();

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                Map<String, Object> req = new HashMap<>();
                req.put("offerId", offer.getOfferId());

                apiService.createTrackingClick(bearerToken, req).enqueue(new Callback<ClickTrackingResponse>() {
                    @Override
                    public void onResponse(Call<ClickTrackingResponse> call, Response<ClickTrackingResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ClickTrackingResponse.ClickData data = response.body().getData();
                            if (data != null && data.getTrackingUrl() != null) {
                                String trackingUrl = data.getTrackingUrl();

                                ShareOfferBottomSheetFragment sheet = ShareOfferBottomSheetFragment.newInstance(offer, trackingUrl);
                                sheet.show(getSupportFragmentManager(), "ShareOfferBottomSheet");
                            }
                        } else {
                            Toast.makeText(ShareEarnActivity.this, "Failed to create tracking click", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ClickTrackingResponse> call, Throwable t) {
                        Toast.makeText(ShareEarnActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ShareEarnActivity.this, "Authentication required", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
