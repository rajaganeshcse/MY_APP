package com.app.rewardsplanet.lucky_draw;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.models.HitRewardzModel;
import com.app.rewardsplanet.repository.UserRepository;
import com.app.rewardsplanet.utils.SuccessAnimationHelper;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HitRewardzActivity extends AppCompatActivity implements HitRewardzAdapter.OnHitzClickListener {

    private static final String TAG = "HitRewardzActivity";

    private UserPref userPref;
    private FirebaseFirestore db;

    private TextView txtCoins;
    private TextView txtProgressCount;
    private ProgressBar progressHitzAds;
    private RecyclerView recyclerView;
    private HitRewardzAdapter adapter;

    private List<HitRewardzModel> hitzList = new ArrayList<>();
    private RewardedAd rewardedAd;
    private HitRewardzModel pendingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hit_rewardz);

        userPref = new UserPref(this);
        db = FirebaseFirestore.getInstance();

        makeFullScreen();
        initViews();
        setupHitzOffers();
        loadRewardedAd();

        updateProgressUI();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        txtCoins = findViewById(R.id.txtCoins);
        txtProgressCount = findViewById(R.id.txtProgressCount);
        progressHitzAds = findViewById(R.id.progressHitzAds);
        recyclerView = findViewById(R.id.recyclerViewHitRewardz);

        if (txtCoins != null) {
            txtCoins.setText(String.valueOf(userPref.getCoins()));
        }

        try {
            MobileAds.initialize(this, status -> {});
        } catch (Exception ignored) {}
    }

    private void setupHitzOffers() {
        hitzList.clear();

        int[] payouts = {10, 25, 25, 25, 50};
        String[] titles = {
                "Mega Hitz Offer #1",
                "Super Video Task #2",
                "Ultra Hitz Offer #3",
                "Premium Ad Task #4",
                "Jackpot Hitz Task #5"
        };

        for (int i = 0; i < 5; i++) {
            int taskId = i + 1;
            boolean completed = userPref.isHitzTaskCompleted(taskId);
            hitzList.add(new HitRewardzModel(taskId, titles[i], payouts[i], "30s Long Ad", completed));
        }

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new HitRewardzAdapter(hitzList, this);
            recyclerView.setAdapter(adapter);
        }
    }

    private void updateProgressUI() {
        int completedCount = userPref.getTodayHitzCount();
        if (txtProgressCount != null) {
            txtProgressCount.setText(completedCount + " / 5");
        }
        if (progressHitzAds != null) {
            progressHitzAds.setProgress(Math.min(completedCount, 5));
        }
        if (txtCoins != null) {
            txtCoins.setText(String.valueOf(userPref.getCoins()));
        }
    }

    @Override
    public void onHitzClick(HitRewardzModel item) {
        if (item.isCompleted()) {
            Toast.makeText(this, "Task already completed today!", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingItem = item;

        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    grantHitzReward(pendingItem);
                    loadRewardedAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    grantHitzReward(pendingItem);
                    loadRewardedAd();
                }
            });

            rewardedAd.show(this, rewardItem -> Log.d(TAG, "User completed 30s Long Ad!"));
        } else {
            Toast.makeText(this, "Loading 30s Long Ad... Please try again in a moment", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
            grantHitzReward(item);
        }
    }

    private void grantHitzReward(HitRewardzModel item) {
        if (item == null || item.isCompleted()) return;

        int reward = item.getCoins();
        item.setCompleted(true);

        String uid = userPref.getUid();
        if (uid != null && !uid.isEmpty()) {
            db.collection("users").document(uid).update(
                    "coins", FieldValue.increment(reward)
            ).addOnFailureListener(e -> Log.e(TAG, "Error updating Firestore Hitz reward", e));
        }

        userPref.addCoins(reward);
        userPref.setHitzTaskCompleted(item.getId());

        UserRepository.getInstance(this).refreshCurrentUser();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        updateProgressUI();
        showHitzRewardDialog(reward, item.getTitle());
    }

    private void showHitzRewardDialog(int rewardCoins, String offerTitle) {
        if (isFinishing() || isDestroyed()) return;

        try {
            Dialog d = new Dialog(this);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(R.layout.dialog_spin_result);

            TextView txtTitle = d.findViewById(R.id.txtTitle);
            TextView txtWinAmount = d.findViewById(R.id.txtWinAmount);
            TextView txtCurrentBalance = d.findViewById(R.id.txtCurrentBalance);
            MaterialButton btnOk = d.findViewById(R.id.btnOk);

            if (txtTitle != null) {
                txtTitle.setText("Hitz Offer Completed! ⚡");
            }
            if (txtWinAmount != null) {
                txtWinAmount.setText("+" + rewardCoins + " Coins");
            }
            if (txtCurrentBalance != null) {
                txtCurrentBalance.setText("Balance: " + userPref.getCoins() + " Coins");
            }

            if (btnOk != null) {
                btnOk.setText("CLAIM REWARD");
                btnOk.setOnClickListener(v -> {
                    try { d.dismiss(); } catch (Exception ignored) {}
                });
            }

            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            SuccessAnimationHelper.animate(d);
            d.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing hitz reward dialog", e);
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
            }
        });
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

    @Override
    protected void onResume() {
        super.onResume();
        makeFullScreen();
        updateProgressUI();
    }
}
