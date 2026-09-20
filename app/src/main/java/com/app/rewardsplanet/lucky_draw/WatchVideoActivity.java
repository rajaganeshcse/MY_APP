package com.app.rewardsplanet.lucky_draw;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.models.WatchVideoModel;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.app.rewardsplanet.utils.SuccessAnimationHelper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchVideoActivity extends AppCompatActivity implements WatchVideoAdapter.OnWatchClickListener {

    private FirebaseFirestore db;
    private ApiService api;
    private UserPref userPref;

    private TextView txtCoins, txtProgressCount;
    private ProgressBar progressDailyAds;
    private RecyclerView recyclerView;
    private WatchVideoAdapter adapter;

    private List<WatchVideoModel> videoList = new ArrayList<>();
    private String uid;
    private int userCoins = 0;
    private int watchedCount = 0;

    private RewardedAd rewardedAd;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_video);

        userPref = new UserPref(this);
        db = FirebaseFirestore.getInstance();
        api = ApiClient.getClient().create(ApiService.class);

        txtCoins = findViewById(R.id.txtCoins);
        txtProgressCount = findViewById(R.id.txtProgressCount);
        progressDailyAds = findViewById(R.id.progressDailyAds);
        recyclerView = findViewById(R.id.recyclerViewWatchVideos);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        makeFullScreen();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = (user != null && user.getUid() != null && !user.getUid().isEmpty())
                ? user.getUid()
                : userPref.getUid();

        userCoins = (int) userPref.getCoins();
        if (txtCoins != null) {
            txtCoins.setText(String.valueOf(userCoins));
        }

        try {
            MobileAds.initialize(this, status -> {});
        } catch (Exception ignored) {}

        setupVideoList();

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new WatchVideoAdapter(videoList, this);
            recyclerView.setAdapter(adapter);
        }

        listenUserData();
    }

    private void makeFullScreen() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    private void setupVideoList() {
        videoList.clear();
        videoList.add(new WatchVideoModel(1, "Video Reward #1", 10, 1, false));
        videoList.add(new WatchVideoModel(2, "Video Reward #2", 10, 1, false));
        videoList.add(new WatchVideoModel(3, "Video Reward #3", 10, 1, false));
        videoList.add(new WatchVideoModel(4, "Video Reward #4", 15, 1, false));
        videoList.add(new WatchVideoModel(5, "Video Reward #5", 15, 1, false));
        videoList.add(new WatchVideoModel(6, "Video Reward #6", 20, 1, false));
        videoList.add(new WatchVideoModel(7, "Video Reward #7", 20, 1, false));
        videoList.add(new WatchVideoModel(8, "Video Reward #8", 25, 1, false));
        videoList.add(new WatchVideoModel(9, "Video Reward #9", 25, 1, false));
        videoList.add(new WatchVideoModel(10, "Super Mega Bonus #10", 50, 2, false));
    }

    private void listenUserData() {
        if (uid == null || uid.isEmpty()) return;

        db.collection("users")
                .document(uid)
                .addSnapshotListener(this, (snap, e) -> {
                    if (snap != null && snap.exists()) {
                        Long coins = snap.getLong("coins");
                        if (coins != null) {
                            userCoins = coins.intValue();
                            userPref.setCoins(userCoins);
                            if (txtCoins != null) {
                                txtCoins.setText(String.valueOf(userCoins));
                            }
                        }

                        Long ads = snap.getLong("daily_ads_count");
                        watchedCount = ads != null ? ads.intValue() : 0;
                        updateProgressUI();
                    }
                });
    }

    private void updateProgressUI() {
        if (txtProgressCount != null) {
            txtProgressCount.setText(watchedCount + " / 10 Completed");
        }
        if (progressDailyAds != null) {
            progressDailyAds.setProgress(Math.min(watchedCount, 10));
        }

        int activeAdapterPosition = -1;
        for (int i = 0; i < videoList.size(); i++) {
            WatchVideoModel item = videoList.get(i);
            if (i < watchedCount) {
                item.setCompleted(true);
                item.setLocked(false);
            } else if (i == watchedCount) {
                item.setCompleted(false);
                item.setLocked(false);
                activeAdapterPosition = i + (i / 3);
            } else {
                item.setCompleted(false);
                item.setLocked(true);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (activeAdapterPosition >= 0 && recyclerView != null) {
            final int pos = activeAdapterPosition;
            recyclerView.post(() -> {
                try {
                    recyclerView.smoothScrollToPosition(pos);
                } catch (Exception ignored) {}
            });
        }
    }

    @Override
    public void onWatchClick(WatchVideoModel item, int position) {
        if (watchedCount >= 10) {
            Toast.makeText(this, "Daily video limit reached (10/10)", Toast.LENGTH_SHORT).show();
            return;
        }

        int taskIndex = item.getId() - 1;
        if (taskIndex > watchedCount) {
            Toast.makeText(this, "Please watch the previous video first!", Toast.LENGTH_SHORT).show();
            return;
        }

        item.setLoading(true);
        if (adapter != null) adapter.notifyItemChanged(position);

        loadAndShowAd(item, position);
    }

    private void loadAndShowAd(WatchVideoModel item, int position) {
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(this, AdsManager.REWARDED_AD_ID, adRequest, new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    rewardedAd = ad;
                    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            rewardedAd = null;
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            rewardedAd = null;
                            item.setLoading(false);
                            if (adapter != null) adapter.notifyItemChanged(position);
                            Toast.makeText(WatchVideoActivity.this, "Failed to display ad", Toast.LENGTH_SHORT).show();
                        }
                    });

                    rewardedAd.show(WatchVideoActivity.this, rewardItem -> {
                        claimAdReward(item, position);
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    rewardedAd = null;
                    item.setLoading(false);
                    if (adapter != null) adapter.notifyItemChanged(position);
                    Toast.makeText(WatchVideoActivity.this, "Ad failed to load. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            item.setLoading(false);
            if (adapter != null) adapter.notifyItemChanged(position);
            Toast.makeText(this, "Ad error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void claimAdReward(WatchVideoModel item, int position) {
        showLoading();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            hideLoading();
            item.setLoading(false);
            if (adapter != null) adapter.notifyItemChanged(position);
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        user.getIdToken(false).addOnSuccessListener(result -> {
            String token = result.getToken();
            Map<String, String> body = new HashMap<>();
            body.put("requestId", UUID.randomUUID().toString());

            api.rewardAd("Bearer " + token, body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    hideLoading();
                    item.setLoading(false);

                    if (response.isSuccessful()) {
                        int coinsEarned = item.getCoinReward();
                        int ticketsEarned = item.getTicketReward();

                        try {
                            if (response.body() != null) {
                                String jsonStr = response.body().string();
                                if (jsonStr.contains("coinReward")) {
                                    JSONObject obj = new JSONObject(jsonStr);
                                    if (obj.has("coinReward")) coinsEarned = obj.getInt("coinReward");
                                    if (obj.has("ticketReward")) ticketsEarned = obj.getInt("ticketReward");
                                }
                            }
                        } catch (Exception ignored) {}

                        item.setCompleted(true);
                        watchedCount++;
                        updateProgressUI();

                        showRewardResultDialog(coinsEarned, ticketsEarned);

                    } else {
                        String err = "Claim failed";
                        try {
                            if (response.errorBody() != null) err = response.errorBody().string();
                        } catch (Exception ignored) {}
                        Toast.makeText(WatchVideoActivity.this, err, Toast.LENGTH_SHORT).show();
                        if (adapter != null) adapter.notifyItemChanged(position);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    hideLoading();
                    item.setLoading(false);
                    if (adapter != null) adapter.notifyItemChanged(position);
                    Toast.makeText(WatchVideoActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> {
            hideLoading();
            item.setLoading(false);
            if (adapter != null) adapter.notifyItemChanged(position);
            Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showRewardResultDialog(int coins, int tickets) {
        if (isFinishing() || isDestroyed()) return;

        try {
            Dialog d = new Dialog(this);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(R.layout.dialog_spin_result);

            TextView txtTitle = d.findViewById(R.id.txtTitle);
            TextView txtWin = d.findViewById(R.id.txtWinAmount);
            View layoutTicket = d.findViewById(R.id.layoutWinTicket);
            TextView txtTicketAmount = d.findViewById(R.id.txtWinTicketAmount);
            TextView txtBal = d.findViewById(R.id.txtCurrentBalance);
            MaterialButton ok = d.findViewById(R.id.btnOk);

            if (txtTitle != null) {
                txtTitle.setText("Video Bonus Claimed! 🎉");
            }
            if (txtWin != null) {
                txtWin.setText("+" + coins + " Coins");
            }
            if (tickets > 0) {
                if (layoutTicket != null) layoutTicket.setVisibility(View.VISIBLE);
                if (txtTicketAmount != null) {
                    txtTicketAmount.setText("+" + tickets + " Ticket" + (tickets > 1 ? "s" : ""));
                }
            } else if (layoutTicket != null) {
                layoutTicket.setVisibility(View.GONE);
            }

            if (txtBal != null) {
                txtBal.setText("Balance: " + userCoins + " Coins");
            }

            if (ok != null) {
                ok.setText("COLLECT REWARD 🎁");
                ok.setOnClickListener(v -> {
                    try { d.dismiss(); } catch (Exception ignored) {}
                });
            }

            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            SuccessAnimationHelper.animate(d);
            d.show();

        } catch (Exception e) {
            Toast.makeText(this, "🎉 +" + coins + " Coins & +" + tickets + " Ticket Added!", Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading() {
        if (isFinishing() || isDestroyed()) return;
        try {
            hideLoading();
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            loadingDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            loadingDialog.show();
        } catch (Exception ignored) {}
    }

    private void hideLoading() {
        if (loadingDialog != null) {
            try {
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            } catch (Exception ignored) {}
            loadingDialog = null;
        }
    }
}
