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

import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HitRewardzActivity extends AppCompatActivity implements HitRewardzAdapter.OnHitzClickListener {

    private static final String TAG = "HitRewardzActivity";

    private UserPref userPref;
    private FirebaseFirestore db;
    private ApiService apiService;

    private TextView txtCoins;
    private TextView txtProgressCount;
    private ProgressBar progressHitzAds;
    private RecyclerView recyclerView;
    private HitRewardzAdapter adapter;

    private TextView txtWeeklyStatusTitle;
    private TextView txtWeeklyResetSubtitle;
    private View cardWeeklyStatus;
    private View layoutCountdownTimer;
    private TextView txtCountdownTimer;
    private boolean isWeeklyClaimed = false;
    private boolean isBackendLoading = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

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
        fetchWeeklyStatus();
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
        txtWeeklyStatusTitle = findViewById(R.id.txtWeeklyStatusTitle);
        txtWeeklyResetSubtitle = findViewById(R.id.txtWeeklyResetSubtitle);
        cardWeeklyStatus = findViewById(R.id.cardWeeklyStatus);
        layoutCountdownTimer = findViewById(R.id.layoutCountdownTimer);
        txtCountdownTimer = findViewById(R.id.txtCountdownTimer);

        if (txtCoins != null) {
            txtCoins.setText(String.valueOf(userPref.getCoins()));
        }

        apiService = ApiClient.getClient().create(ApiService.class);

        try {
            MobileAds.initialize(this, status -> {});
        } catch (Exception ignored) {}
    }

    private void setupHitzOffers() {
        // Default initial setup
        populateOffersFromConfig(
                new int[]{10, 25, 25, 25, 50},
                new String[]{
                        "Mega Hitz Offer #1",
                        "Super Video Task #2",
                        "Ultra Hitz Offer #3",
                        "Premium Ad Task #4",
                        "Jackpot Hitz Task #5"
                },
                "30s Long Ad"
        );

        // Fetch dynamic rewards configuration from Backend Firestore
        fetchBackendRewardsConfig();
    }

    private void fetchBackendRewardsConfig() {
        if (db == null) return;

        db.collection("settings").document("hitz_rewards")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<Long> backendPayouts = (List<Long>) documentSnapshot.get("payouts");
                        List<String> backendTitles = (List<String>) documentSnapshot.get("titles");
                        String duration = documentSnapshot.getString("duration");
                        if (duration == null) duration = "30s Long Ad";

                        if (backendPayouts != null && !backendPayouts.isEmpty()) {
                            int[] payouts = new int[backendPayouts.size()];
                            for (int i = 0; i < backendPayouts.size(); i++) {
                                payouts[i] = backendPayouts.get(i).intValue();
                            }

                            String[] titles;
                            if (backendTitles != null && backendTitles.size() >= payouts.length) {
                                titles = backendTitles.toArray(new String[0]);
                            } else {
                                titles = new String[payouts.length];
                                for (int i = 0; i < payouts.length; i++) {
                                    titles[i] = "Hitz Task #" + (i + 1);
                                }
                            }

                            populateOffersFromConfig(payouts, titles, duration);
                        }
                    } else {
                        // Seed Firestore config document if not present
                        Map<String, Object> config = new HashMap<>();
                        List<Integer> defaultPayouts = new ArrayList<>();
                        defaultPayouts.add(10);
                        defaultPayouts.add(25);
                        defaultPayouts.add(25);
                        defaultPayouts.add(25);
                        defaultPayouts.add(50);
                        config.put("payouts", defaultPayouts);

                        List<String> defaultTitles = new ArrayList<>();
                        defaultTitles.add("Mega Hitz Offer #1");
                        defaultTitles.add("Super Video Task #2");
                        defaultTitles.add("Ultra Hitz Offer #3");
                        defaultTitles.add("Premium Ad Task #4");
                        defaultTitles.add("Jackpot Hitz Task #5");
                        config.put("titles", defaultTitles);

                        config.put("duration", "30s Long Ad");
                        db.collection("settings").document("hitz_rewards").set(config);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load backend hitz config: " + e.getMessage()));
    }

    private void populateOffersFromConfig(int[] payouts, String[] titles, String duration) {
        hitzList.clear();
        int completedCount = userPref.getTodayHitzCount();

        for (int i = 0; i < payouts.length; i++) {
            int taskId = i + 1;
            boolean completed = i < completedCount;
            boolean locked = i > completedCount;
            String title = (titles != null && i < titles.length) ? titles[i] : "Hitz Task #" + taskId;
            hitzList.add(new HitRewardzModel(taskId, title, payouts[i], 5, completed, locked));
        }

        if (recyclerView != null) {
            if (adapter == null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new HitRewardzAdapter(hitzList, this);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }

        updateProgressUI();
    }

    private void updateProgressUI() {
        int completedCount = userPref.getTodayHitzCount();
        if (completedCount >= 5) {
            isWeeklyClaimed = true;
        }

        int displayCount = isWeeklyClaimed ? 5 : Math.min(completedCount, 5);

        if (txtProgressCount != null) {
            txtProgressCount.setText(displayCount + " / 5");
        }
        if (progressHitzAds != null) {
            progressHitzAds.setMax(5);
            progressHitzAds.setProgress(displayCount);
        }
        if (txtCoins != null) {
            txtCoins.setText(String.valueOf(userPref.getCoins()));
        }

        for (int i = 0; i < hitzList.size(); i++) {
            HitRewardzModel item = hitzList.get(i);
            if (isWeeklyClaimed || i < displayCount) {
                item.setCompleted(true);
                item.setLocked(false);
            } else if (i == displayCount) {
                item.setCompleted(false);
                item.setLocked(false);
            } else {
                item.setCompleted(false);
                item.setLocked(true);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onHitzClick(HitRewardzModel item) {
        if (isWeeklyClaimed) {
            Toast.makeText(this, "✓ Weekly reward already claimed! Come back next Monday.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isBackendLoading) {
            Toast.makeText(this, "Checking weekly reward status... Please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item.isCompleted()) {
            Toast.makeText(this, "Task already completed!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item.isLocked()) {
            Toast.makeText(this, "Task is locked 🔒 Watch previous tasks first!", Toast.LENGTH_SHORT).show();
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

            rewardedAd.show(this, rewardItem -> Log.d(TAG, "User completed Rewarded Ad!"));
        } else {
            Toast.makeText(this, "Loading Rewarded Ad... Please try again in a moment", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
            grantHitzReward(item);
        }
    }

    private void grantHitzReward(HitRewardzModel item) {
        if (item == null || item.isCompleted()) return;

        item.setCompleted(true);

        String requestId = UUID.randomUUID().toString();
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("requestId", requestId);
        reqBody.put("taskId", item.getId());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnCompleteListener(task -> {
                String token = task.isSuccessful() && task.getResult() != null ? task.getResult().getToken() : "";
                callBackendClaimReward(token, reqBody, item);
            });
        } else {
            callBackendClaimReward("", reqBody, item);
        }
    }

    private void callBackendClaimReward(String token, Map<String, Object> reqBody, HitRewardzModel item) {
        String authHeader = (token != null && !token.isEmpty()) ? "Bearer " + token : "";
        int fallbackCoins = item.getCoins();
        int fallbackTickets = item.getTickets() > 0 ? item.getTickets() : 5;

        if (apiService != null && !authHeader.isEmpty()) {
            apiService.claimHitzReward(authHeader, reqBody).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String rawJson = response.body().string();
                            JSONObject json = new JSONObject(rawJson);

                            boolean claimed = json.optBoolean("claimed", false);
                            int completedTasksCount = json.optInt("completedTasksCount", item.getId());
                            String message = json.optString("message", "Task completed successfully");
                            String nextReset = json.optString("nextReset", "Monday 12:00 AM");

                            int coinReward = json.optInt("coinReward", fallbackCoins);
                            int ticketReward = json.optInt("ticketReward", fallbackTickets);
                            long totalCoins = json.optLong("totalCoins", userPref.getCoins() + coinReward);
                            long totalTickets = json.optLong("totalTickets", userPref.getTickets() + ticketReward);

                            userPref.setCoins(totalCoins);
                            userPref.setTickets((int) totalTickets);
                            userPref.setHitzTaskCompleted(item.getId());

                            isWeeklyClaimed = claimed || completedTasksCount >= 5 || userPref.getTodayHitzCount() >= 5;
                            updateWeeklyUIState(isWeeklyClaimed, message, nextReset);

                            UserRepository.getInstance(HitRewardzActivity.this).refreshCurrentUser();
                            showHitzRewardDialog(coinReward, ticketReward, item.getTitle());
                            return;
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing backend hitz reward response", e);
                        }
                    }
                    applyFallbackReward(item, fallbackCoins, fallbackTickets);
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Log.e(TAG, "Backend claim hitz reward failed: " + t.getMessage());
                    applyFallbackReward(item, fallbackCoins, fallbackTickets);
                }
            });
        } else {
            applyFallbackReward(item, fallbackCoins, fallbackTickets);
        }
    }

    private void fetchWeeklyStatus() {
        if (apiService == null) return;

        isBackendLoading = true;
        if (txtWeeklyStatusTitle != null) {
            txtWeeklyStatusTitle.setText("⏳ Checking Weekly Status...");
            txtWeeklyStatusTitle.setTextColor(Color.parseColor("#CBD5E1"));
        }
        if (txtWeeklyResetSubtitle != null) {
            txtWeeklyResetSubtitle.setText("Connecting to server... Please wait");
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getToken() != null && !task.getResult().getToken().isEmpty()) {
                    requestWeeklyStatusApi(task.getResult().getToken());
                } else {
                    user.getIdToken(true).addOnCompleteListener(task2 -> {
                        String token = (task2.isSuccessful() && task2.getResult() != null) ? task2.getResult().getToken() : "";
                        requestWeeklyStatusApi(token);
                    });
                }
            });
        } else {
            requestWeeklyStatusApi("");
        }
    }

    private void requestWeeklyStatusApi(String token) {
        String authHeader = (token != null && !token.isEmpty()) ? "Bearer " + token : "";
        if (authHeader.isEmpty()) {
            isBackendLoading = false;
            updateWeeklyUIError();
            return;
        }

        apiService.getHitzRewardsStatus(authHeader).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                isBackendLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        JSONObject json = new JSONObject(rawJson);
                        boolean claimed = json.optBoolean("claimed", false);
                        int completedTasksCount = json.optInt("completedTasksCount", 0);
                        String message = json.optString("message", "");
                        String nextReset = json.optString("nextReset", "Monday 12:00 AM");

                        if (completedTasksCount > 0) {
                            for (int i = 1; i <= completedTasksCount; i++) {
                                userPref.setHitzTaskCompleted(i);
                            }
                        }

                        isWeeklyClaimed = claimed || completedTasksCount >= 5 || userPref.getTodayHitzCount() >= 5;
                        updateWeeklyUIState(isWeeklyClaimed, message, nextReset);
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing weekly status response", e);
                    }
                }
                updateWeeklyUIError();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                isBackendLoading = false;
                Log.e(TAG, "Failed to fetch weekly status: " + t.getMessage());
                updateWeeklyUIError();
            }
        });
    }

    private long getNextMondayResetMillis() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Kolkata");
        Calendar now = Calendar.getInstance(tz);
        Calendar nextMonday = Calendar.getInstance(tz);
        nextMonday.set(Calendar.HOUR_OF_DAY, 0);
        nextMonday.set(Calendar.MINUTE, 0);
        nextMonday.set(Calendar.SECOND, 0);
        nextMonday.set(Calendar.MILLISECOND, 0);

        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int daysUntilMonday = (Calendar.MONDAY - dayOfWeek + 7) % 7;
        if (daysUntilMonday == 0) {
            if (!now.before(nextMonday)) {
                daysUntilMonday = 7;
            }
        }
        nextMonday.add(Calendar.DAY_OF_MONTH, daysUntilMonday);
        return nextMonday.getTimeInMillis();
    }

    private void startCountdownTimer() {
        stopCountdownTimer();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long targetMillis = getNextMondayResetMillis();
                long nowMillis = System.currentTimeMillis();
                long diffMillis = targetMillis - nowMillis;

                if (diffMillis <= 0) {
                    if (txtCountdownTimer != null) {
                        txtCountdownTimer.setText("00d : 00h : 00m : 00s");
                    }
                    fetchWeeklyStatus();
                    return;
                }

                long seconds = (diffMillis / 1000) % 60;
                long minutes = (diffMillis / (1000 * 60)) % 60;
                long hours = (diffMillis / (1000 * 60 * 60)) % 24;
                long days = diffMillis / (1000 * 60 * 60 * 24);

                String timeStr = String.format(Locale.US, "%02dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds);
                if (txtCountdownTimer != null) {
                    txtCountdownTimer.setText(timeStr);
                }

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopCountdownTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateWeeklyUIState(boolean claimed, String message, String nextReset) {
        isWeeklyClaimed = claimed;
        if (claimed) {
            if (txtWeeklyStatusTitle != null) {
                txtWeeklyStatusTitle.setText("✓ Reward Claimed");
                txtWeeklyStatusTitle.setTextColor(Color.parseColor("#4ADE80"));
            }
            if (txtWeeklyResetSubtitle != null) {
                txtWeeklyResetSubtitle.setText("Come back next week • Reset every Monday 00:00 AM IST");
            }
            if (layoutCountdownTimer != null) {
                layoutCountdownTimer.setVisibility(View.VISIBLE);
            }
            startCountdownTimer();
        } else {
            if (txtWeeklyStatusTitle != null) {
                txtWeeklyStatusTitle.setText("🎁 Hit Reward - Weekly Available");
                txtWeeklyStatusTitle.setTextColor(Color.WHITE);
            }
            if (txtWeeklyResetSubtitle != null) {
                txtWeeklyResetSubtitle.setText("Complete offers before Monday 12:00 AM reset!");
            }
            if (layoutCountdownTimer != null) {
                layoutCountdownTimer.setVisibility(View.GONE);
            }
            stopCountdownTimer();
        }

        updateProgressUI();

        if (cardWeeklyStatus != null) {
            cardWeeklyStatus.setOnClickListener(null);
            cardWeeklyStatus.setClickable(false);
        }
    }

    private void updateWeeklyUIError() {
        if (txtWeeklyStatusTitle != null) {
            txtWeeklyStatusTitle.setText("⚠️ Connection Error");
            txtWeeklyStatusTitle.setTextColor(Color.parseColor("#F87171"));
        }
        if (txtWeeklyResetSubtitle != null) {
            txtWeeklyResetSubtitle.setText("Unable to verify weekly status. Tap card to retry.");
        }
        if (layoutCountdownTimer != null) {
            layoutCountdownTimer.setVisibility(View.GONE);
        }
        stopCountdownTimer();
        if (cardWeeklyStatus != null) {
            cardWeeklyStatus.setClickable(true);
            cardWeeklyStatus.setFocusable(true);
            cardWeeklyStatus.setOnClickListener(v -> {
                Toast.makeText(HitRewardzActivity.this, "Checking connection... Please wait", Toast.LENGTH_SHORT).show();
                fetchWeeklyStatus();
            });
        }
    }

    private void applyFallbackReward(HitRewardzModel item, int rewardCoins, int rewardTickets) {
        String uid = userPref.getUid();
        if (uid != null && !uid.isEmpty()) {
            db.collection("users").document(uid).update(
                    "coins", FieldValue.increment(rewardCoins),
                    "tickets", FieldValue.increment(rewardTickets)
            ).addOnFailureListener(e -> Log.e(TAG, "Error updating Firestore Hitz reward fallback", e));
        }

        userPref.addCoins(rewardCoins);
        userPref.setTickets(userPref.getTickets() + rewardTickets);
        userPref.setHitzTaskCompleted(item.getId());

        UserRepository.getInstance(this).refreshCurrentUser();
        updateProgressUI();
        showHitzRewardDialog(rewardCoins, rewardTickets, item.getTitle());
    }

    private void showHitzRewardDialog(int rewardCoins, int rewardTickets, String offerTitle) {
        if (isFinishing() || isDestroyed()) return;

        try {
            Dialog d = new Dialog(this);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(R.layout.dialog_spin_result);

            TextView txtTitle = d.findViewById(R.id.txtTitle);
            TextView txtWinAmount = d.findViewById(R.id.txtWinAmount);
            View layoutWinTicket = d.findViewById(R.id.layoutWinTicket);
            TextView txtWinTicketAmount = d.findViewById(R.id.txtWinTicketAmount);
            TextView txtCurrentBalance = d.findViewById(R.id.txtCurrentBalance);
            MaterialButton btnOk = d.findViewById(R.id.btnOk);

            if (txtTitle != null) {
                txtTitle.setText("Hitz Offer Completed! ⚡");
            }
            if (txtWinAmount != null) {
                txtWinAmount.setText("+" + rewardCoins + " Coins");
            }

            if (rewardTickets > 0 && layoutWinTicket != null && txtWinTicketAmount != null) {
                txtWinTicketAmount.setText("+" + rewardTickets + " Ticket" + (rewardTickets > 1 ? "s" : ""));
                layoutWinTicket.setVisibility(View.VISIBLE);
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
        RewardedAd.load(this, com.app.rewardsplanet.ads.AdsManager.REWARDED_AD_ID, adRequest, new RewardedAdLoadCallback() {
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
        if (isWeeklyClaimed) {
            startCountdownTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCountdownTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdownTimer();
    }
}
