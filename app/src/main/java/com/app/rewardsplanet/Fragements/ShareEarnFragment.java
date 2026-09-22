package com.app.rewardsplanet.Fragements;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.app.rewardsplanet.share_earn.ui.OfferDetailsActivity;
import com.app.rewardsplanet.share_earn.ui.OfferHistoryActivity;
import com.app.rewardsplanet.share_earn.ui.ShareEarnEarningsActivity;
import com.app.rewardsplanet.share_earn.ui.ShareOfferBottomSheetFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareEarnFragment extends Fragment {

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

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_share_earn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient().create(ShareEarnApiService.class);

        View btnHistory = view.findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                if (getContext() != null) {
                    startActivity(new Intent(getContext(), OfferHistoryActivity.class));
                }
            });
        }

        View btnEarnings = view.findViewById(R.id.btnEarnings);
        if (btnEarnings != null) {
            btnEarnings.setOnClickListener(v -> {
                if (getContext() != null) {
                    startActivity(new Intent(getContext(), ShareEarnEarningsActivity.class));
                }
            });
        }

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rvOffers = view.findViewById(R.id.rvOffers);
        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutError = view.findViewById(R.id.layoutError);
        txtErrorMsg = view.findViewById(R.id.txtErrorMsg);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        edtSearch = view.findViewById(R.id.edtSearch);
        Button btnRetry = view.findViewById(R.id.btnRetry);

        if (rvOffers != null && getContext() != null) {
            rvOffers.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ShareEarnOfferAdapter(getContext(), offerList, new ShareEarnOfferAdapter.OnOfferClickListener() {
                @Override
                public void onOfferClick(ShareEarnOffer offer) {
                    if (offer != null && getContext() != null) {
                        Intent intent = new Intent(getContext(), OfferDetailsActivity.class);
                        intent.putExtra(OfferDetailsActivity.EXTRA_OFFER, offer);
                        startActivity(intent);
                    }
                }

                @Override
                public void onShareClick(ShareEarnOffer offer) {
                    generateClickAndShare(offer);
                }
            });
            rvOffers.setAdapter(adapter);
        }

        setupCategoryChips();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::fetchOffers);
        }
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> fetchOffers());
        }

        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().trim();
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> fetchOffers();
                    searchHandler.postDelayed(searchRunnable, 350);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        fetchOffers();
    }

    private void setupCategoryChips() {
        if (!isAdded() || getContext() == null || chipGroupCategory == null) return;

        chipGroupCategory.removeAllViews();
        Context context = getContext();

        for (String cat : categories) {
            TextView chip = new TextView(context);
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

    private void fetchOffers() {
        if (!isAdded() || getContext() == null) return;

        if (swipeRefresh != null && !swipeRefresh.isRefreshing()) {
            if (shimmerViewContainer != null) {
                try {
                    shimmerViewContainer.setVisibility(View.VISIBLE);
                    shimmerViewContainer.startShimmer();
                } catch (Exception ignored) {}
            }
            if (rvOffers != null) rvOffers.setVisibility(View.GONE);
        }
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.GONE);

        if (apiService == null) {
            apiService = ApiClient.getClient().create(ShareEarnApiService.class);
        }

        apiService.getOffers(currentCategory, currentSearchQuery).enqueue(new Callback<ShareEarnOffersResponse>() {
            @Override
            public void onResponse(Call<ShareEarnOffersResponse> call, Response<ShareEarnOffersResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (shimmerViewContainer != null) {
                    try {
                        shimmerViewContainer.stopShimmer();
                        shimmerViewContainer.setVisibility(View.GONE);
                    } catch (Exception ignored) {}
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<ShareEarnOffer> data = response.body().getData();
                    offerList.clear();
                    if (data != null && !data.isEmpty()) {
                        offerList.addAll(data);
                        if (adapter != null) adapter.notifyDataSetChanged();
                        if (rvOffers != null) rvOffers.setVisibility(View.VISIBLE);
                    } else {
                        if (rvOffers != null) rvOffers.setVisibility(View.GONE);
                        if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    showError("Failed to fetch offers from server");
                }
            }

            @Override
            public void onFailure(Call<ShareEarnOffersResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;

                if (shimmerViewContainer != null) {
                    try {
                        shimmerViewContainer.stopShimmer();
                        shimmerViewContainer.setVisibility(View.GONE);
                    } catch (Exception ignored) {}
                }
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                String msg = (t != null && t.getMessage() != null) ? t.getMessage() : "Unknown network error";
                showError("Network error: " + msg);
            }
        });
    }

    private void showError(String msg) {
        if (!isAdded() || getContext() == null) return;

        if (shimmerViewContainer != null) {
            try {
                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
            } catch (Exception ignored) {}
        }
        if (rvOffers != null) rvOffers.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.VISIBLE);
        if (txtErrorMsg != null) txtErrorMsg.setText(msg);
    }

    private void generateClickAndShare(ShareEarnOffer offer) {
        if (!isAdded() || getContext() == null) return;
        if (offer == null || offer.getOfferId() == null) return;

        Toast.makeText(getContext(), "Generating tracking link...", Toast.LENGTH_SHORT).show();

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                if (!isAdded() || getContext() == null) return;

                Map<String, Object> req = new HashMap<>();
                req.put("offerId", offer.getOfferId());

                if (apiService == null) {
                    apiService = ApiClient.getClient().create(ShareEarnApiService.class);
                }

                apiService.createTrackingClick(bearerToken, req).enqueue(new Callback<ClickTrackingResponse>() {
                    @Override
                    public void onResponse(Call<ClickTrackingResponse> call, Response<ClickTrackingResponse> response) {
                        if (!isAdded() || getContext() == null) return;

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ClickTrackingResponse.ClickData data = response.body().getData();
                            if (data != null && data.getTrackingUrl() != null) {
                                String trackingUrl = data.getTrackingUrl();

                                try {
                                    ShareOfferBottomSheetFragment sheet = ShareOfferBottomSheetFragment.newInstance(offer, trackingUrl);
                                    sheet.show(getChildFragmentManager(), "ShareOfferBottomSheet");
                                } catch (Exception e) {
                                    android.util.Log.w("ShareEarnFragment", "Failed to show bottom sheet: " + e.getMessage());
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to create tracking click", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ClickTrackingResponse> call, Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Network error: " + (t != null ? t.getMessage() : "failed"), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Authentication required", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (shimmerViewContainer != null) {
            try {
                shimmerViewContainer.stopShimmer();
            } catch (Exception ignored) {}
        }
        super.onDestroyView();
    }
}
