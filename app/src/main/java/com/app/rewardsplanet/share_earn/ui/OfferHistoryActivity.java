package com.app.rewardsplanet.share_earn.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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

        apiService = ApiClient.getClient().create(ShareEarnApiService.class);

        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());

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
        if (!swipeHistory.isRefreshing()) {
            progressHistory.setVisibility(View.VISIBLE);
        }
        layoutEmptyHistory.setVisibility(View.GONE);

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                apiService.getMyOffers(bearerToken, currentStatus).enqueue(new Callback<OfferHistoryResponse>() {
                    @Override
                    public void onResponse(Call<OfferHistoryResponse> call, Response<OfferHistoryResponse> response) {
                        progressHistory.setVisibility(View.GONE);
                        swipeHistory.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<OfferHistoryResponse.HistoryItem> data = response.body().getData();
                            historyList.clear();
                            if (data != null && !data.isEmpty()) {
                                historyList.addAll(data);
                                adapter.notifyDataSetChanged();
                                rvOfferHistory.setVisibility(View.VISIBLE);
                            } else {
                                rvOfferHistory.setVisibility(View.GONE);
                                layoutEmptyHistory.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(OfferHistoryActivity.this, "Failed to load offer history", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<OfferHistoryResponse> call, Throwable t) {
                        progressHistory.setVisibility(View.GONE);
                        swipeHistory.setRefreshing(false);
                        Toast.makeText(OfferHistoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                progressHistory.setVisibility(View.GONE);
                swipeHistory.setRefreshing(false);
                Toast.makeText(OfferHistoryActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
