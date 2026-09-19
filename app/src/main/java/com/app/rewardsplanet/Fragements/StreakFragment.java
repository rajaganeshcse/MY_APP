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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreakFragment extends Fragment {

    private ShimmerFrameLayout shimmerContainer;
    private LinearLayout        contentLayout;
    private GridLayout          shimmerGrid;
    private GridLayout          streakContainer;
    private Button              btnClaim;
    private ImageView           btnBack;
    private TextView            txtHeaderStreak;

    // AdMob Ads
    private RewardedAd rewardedAd = null;
    private boolean    adLoading  = false;
    private AdView     adView     = null;

    private ApiService apiService;

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

        // API
        apiService = ApiClient.getClient().create(ApiService.class);

        // Back button
        btnBack.setOnClickListener(v -> loadHomeFragment());

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
        btnClaim.setOnClickListener(v -> claimStreak());

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
    // LOAD STREAK STATUS
    // =========================================================

    private void loadStreakStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showContent();
            return;
        }

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = result.getToken();
            if (token == null) {
                showContent();
                return;
            }

            apiService.getStreakStatus(token).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String res = response.body().string();
                            JSONObject json = new JSONObject(res);

                            currentStreak  = json.optInt("streak", 0);
                            isClaimedToday = json.optBoolean("claimedToday", false);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    showContent();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading streak", Toast.LENGTH_SHORT).show();
                    }
                    showContent();
                }
            });
        }).addOnFailureListener(e -> showContent());
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
                btnClaim.setAlpha(1.0f);
                btnClaim.setTextColor(Color.parseColor("#FFFFFF"));
                btnClaim.setText("✓ CLAIMED TODAY");
                btnClaim.setBackgroundResource(R.drawable.bg_btn_claim_done);
            } else {
                btnClaim.setEnabled(true);
                btnClaim.setAlpha(1.0f);
                btnClaim.setTextColor(Color.parseColor("#FFFFFF"));
                btnClaim.setText("🎁 CLAIM TODAY'S REWARD");
                btnClaim.setBackgroundResource(R.drawable.bg_btn_claim_active);
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

        btnClaim.setEnabled(false);

        user.getIdToken(true).addOnSuccessListener(result -> {
            String token = result.getToken();
            if (token == null) {
                btnClaim.setEnabled(true);
                return;
            }

            apiService.claimStreak(token).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            int claimedReward = json.optInt("reward", 10);
                            currentStreak     = json.optInt("streak", currentStreak);
                            isClaimedToday    = true;

                            // Process Reward & Play Rewarded Ad before showing Dialog
                            processClaimWithAd(claimedReward);

                        } catch (Exception e) {
                            btnClaim.setEnabled(true);
                            e.printStackTrace();
                        }
                    } else {
                        btnClaim.setEnabled(true);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Unable to claim reward", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    btnClaim.setEnabled(true);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }).addOnFailureListener(e -> {
            btnClaim.setEnabled(true);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Authentication error", Toast.LENGTH_SHORT).show();
            }
        });
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

        long currentCoins = new UserPref(requireContext()).getCoins();
        if (txtCurrentBalance != null) txtCurrentBalance.setText("Current Balance: " + currentCoins + " Coins");

        if (btnOk != null) {
            btnOk.setText("COLLECT REWARD");
            btnOk.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
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