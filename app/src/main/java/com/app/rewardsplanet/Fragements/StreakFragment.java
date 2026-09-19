package com.app.rewardsplanet.Fragements;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.Activitys.MainActivity;
import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.app.rewardsplanet.repository.UserRepository;
import com.app.rewardsplanet.utils.SuccessAnimationHelper;
import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * StreakFragment — 7-Day Progressive Daily Streak Feature.
 *
 * Distinct from Daily Bonus (HomeFragment 5-sec hold card).
 * Features:
 *  - Backend API status sync with Firestore fallback
 *  - Independent Firestore keys (`streak_count` & `streak_claimed_date`)
 *  - Progressive 7-day rewards (+10, +20, +30, +40, +50, +75, +100)
 *  - AdMob Rewarded Ad integration on claim
 */
public class StreakFragment extends Fragment {

    private ShimmerFrameLayout shimmerContainer;
    private LinearLayout        contentLayout;
    private GridLayout          shimmerGrid;
    private GridLayout          streakContainer;
    private MaterialButton      btnClaim;
    private ImageView           btnBack;
    private TextView            txtHeaderStreak;

    // AdMob Ads
    private RewardedAd rewardedAd = null;
    private boolean    adLoading  = false;
    private AdView     adView     = null;

    private ApiService        apiService;
    private FirebaseFirestore db;
    private UserPref          userPref;

    private int     currentStreak  = 0;
    private boolean isClaimedToday = false;

    private OnBackPressedCallback backPressedCallback;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_fragment_streak,
                container,
                false
        );

        // Find Views
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        contentLayout    = view.findViewById(R.id.contentLayout);
        shimmerGrid      = view.findViewById(R.id.shimmerGrid);
        streakContainer  = view.findViewById(R.id.streakContainer);
        btnClaim         = view.findViewById(R.id.btnClaim);
        btnBack          = view.findViewById(R.id.btnBack);
        txtHeaderStreak  = view.findViewById(R.id.txtHeaderStreak);

        // API & DB
        apiService = ApiClient.getClient().create(ApiService.class);
        db         = FirebaseFirestore.getInstance();
        if (getContext() != null) {
            userPref = new UserPref(getContext());
        }

        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> loadHomeFragment());
        }

        setupBackPressed();
        setupShimmerGrid();
        applyGreyShimmer();

        if (shimmerContainer != null) {
            shimmerContainer.startShimmer();
        }

        loadStreakStatus();

        // Init Ads (Bottom Banner & Rewarded Video)
        initAds(view);

        // Claim button
        if (btnClaim != null) {
            btnClaim.setOnClickListener(v -> claimStreak());
        }

        return view;
    }

    // =========================================================
    // BACK PRESS
    // =========================================================

    private void setupBackPressed() {
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                loadHomeFragment();
            }
        };

        if (getActivity() != null) {
            getActivity().getOnBackPressedDispatcher().addCallback(
                    getViewLifecycleOwner(),
                    backPressedCallback
            );
        }
    }

    private void loadHomeFragment() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.selectNav(activity.navHome);
            activity.loadFragment(new HomeFragment());
        }
    }

    // =========================================================
    // SHIMMER
    // =========================================================

    private void setupShimmerGrid() {
        if (shimmerGrid == null || getContext() == null) return;
        shimmerGrid.removeAllViews();

        for (int i = 0; i < 7; i++) {
            View item = LayoutInflater.from(requireContext()).inflate(
                    R.layout.item_shimmer_streak,
                    shimmerGrid,
                    false
            );

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            item.setLayoutParams(params);

            shimmerGrid.addView(item);
        }
    }

    private void applyGreyShimmer() {
        if (shimmerContainer == null) return;
        Shimmer shimmer = new Shimmer.ColorHighlightBuilder()
                .setBaseColor(Color.parseColor("#64748B"))
                .setHighlightColor(Color.parseColor("#FFFFFF"))
                .setIntensity(1.0f)
                .setDropoff(0.1f)
                .setDuration(800)
                .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .build();
        shimmerContainer.setShimmer(shimmer);
    }

    // =========================================================
    // LOAD STREAK STATUS (Backend API + Firestore Fallback)
    // =========================================================

    private void loadStreakStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showContent();
            return;
        }

        // Pre-load from UserPref for instant display
        if (userPref != null) {
            int cachedStreak = userPref.getStreakCount();
            if (cachedStreak > 0) {
                currentStreak = cachedStreak;
            }
            String cachedDate = userPref.getStreakClaimedDate();
            if (getTodayDate().equals(cachedDate)) {
                isClaimedToday = true;
            }
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = result.getToken();
            if (token == null) {
                loadStreakFromFirestore(user.getUid());
                return;
            }

            apiService.getStreakStatus(token).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String res = response.body().string();
                            JSONObject json = new JSONObject(res);

                            // Inspect root and optional nested data object
                            JSONObject dataObj = json.optJSONObject("data");
                            JSONObject source  = (dataObj != null) ? dataObj : json;

                            int serverStreak = source.optInt("streak",
                                    source.optInt("streak_count",
                                    source.optInt("current_streak", -1)));

                            boolean serverClaimed = source.optBoolean("claimedToday",
                                    source.optBoolean("claimed_today",
                                    source.optBoolean("claimed", false)));

                            if (serverStreak >= 0) {
                                currentStreak  = serverStreak;
                                isClaimedToday = serverClaimed;
                                showContent();
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // Fall back to Firestore if API status is invalid or failed
                    loadStreakFromFirestore(user.getUid());
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    loadStreakFromFirestore(user.getUid());
                }
            });
        }).addOnFailureListener(e -> loadStreakFromFirestore(user.getUid()));
    }

    private void loadStreakFromFirestore(String uid) {
        if (uid == null) {
            showContent();
            return;
        }

        String today = getTodayDate();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Long streakLong = doc.getLong("streak_count");
                        if (streakLong != null && streakLong > 0) {
                            currentStreak = streakLong.intValue();
                        }

                        String lastClaimDate = doc.getString("streak_claimed_date");
                        if (lastClaimDate == null) {
                            lastClaimDate = doc.getString("last_streak_date");
                        }

                        if (lastClaimDate != null && lastClaimDate.startsWith(today)) {
                            isClaimedToday = true;
                        } else {
                            isClaimedToday = false;
                        }
                    }
                    showContent();
                })
                .addOnFailureListener(e -> showContent());
    }

    // =========================================================
    // SHOW CONTENT
    // =========================================================

    private void showContent() {
        if (!isAdded()) return;

        if (shimmerContainer != null) {
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
        }

        if (contentLayout != null) {
            contentLayout.setVisibility(View.VISIBLE);
        }

        setupStreakUI();
    }

    // =========================================================
    // STREAK UI
    // =========================================================

    private void setupStreakUI() {
        if (streakContainer == null || getContext() == null) return;
        streakContainer.removeAllViews();

        if (txtHeaderStreak != null) {
            txtHeaderStreak.setText("🔥 " + currentStreak + " Day" + (currentStreak == 1 ? "" : "s"));
        }

        int[] rewards = {10, 20, 30, 40, 50, 75, 100};

        // Calculate cycle status (1..7 loop)
        int completedInCycle;
        int activeCardIndex;

        if (isClaimedToday) {
            completedInCycle = (currentStreak % 7 == 0) ? 7 : (currentStreak % 7);
            activeCardIndex  = completedInCycle - 1;
        } else {
            completedInCycle = currentStreak % 7;
            activeCardIndex  = completedInCycle;
        }

        for (int i = 0; i < 7; i++) {
            View item = LayoutInflater.from(requireContext()).inflate(
                    R.layout.item_streak,
                    streakContainer,
                    false
            );

            View cardParent   = item.findViewById(R.id.cardParent);
            TextView day       = item.findViewById(R.id.txtDay);
            TextView reward    = item.findViewById(R.id.txtReward);
            ImageView fire     = item.findViewById(R.id.imgFire);
            ImageView smallCoin = item.findViewById(R.id.imgSmallCoin);

            if (isClaimedToday) {
                if (i < completedInCycle) {
                    // COMPLETED IN CURRENT CYCLE
                    day.setText(i == activeCardIndex ? "TODAY" : "DAY " + (i + 1));
                    day.setBackgroundResource(R.drawable.bg_streak_badge_completed);
                    day.setTextColor(Color.parseColor("#047857"));

                    fire.setImageResource(R.drawable.ic_success);
                    fire.clearColorFilter();
                    fire.setAlpha(1.0f);
                    reward.setText("+" + rewards[i]);
                    reward.setTextColor(Color.parseColor("#047857"));
                    if (smallCoin != null) {
                        smallCoin.setImageResource(R.drawable.ic_coin);
                        smallCoin.setAlpha(1.0f);
                    }
                    cardParent.setBackgroundResource(R.drawable.bg_streak_card_completed);
                    cardParent.setScaleX(1.0f);
                    cardParent.setScaleY(1.0f);
                } else {
                    // LOCKED / FUTURE DAYS
                    day.setText("DAY " + (i + 1));
                    day.setBackgroundResource(R.drawable.bg_streak_badge_locked);
                    day.setTextColor(Color.parseColor("#64748B"));

                    fire.setImageResource(R.drawable.ic_coin);
                    fire.clearColorFilter();
                    fire.setAlpha(0.45f);
                    reward.setText("+" + rewards[i]);
                    reward.setTextColor(Color.parseColor("#64748B"));
                    if (smallCoin != null) {
                        smallCoin.setImageResource(R.drawable.ic_coin);
                        smallCoin.setAlpha(0.45f);
                    }
                    cardParent.setBackgroundResource(R.drawable.bg_streak_card_locked);
                    cardParent.setScaleX(1.0f);
                    cardParent.setScaleY(1.0f);
                }
            } else {
                if (i < completedInCycle) {
                    // COMPLETED
                    day.setText("DAY " + (i + 1));
                    day.setBackgroundResource(R.drawable.bg_streak_badge_completed);
                    day.setTextColor(Color.parseColor("#047857"));

                    fire.setImageResource(R.drawable.ic_success);
                    fire.clearColorFilter();
                    fire.setAlpha(1.0f);
                    reward.setText("+" + rewards[i]);
                    reward.setTextColor(Color.parseColor("#047857"));
                    if (smallCoin != null) {
                        smallCoin.setImageResource(R.drawable.ic_coin);
                        smallCoin.setAlpha(1.0f);
                    }
                    cardParent.setBackgroundResource(R.drawable.bg_streak_card_completed);
                    cardParent.setScaleX(1.0f);
                    cardParent.setScaleY(1.0f);
                } else if (i == activeCardIndex) {
                    // ACTIVE / TODAY
                    day.setText("TODAY");
                    day.setBackgroundResource(R.drawable.bg_streak_badge_active);
                    day.setTextColor(Color.parseColor("#B45309"));

                    fire.setImageResource(R.drawable.ic_coin);
                    fire.clearColorFilter();
                    fire.setAlpha(1.0f);
                    reward.setText("+" + rewards[i]);
                    reward.setTextColor(Color.parseColor("#B45309"));
                    if (smallCoin != null) {
                        smallCoin.setImageResource(R.drawable.ic_coin);
                        smallCoin.setAlpha(1.0f);
                    }
                    cardParent.setBackgroundResource(R.drawable.bg_streak_card_active);

                    cardParent.setScaleX(1.05f);
                    cardParent.setScaleY(1.05f);
                } else {
                    // LOCKED / FUTURE DAYS
                    day.setText("DAY " + (i + 1));
                    day.setBackgroundResource(R.drawable.bg_streak_badge_locked);
                    day.setTextColor(Color.parseColor("#64748B"));

                    fire.setImageResource(R.drawable.ic_coin);
                    fire.clearColorFilter();
                    fire.setAlpha(0.45f);
                    reward.setText("+" + rewards[i]);
                    reward.setTextColor(Color.parseColor("#64748B"));
                    if (smallCoin != null) {
                        smallCoin.setImageResource(R.drawable.ic_coin);
                        smallCoin.setAlpha(0.45f);
                    }
                    cardParent.setBackgroundResource(R.drawable.bg_streak_card_locked);
                    cardParent.setScaleX(1.0f);
                    cardParent.setScaleY(1.0f);
                }
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            item.setLayoutParams(params);

            streakContainer.addView(item);
        }

        if (btnClaim != null) {
            if (isClaimedToday) {
                btnClaim.setEnabled(false);
                btnClaim.setAlpha(0.85f);
                btnClaim.setTextColor(Color.parseColor("#FFFFFF"));
                btnClaim.setText("CLAIMED TODAY");
                if (getContext() != null) {
                    btnClaim.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.color_blue));
                }
            } else {
                btnClaim.setEnabled(true);
                btnClaim.setAlpha(1.0f);
                btnClaim.setTextColor(Color.parseColor("#FFFFFF"));
                btnClaim.setText("🎁 CLAIM TODAY'S REWARD");
                if (getContext() != null) {
                    btnClaim.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.bule_color2));
                }
            }
        }
    }

    // =========================================================
    // ADMOB ADS INITIALIZATION
    // =========================================================

    private void initAds(View view) {
        if (getContext() == null) return;
        try {
            MobileAds.initialize(requireContext(), status -> {});

            // Initialize bottom Banner Ad
            if (view != null) {
                adView = view.findViewById(R.id.adView);
                if (adView != null) {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    adView.loadAd(adRequest);
                }
            }

            // Pre-load Rewarded Ad for claim action
            loadRewardedAd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRewardedAd() {
        if (getContext() == null || adLoading) return;
        adLoading = true;

        String adUnitId;
        try {
            adUnitId = getString(R.string.rewarded_ad_unit_id);
        } catch (Exception e) {
            adUnitId = AdsManager.REWARDED_AD_ID;
        }
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            adUnitId = AdsManager.REWARDED_AD_ID;
        }

        RewardedAd.load(
            requireContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    rewardedAd = ad;
                    adLoading  = false;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    rewardedAd = null;
                    adLoading  = false;
                }
            }
        );
    }

    // =========================================================
    // CLAIM STREAK
    // =========================================================

    private void claimStreak() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (isClaimedToday) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Streak already claimed today", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (btnClaim != null) btnClaim.setEnabled(false);

        int[] rewards = {10, 20, 30, 40, 50, 75, 100};
        int cycleIndex = currentStreak % 7;
        int expectedReward = rewards[cycleIndex];

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = result.getToken();
            if (token == null) {
                executeFirestoreStreakClaim(user.getUid(), expectedReward);
                return;
            }

            apiService.claimStreak(token).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            int claimedReward = json.optInt("reward", expectedReward);
                            currentStreak     = json.optInt("streak", currentStreak + 1);
                            isClaimedToday    = true;

                            executeFirestoreStreakClaimSuccess(user.getUid(), claimedReward);
                            return;

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // Fallback to Firestore claim logic
                    executeFirestoreStreakClaim(user.getUid(), expectedReward);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    executeFirestoreStreakClaim(user.getUid(), expectedReward);
                }
            });
        }).addOnFailureListener(e -> executeFirestoreStreakClaim(user.getUid(), expectedReward));
    }

    private void executeFirestoreStreakClaim(String uid, int rewardAmount) {
        if (uid == null) {
            if (btnClaim != null) btnClaim.setEnabled(true);
            return;
        }

        String today = getTodayDate();
        currentStreak = currentStreak + 1;
        isClaimedToday = true;

        Map<String, Object> updates = new HashMap<>();
        updates.put("coins", FieldValue.increment(rewardAmount));
        updates.put("streak_count", currentStreak);
        updates.put("streak_claimed_date", today);
        updates.put("last_streak_date", today);

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> executeFirestoreStreakClaimSuccess(uid, rewardAmount))
                .addOnFailureListener(e -> {
                    // Try set with merge if update fails
                    db.collection("users")
                            .document(uid)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> executeFirestoreStreakClaimSuccess(uid, rewardAmount))
                            .addOnFailureListener(e2 -> {
                                executeFirestoreStreakClaimSuccess(uid, rewardAmount);
                            });
                });
    }

    private void executeFirestoreStreakClaimSuccess(String uid, int rewardAmount) {
        if (userPref != null) {
            userPref.addCoins(rewardAmount);
            userPref.saveStreak(currentStreak, getTodayDate());
        }

        processClaimWithAd(rewardAmount);
    }

    private void processClaimWithAd(int claimedReward) {
        // Haptic vibration feedback
        triggerVibration();

        // Sync Firestore user profile & coins
        if (getContext() != null) {
            UserRepository.getInstance(getContext()).refreshCurrentUser();
        }

        // Update UI
        setupStreakUI();

        // Show Rewarded Ad if available, then show Reward Dialog
        if (rewardedAd != null && getActivity() != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    rewardedAd = null;
                    loadRewardedAd();
                    showStreakRewardDialog(claimedReward);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    rewardedAd = null;
                    loadRewardedAd();
                    showStreakRewardDialog(claimedReward);
                }
            });

            rewardedAd.show(getActivity(), rewardItem -> {
                // User watched rewarded ad
            });
        } else {
            // If ad not loaded or unavailable, present reward dialog directly
            loadRewardedAd();
            showStreakRewardDialog(claimedReward);
        }
    }

    // =========================================================
    // HAPTIC VIBRATION
    // =========================================================

    private void triggerVibration() {
        try {
            if (getContext() != null) {
                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(120);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================
    // STREAK REWARD DIALOG
    // =========================================================

    private void showStreakRewardDialog(int rewardAmount) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_spin_result);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView txtTitle          = dialog.findViewById(R.id.txtTitle);
        TextView txtWinAmount      = dialog.findViewById(R.id.txtWinAmount);
        TextView txtCurrentBalance = dialog.findViewById(R.id.txtCurrentBalance);
        Button   btnOk             = dialog.findViewById(R.id.btnOk);

        if (txtTitle != null)          txtTitle.setText("Streak Claimed! 🔥");
        if (txtWinAmount != null)      txtWinAmount.setText("+" + rewardAmount + " COINS");

        long currentCoins = (userPref != null) ? userPref.getCoins() : 0;
        if (txtCurrentBalance != null) txtCurrentBalance.setText("Current Balance: " + currentCoins + " Coins");

        if (btnOk != null) {
            btnOk.setText("COLLECT REWARD");
            btnOk.setOnClickListener(v -> dialog.dismiss());
        }

        SuccessAnimationHelper.animate(dialog);

        dialog.show();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    // =========================================================
    // DATE HELPERS
    // =========================================================

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroyView() {
        if (adView != null) {
            adView.destroy();
        }
        if (shimmerContainer != null) {
            shimmerContainer.stopShimmer();
        }
        super.onDestroyView();
    }
}