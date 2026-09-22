package com.app.rewardsplanet.share_earn.ui;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
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
import com.app.rewardsplanet.share_earn.adapter.OfferHistoryAdapter;
import com.app.rewardsplanet.share_earn.model.OfferHistoryResponse;
import com.app.rewardsplanet.share_earn.network.ShareEarnApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfferHistoryActivity extends AppCompatActivity {

    private ShareEarnApiService apiService;
    private OfferHistoryAdapter adapter;
    private final List<OfferHistoryResponse.HistoryItem> historyList = new ArrayList<>();

    private SwipeRefreshLayout swipeHistory;
    private RecyclerView rvOfferHistory;
    private ProgressBar progressHistory;
    private LinearLayout layoutEmptyHistory;

    private TextView tabAll, tabPending, tabCompleted, tabRejected;
    private String currentStatus = "All";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offer_history);
        makeFullScreen();

        apiService = ApiClient.getClient().create(ShareEarnApiService.class);

        View btnBack = findViewById(R.id.btnBackHistory);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        swipeHistory = findViewById(R.id.swipeHistory);
        rvOfferHistory = findViewById(R.id.rvOfferHistory);
        progressHistory = findViewById(R.id.progressHistory);
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory);

        tabAll = findViewById(R.id.tabHistoryAll);
        tabPending = findViewById(R.id.tabHistoryPending);
        tabCompleted = findViewById(R.id.tabHistoryCompleted);
        tabRejected = findViewById(R.id.tabHistoryRejected);

        rvOfferHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfferHistoryAdapter(this, historyList);
        rvOfferHistory.setAdapter(adapter);

        tabAll.setOnClickListener(v -> selectTab("All", tabAll));
        tabPending.setOnClickListener(v -> selectTab("Pending", tabPending));
        tabCompleted.setOnClickListener(v -> selectTab("APPROVED", tabCompleted));
        tabRejected.setOnClickListener(v -> selectTab("Rejected", tabRejected));

        swipeHistory.setOnRefreshListener(this::fetchHistory);

        fetchHistory();
    }

    private void selectTab(String status, TextView selectedTab) {
        currentStatus = status;

        resetTabStyles();
        selectedTab.setBackgroundColor(Color.parseColor("#4F46E5"));
        selectedTab.setTextColor(Color.WHITE);

        fetchHistory();
    }

    private void resetTabStyles() {
        TextView[] tabs = {tabAll, tabPending, tabCompleted, tabRejected};
        for (TextView t : tabs) {
            t.setBackgroundColor(Color.WHITE);
            t.setTextColor(Color.parseColor("#64748B"));
        }
    }

    private void fetchHistory() {
        if (isFinishing() || isDestroyed()) return;
        if (progressHistory != null) progressHistory.setVisibility(View.VISIBLE);

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                if (isFinishing() || isDestroyed()) return;

                if (apiService == null) {
                    apiService = ApiClient.getClient().create(ShareEarnApiService.class);
                }

                apiService.getMyOffers(bearerToken, currentStatus).enqueue(new Callback<OfferHistoryResponse>() {
                    @Override
                    public void onResponse(Call<OfferHistoryResponse> call, Response<OfferHistoryResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (progressHistory != null) progressHistory.setVisibility(View.GONE);
                        if (swipeHistory != null) swipeHistory.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<OfferHistoryResponse.HistoryItem> data = response.body().getData();
                            historyList.clear();
                            if (data != null && !data.isEmpty()) {
                                historyList.addAll(data);
                                if (adapter != null) adapter.notifyDataSetChanged();
                                if (rvOfferHistory != null) rvOfferHistory.setVisibility(View.VISIBLE);
                                if (layoutEmptyHistory != null) layoutEmptyHistory.setVisibility(View.GONE);
                            } else {
                                if (rvOfferHistory != null) rvOfferHistory.setVisibility(View.GONE);
                                if (layoutEmptyHistory != null) layoutEmptyHistory.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(OfferHistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<OfferHistoryResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        if (progressHistory != null) progressHistory.setVisibility(View.GONE);
                        if (swipeHistory != null) swipeHistory.setRefreshing(false);
                        Toast.makeText(OfferHistoryActivity.this, "Network error: " + (t != null ? t.getMessage() : "failed"), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                if (progressHistory != null) progressHistory.setVisibility(View.GONE);
                if (swipeHistory != null) swipeHistory.setRefreshing(false);
                Toast.makeText(OfferHistoryActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
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
            android.util.Log.e("OfferHistoryActivity", "makeFullScreen error", e);
        }
    }
}
