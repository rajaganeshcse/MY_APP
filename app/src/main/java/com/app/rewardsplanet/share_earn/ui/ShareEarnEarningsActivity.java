package com.app.rewardsplanet.share_earn.ui;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.AuthTokenHelper;
import com.app.rewardsplanet.share_earn.adapter.OfferPerformanceAdapter;
import com.app.rewardsplanet.share_earn.model.ShareEarnEarningsResponse;
import com.app.rewardsplanet.share_earn.network.ShareEarnApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareEarnEarningsActivity extends AppCompatActivity {

    private ShareEarnApiService apiService;
    private OfferPerformanceAdapter adapter;
    private final List<ShareEarnEarningsResponse.OfferPerformance> perfList = new ArrayList<>();

    private TextView txtTotalEarnedCoins, txtStatClicks, txtStatConversions, txtStatPending;
    private RecyclerView rvOfferPerformance;
    private ProgressBar progressEarnings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_earn_earnings);
        makeFullScreen();

        apiService = ApiClient.getClient().create(ShareEarnApiService.class);

        View btnBack = findViewById(R.id.btnBackEarnings);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        txtTotalEarnedCoins = findViewById(R.id.txtTotalEarnedCoins);
        txtStatClicks = findViewById(R.id.txtStatClicks);
        txtStatConversions = findViewById(R.id.txtStatConversions);
        txtStatPending = findViewById(R.id.txtStatPending);
        rvOfferPerformance = findViewById(R.id.rvOfferPerformance);
        progressEarnings = findViewById(R.id.progressEarnings);

        rvOfferPerformance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfferPerformanceAdapter(this, perfList);
        rvOfferPerformance.setAdapter(adapter);

        fetchEarnings();
    }

    private void fetchEarnings() {
        if (isFinishing() || isDestroyed()) return;
        if (progressEarnings != null) progressEarnings.setVisibility(View.VISIBLE);

        AuthTokenHelper.getBearerToken(new AuthTokenHelper.TokenCallback() {
            @Override
            public void onSuccess(String bearerToken) {
                if (isFinishing() || isDestroyed()) return;

                if (apiService == null) {
                    apiService = ApiClient.getClient().create(ShareEarnApiService.class);
                }

                apiService.getMyEarnings(bearerToken).enqueue(new Callback<ShareEarnEarningsResponse>() {
                    @Override
                    public void onResponse(Call<ShareEarnEarningsResponse> call, Response<ShareEarnEarningsResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (progressEarnings != null) progressEarnings.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ShareEarnEarningsResponse.EarningsData data = response.body().getData();
                            if (data != null) {
                                if (txtTotalEarnedCoins != null) txtTotalEarnedCoins.setText(String.format("%,d", data.getTotalEarnedCoins()));
                                if (txtStatClicks != null) txtStatClicks.setText(String.valueOf(data.getTotalClicks()));
                                if (txtStatConversions != null) txtStatConversions.setText(String.valueOf(data.getTotalConversions()));
                                if (txtStatPending != null) txtStatPending.setText(String.valueOf(data.getPendingConversions()));

                                perfList.clear();
                                if (data.getOfferPerformance() != null) {
                                    perfList.addAll(data.getOfferPerformance());
                                }
                                if (adapter != null) adapter.notifyDataSetChanged();
                            }
                        } else {
                            Toast.makeText(ShareEarnEarningsActivity.this, "Failed to load earnings", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ShareEarnEarningsResponse> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        if (progressEarnings != null) progressEarnings.setVisibility(View.GONE);
                        Toast.makeText(ShareEarnEarningsActivity.this, "Network error: " + (t != null ? t.getMessage() : "failed"), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                if (progressEarnings != null) progressEarnings.setVisibility(View.GONE);
                Toast.makeText(ShareEarnEarningsActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
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
            android.util.Log.e("ShareEarnEarnings", "makeFullScreen error", e);
        }
    }
}
