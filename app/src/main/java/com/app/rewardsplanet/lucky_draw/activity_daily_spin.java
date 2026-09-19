package com.app.rewardsplanet.lucky_draw;

import android.animation.*;
import android.app.Dialog;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.models.SpinResponse;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.app.rewardsplanet.utils.SuccessAnimationHelper;
import com.google.android.material.button.MaterialButton;

import com.google.android.gms.ads.*;
import com.google.android.gms.ads.rewarded.*;

import retrofit2.*;

/**
 * activity_daily_spin — Backend-authoritative Spin Wheel Activity
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  SPIN COUNT FLOW                                                          │
 * │   1. Open screen  → show LOCAL count immediately (no "Loading..." flicker) │
 * │   2. loadSpinStatus() → backend confirms real count → update display      │
 * │   3. After every spin → backend spin response updates count in real-time  │
 * │   4. Backend absent  → decrement locally as graceful fallback             │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │  WHEEL ANGLE FORMULA (backend determines segment, not the app)            │
 * │   sectionDeg    = 360 / totalSegments                                     │
 * │   centerOfSlice = segmentIndex × sectionDeg + sectionDeg / 2             │
 * │   finalAngle    = (360 − centerOfSlice) % 360   [pointer is at top]      │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
public class activity_daily_spin extends AppCompatActivity {

    private static final String TAG = "DailySpin";

    // ── Wheel constants ────────────────────────────────────────────────────
    // DEFAULT_SEGMENT_COUNT is the fallback when backend does NOT send total_segments.
    // It MUST match SpinWheelView.DEFAULT_VALUES.length and backend wheel config.
    private static final int  DEFAULT_SEGMENT_COUNT = 8;
    private static final int  SPIN_FULL_ROTATIONS   = 6;      // extra loops before stopping
    private static final long IDLE_SPIN_MS          = 550;    // ms per idle rotation
    private static final long STOP_SPIN_MS          = 3400;   // deceleration duration

    // ── UI ─────────────────────────────────────────────────────────────────
    private SpinWheelView  wheelView;
    private MaterialButton btnSpin;
    private TextView       txtCoins, txtSpinsLeft;
    private LinearLayout   legendRow1, legendRow2;
    private AdView         adView;

    // ── State ──────────────────────────────────────────────────────────────
    private boolean        isSpinning     = false;
    private ObjectAnimator idleAnimator;
    private int            remainingSpins = -1;   // -1 = not yet synced from backend

    // ── Preferences ────────────────────────────────────────────────────────
    private UserPref userPref;

    // ── Ads ────────────────────────────────────────────────────────────────
    private RewardedAd rewardedAd;
    private int        spinAdCounter = 0;

    // ── Sound ──────────────────────────────────────────────────────────────
    private MediaPlayer winSound;

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_spin);

        wheelView   = findViewById(R.id.wheelView);
        btnSpin     = findViewById(R.id.btnSpin);
        txtCoins    = findViewById(R.id.txtCoins);
        txtSpinsLeft = findViewById(R.id.txtSpinsLeft);

        adView      = findViewById(R.id.adView);

        userPref = new UserPref(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        makeFullScreen();

        // ── STEP 1: Show LOCAL count immediately — no "Loading..." flicker ──
        remainingSpins = localRemainingSpins();
        updateUI();
        buildLegend();   // show default segment values immediately

        // ── STEP 2: Sync from backend (updates display when response arrives) ──
        loadSpinStatus();

        // Ads
        MobileAds.initialize(this, status -> {});
        if (adView != null) adView.loadAd(new AdRequest.Builder().build());
        loadAd();

        btnSpin.setOnClickListener(v -> {
            if (isSpinning) return;
            if (remainingSpins <= 0) {
                Toast.makeText(this, "No spins left today. Come back tomorrow!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startSpin();
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════════════

    private void makeFullScreen() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController c = window.getInsetsController();
            if (c != null)
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            //noinspection deprecation
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            window.setNavigationBarColor(Color.TRANSPARENT);
    }

    /** Refresh header coin count and spins-left chip. */
    private void updateUI() {
        if (txtCoins != null)
            txtCoins.setText(String.valueOf(userPref.getCoins()));

        if (txtSpinsLeft != null) {
            int display = (remainingSpins >= 0) ? remainingSpins : localRemainingSpins();
            txtSpinsLeft.setText("Spins Left: " + display);
            if (btnSpin != null)
                btnSpin.setEnabled(display > 0 && !isSpinning);
        }
    }

    /**
     * Build the reward legend chips at the bottom showing each segment's coin value.
     * Called once on start. Re-called after backend sends wheelSegments.
     */
    private void buildLegend() {
        if (legendRow1 == null || legendRow2 == null || wheelView == null) return;

        legendRow1.removeAllViews();
        legendRow2.removeAllViews();

        int[] vals  = wheelView.getSegmentValues();
        int   total = vals.length;

        for (int i = 0; i < total; i++) {
            TextView chip = new TextView(this);
            chip.setText("✦ " + vals[i] + "c");
            chip.setTextColor(0xFFFFD700);
            chip.setTextSize(12f);
            chip.setPadding(12, 6, 12, 6);
            chip.setBackgroundResource(R.drawable.bg_coin_chip_dark);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(4, 0, 4, 0);
            chip.setLayoutParams(lp);

            if (i < total / 2) {
                legendRow1.addView(chip);
            } else {
                legendRow2.addView(chip);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SPIN STATUS — Step 2: load from backend, update UI
    // ═══════════════════════════════════════════════════════════════════════

    private void loadSpinStatus() {
        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(
            new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {

                @Override
                public void onSuccess(String token) {
                    ApiService api = ApiClient.getClient().create(ApiService.class);
                    api.spinStatus(token).enqueue(new Callback<SpinResponse>() {

                        @Override
                        public void onResponse(Call<SpinResponse> call,
                                               Response<SpinResponse> resp) {
                            if (resp.isSuccessful() && resp.body() != null) {
                                SpinResponse sr = resp.body();
                                int effective = sr.getEffectiveRemainingSpins();

                                Log.d(TAG, "spinStatus → raw=" + sr.remainingSpins
                                        + " effective=" + effective);

                                if (effective >= 0) {
                                    // Backend gave a definitive count — trust it
                                    remainingSpins = effective;
                                }
                                // If effective == -1, keep local count already shown

                                // Update wheel layout if backend sent it
                                if (sr.wheelSegments != null && !sr.wheelSegments.isEmpty()) {
                                    wheelView.setSegmentValues(sr.wheelSegments);
                                    buildLegend();
                                    Log.d(TAG, "Wheel updated from spinStatus: "
                                            + sr.wheelSegments);
                                }

                            } else {
                                Log.w(TAG, "spinStatus HTTP " + resp.code()
                                        + " — keeping local count");
                            }
                            updateUI();
                        }

                        @Override
                        public void onFailure(Call<SpinResponse> call, Throwable t) {
                            Log.w(TAG, "spinStatus failed: " + t.getMessage()
                                    + " — keeping local count");
                            updateUI();
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.w(TAG, "Token error in spinStatus: " + e.getMessage());
                    updateUI();
                }
            });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SPIN — Phase 1: start idle animation + call backend
    // ═══════════════════════════════════════════════════════════════════════

    private void startSpin() {
        isSpinning = true;
        if (btnSpin != null) btnSpin.setEnabled(false);

        // ── BUG FIX: Start idle animation from CURRENT rotation angle, not 0.
        // Starting from 0 causes a visible jump if the wheel is not at 0° already.
        float fromAngle = ((wheelView.getRotation() % 360f) + 360f) % 360f;
        idleAnimator = ObjectAnimator.ofFloat(
                wheelView, "rotation",
                fromAngle, fromAngle + 360f);         // always one full forward rotation
        idleAnimator.setDuration(IDLE_SPIN_MS);
        idleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        idleAnimator.setRepeatMode(ValueAnimator.RESTART);
        idleAnimator.setInterpolator(new LinearInterpolator());
        idleAnimator.start();

        callSpinApi();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SPIN — Phase 2: backend call, receive authoritative result
    // ═══════════════════════════════════════════════════════════════════════

    private void callSpinApi() {
        com.app.rewardsplanet.network.AuthTokenHelper.getBearerToken(
            new com.app.rewardsplanet.network.AuthTokenHelper.TokenCallback() {

                @Override
                public void onSuccess(String token) {
                    ApiService api = ApiClient.getClient().create(ApiService.class);
                    api.spin(token).enqueue(new Callback<SpinResponse>() {

                        @Override
                        public void onResponse(Call<SpinResponse> call,
                                               Response<SpinResponse> resp) {

                            if (resp.isSuccessful() && resp.body() != null) {
                                SpinResponse sr = resp.body();

                                int reward        = sr.reward;
                                int segmentIndex  = sr.getEffectiveSegmentIndex();
                                int totalSegments = sr.getEffectiveTotalSegments();
                                int effective     = sr.getEffectiveRemainingSpins();

                                // ── Update spin count ──────────────────────
                                userPref.increaseSpinCount();   // local audit trail
                                if (effective >= 0) {
                                    remainingSpins = effective; // backend is authoritative
                                } else {
                                    remainingSpins = Math.max(0, remainingSpins - 1);
                                }

                                // ── Update wheel layout if backend sent it ─
                                if (sr.wheelSegments != null && !sr.wheelSegments.isEmpty()) {
                                    wheelView.setSegmentValues(sr.wheelSegments);
                                    buildLegend();
                                }

                                // ── Wheel verification ────────────────────
                                if (sr.wheelSegments != null && !sr.wheelSegments.isEmpty()
                                        && segmentIndex >= 0
                                        && segmentIndex < sr.wheelSegments.size()) {
                                    int expected = sr.wheelSegments.get(segmentIndex);
                                    if (expected != reward) {
                                        Log.e(TAG, "⚠ WHEEL MISMATCH: reward=" + reward
                                                + " but wheelSegments[" + segmentIndex + "]="
                                                + expected);
                                    } else {
                                        Log.d(TAG, "✓ Wheel verify OK: seg=" + segmentIndex
                                                + " = " + reward + " coins");
                                    }
                                }

                                Log.d(TAG, "spin → reward=" + reward
                                        + " segIdx=" + segmentIndex
                                        + " totalSeg=" + totalSegments
                                        + " remaining=" + remainingSpins
                                        + " rawRemaining=" + sr.remainingSpins);

                                // ── Phase 3: animate to correct segment ────
                                stopSpin(reward, segmentIndex, totalSegments);

                            } else if (resp.code() == 400) {
                                // Backend confirmed daily limit
                                cancelIdleSpin();
                                remainingSpins = 0;
                                isSpinning = false;
                                updateUI();
                                Toast.makeText(activity_daily_spin.this,
                                        "No spins left today!", Toast.LENGTH_SHORT).show();

                            } else {
                                // Server error — do NOT award coins
                                Log.e(TAG, "spin API error: " + resp.code());
                                cancelIdleSpin();
                                isSpinning = false;
                                if (btnSpin != null) btnSpin.setEnabled(remainingSpins > 0);
                                Toast.makeText(activity_daily_spin.this,
                                        "Server error, please try again",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<SpinResponse> call, Throwable t) {
                            Log.e(TAG, "spin API failed: " + t.getMessage());
                            cancelIdleSpin();
                            isSpinning = false;
                            if (btnSpin != null) btnSpin.setEnabled(remainingSpins > 0);
                            Toast.makeText(activity_daily_spin.this,
                                    "Network error, please try again",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Token error during spin: " + e.getMessage());
                    cancelIdleSpin();
                    isSpinning = false;
                    if (btnSpin != null) btnSpin.setEnabled(remainingSpins > 0);
                    Toast.makeText(activity_daily_spin.this,
                            "Auth error, please try again", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void cancelIdleSpin() {
        if (idleAnimator != null && idleAnimator.isRunning())
            idleAnimator.cancel();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SPIN — Phase 3: decelerate to backend-determined segment
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Stops idle rotation and precisely rotates the wheel to the segment the
     * backend determined as the winner.
     *
     * @param reward        Coins awarded (for dialog display only)
     * @param segmentIndex  0-based winning segment (clockwise from 12 o'clock)
     * @param totalSegments Total segments on this wheel configuration
     */
    private void stopSpin(int reward, int segmentIndex, int totalSegments) {
        cancelIdleSpin();

        int segments = (totalSegments > 0) ? totalSegments : wheelView.getSegmentCount();

        // ── BUG FIX 1: If backend didn't send segmentIndex (-1), derive it from reward value.
        // Without this, the wheel ALWAYS lands on segment 0 regardless of what was won.
        int resolvedIndex = segmentIndex;
        if (resolvedIndex < 0) {
            resolvedIndex = findSegmentForReward(reward);
            Log.d(TAG, "segmentIndex not from backend — derived idx=" + resolvedIndex
                    + " for reward=" + reward + "c");
        }

        float target = computeTargetAngle(resolvedIndex, segments);

        // ── BUG FIX 2: Use the RAW (un-modulo'd) rotation as animation start.
        // Using (rotation % 360) as the start causes a visible backward jump
        // because the ObjectAnimator would animate from the modulo value while
        // the View is actually at a much larger accumulated value.
        float rawCurrent  = wheelView.getRotation();
        float normCurrent = ((rawCurrent % 360f) + 360f) % 360f;   // normalize to [0, 360)

        // delta = shortest clockwise distance from current normalized angle to target
        float delta = ((target - normCurrent) + 360f) % 360f;
        // Guarantee at least a small forward spin (avoid delta=0 = no movement)
        if (delta < 2f) delta += 360f;

        // Animate from the RAW accumulated rotation — no visual jump
        float endRotation = rawCurrent + SPIN_FULL_ROTATIONS * 360f + delta;

        Log.d(TAG, String.format(
                "stopSpin: reward=%d  resolvedIdx=%d  target=%.1f°  rawCurrent=%.1f°  delta=%.1f°",
                reward, resolvedIndex, target, rawCurrent, delta));

        ObjectAnimator stopAnim = ObjectAnimator.ofFloat(
                wheelView, "rotation",
                rawCurrent, endRotation);
        stopAnim.setDuration(STOP_SPIN_MS);
        stopAnim.setInterpolator(new DecelerateInterpolator(2.5f));

        stopAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Normalize to clean [0,360) value to prevent float drift on future spins
                wheelView.setRotation(((target % 360f) + 360f) % 360f);

                // Credit coins (as confirmed by backend)
                userPref.addCoins(reward);

                // ── BUG FIX: Set isSpinning = false BEFORE updateUI() ──────
                // Previously, updateUI() was called while isSpinning was still true,
                // leaving btnSpin disabled forever after a spin.
                isSpinning = false;
                updateUI();

                com.app.rewardsplanet.repository.UserRepository
                        .getInstance(activity_daily_spin.this)
                        .refreshCurrentUser();

                playEffects();

                spinAdCounter++;
                if (spinAdCounter >= 2) {
                    spinAdCounter = 0;
                    showRewardDialog(reward, true);
                } else {
                    showRewardDialog(reward, false);
                }
            }
        });

        stopAnim.start();
    }

    /**
     * Finds the wheel segment index that corresponds to the given reward value.
     *
     * Used when the backend does not send `segment_index` — the app scans the
     * wheel's value array and picks the matching segment.
     *
     * If multiple segments share the same value (e.g. two "5-coin" segments),
     * one is chosen randomly — all are visually equivalent for that reward.
     *
     * If no exact match exists, the closest value is used and a warning is logged
     * (indicates the wheel image and backend config are out of sync).
     *
     * @param reward  Coin value returned by the backend spin API
     * @return        0-based segment index (clockwise from 12 o'clock)
     */
    private int findSegmentForReward(int reward) {
        int[] vals = wheelView.getSegmentValues();

        // Collect all segments whose value exactly matches the reward
        java.util.List<Integer> exactMatches = new java.util.ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == reward) exactMatches.add(i);
        }

        if (!exactMatches.isEmpty()) {
            // Pick randomly among exact matches (all are equivalent reward-wise)
            int picked = exactMatches.get((int) (Math.random() * exactMatches.size()));
            Log.d(TAG, "findSegmentForReward: reward=" + reward
                    + " → exactMatch idx=" + picked
                    + " (" + exactMatches.size() + " candidates)");
            return picked;
        }

        // No exact match — find segment with closest value
        // This means wheel image and backend are out of sync; log a clear warning
        int closestIdx  = 0;
        int closestDiff = Integer.MAX_VALUE;
        for (int i = 0; i < vals.length; i++) {
            int diff = Math.abs(vals[i] - reward);
            if (diff < closestDiff) {
                closestDiff = diff;
                closestIdx  = i;
            }
        }
        Log.e(TAG, "⚠ findSegmentForReward: NO exact match for reward=" + reward
                + ". Using closest segment " + closestIdx
                + " (value=" + vals[closestIdx] + "). "
                + "Update wheel_segments to match backend!");
        return closestIdx;
    }

    /**
     * Computes the final wheel rotation angle so that the pointer (fixed at top,
     * 12 o'clock) aligns exactly with the CENTER of the winning segment.
     *
     * SpinWheelView draws segment i starting at canvas angle: -90° + i × sectionDeg
     * (segment 0 = 12 o'clock, going clockwise).
     *
     * After View.setRotation(r), a point originally at local angle θ appears at
     * screen angle θ + r.  We want the segment center to appear at screen angle −90°:
     *
     *   (-90 + i×section + section/2) + r  =  -90
     *   r  =  -(i×section + section/2)
     *   r  =  360 - (i×section + section/2)   [mod 360, positive clockwise]
     */
    private float computeTargetAngle(int segmentIndex, int segments) {
        if (segments <= 0) segments = DEFAULT_SEGMENT_COUNT;
        if (segmentIndex < 0 || segmentIndex >= segments) {
            Log.w(TAG, "computeTargetAngle: clipping segmentIndex=" + segmentIndex
                    + " to 0 (segments=" + segments + ")");
            segmentIndex = 0;
        }

        float sectionDeg    = 360f / segments;
        float centerOfSlice = segmentIndex * sectionDeg + sectionDeg / 2f;
        float finalAngle    = ((360f - centerOfSlice) % 360f + 360f) % 360f;

        Log.d(TAG, String.format(
                "computeTargetAngle  seg=%d/%d  section=%.2f°  center=%.2f°  → target=%.2f°",
                segmentIndex, segments, sectionDeg, centerOfSlice, finalAngle));

        return finalAngle;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOCAL FALLBACK
    // ═══════════════════════════════════════════════════════════════════════

    /** Safe local remaining-spin count from SharedPrefs (offline fallback only). */
    private int localRemainingSpins() {
        return Math.max(0, 10 - userPref.getTodaySpinCount());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REWARD DIALOG
    // ═══════════════════════════════════════════════════════════════════════

    private void showRewardDialog(int reward, boolean withAd) {
        if (isFinishing() || isDestroyed()) return;

        try {
            Dialog d = new Dialog(this);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(R.layout.dialog_spin_result);

            TextView txtWin = d.findViewById(R.id.txtWinAmount);
            TextView txtBal = d.findViewById(R.id.txtCurrentBalance);
            MaterialButton ok = d.findViewById(R.id.btnOk);

            if (txtWin != null) txtWin.setText("+" + reward + " Coins");
            if (txtBal != null)
                txtBal.setText("Balance: " + userPref.getCoins() + " Coins");

            if (ok != null) {
                ok.setOnClickListener(v -> {
                    stopSound();
                    try { d.dismiss(); } catch (Exception ignored) {}
                    if (withAd) showAd();
                });
            }

            d.setOnDismissListener(dialog -> {
                stopSound();
                isSpinning = false;
                updateUI();
            });
            if (d.getWindow() != null)
                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            SuccessAnimationHelper.animate(d);
            d.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing spin reward dialog: " + e.getMessage());
            isSpinning = false;
            updateUI();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WIN EFFECTS
    // ═══════════════════════════════════════════════════════════════════════

    private void playEffects() {
        if (isFinishing() || isDestroyed()) return;

        FrameLayout root = findViewById(android.R.id.content);

        if (root != null) {
            // Coin shower (7 coins)
            for (int i = 0; i < 7; i++) {
                ImageView coin = new ImageView(this);
                coin.setImageResource(R.drawable.ic_coin);
                coin.setLayoutParams(new FrameLayout.LayoutParams(40, 40));
                root.addView(coin);

                float startX = root.getWidth() / 2f;
                float startY = root.getHeight() / 3f;
                coin.setX(startX);
                coin.setY(startY);

                float spreadX = (float) (Math.random() * 300 - 150);
                float spreadY = (float) -(Math.random() * 400 + 200);

                ObjectAnimator tx = ObjectAnimator.ofFloat(coin, "translationX", 0, spreadX);
                ObjectAnimator ty = ObjectAnimator.ofFloat(coin, "translationY", 0, spreadY);
                ObjectAnimator fa = ObjectAnimator.ofFloat(coin, "alpha", 1f, 0f);
                ObjectAnimator sc = ObjectAnimator.ofFloat(coin, "scaleX", 1f, 0.5f);
                ObjectAnimator sy = ObjectAnimator.ofFloat(coin, "scaleY", 1f, 0.5f);

                AnimatorSet set = new AnimatorSet();
                set.setDuration(1000);
                set.setStartDelay(i * 70L);
                set.setInterpolator(new DecelerateInterpolator());
                set.playTogether(tx, ty, fa, sc, sy);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator a) {
                        try { root.removeView(coin); } catch (Exception ignored) {}
                    }
                });
                set.start();
            }

            // Emoji burst
            TextView burst = new TextView(this);
            burst.setText("🎉✨🎆");
            burst.setTextSize(36f);
            burst.setX(root.getWidth() / 2f - 60);
            burst.setY(root.getHeight() / 4f);
            root.addView(burst);

            ObjectAnimator burstFade = ObjectAnimator.ofFloat(burst, "alpha", 1f, 0f);
            ObjectAnimator burstUp   = ObjectAnimator.ofFloat(burst, "translationY", 0, -200f);
            AnimatorSet burstSet = new AnimatorSet();
            burstSet.setDuration(1200);
            burstSet.playTogether(burstFade, burstUp);
            burstSet.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    try { root.removeView(burst); } catch (Exception ignored) {}
                }
            });
            burstSet.start();
        }

        // Sound
        try {
            stopSound();
            winSound = MediaPlayer.create(this, R.raw.win_sound);
            if (winSound != null) {
                winSound.setOnCompletionListener(mp -> stopSound());
                winSound.start();
            }
        } catch (Exception e) {
            Log.w(TAG, "Sound error: " + e.getMessage());
        }

        // Vibration
        try {
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) vib.vibrate(180);
        } catch (Exception e) {
            Log.w(TAG, "Vibrate error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SOUND
    // ═══════════════════════════════════════════════════════════════════════

    private void stopSound() {
        if (winSound != null) {
            try {
                if (winSound.isPlaying()) winSound.stop();
                winSound.release();
            } catch (Exception ignored) {
            } finally {
                winSound = null;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADS
    // ═══════════════════════════════════════════════════════════════════════

    private void loadAd() {
        RewardedAd.load(this, AdsManager.REWARDED_AD_ID,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override public void onAdLoaded(RewardedAd ad)           { rewardedAd = ad; }
                    @Override public void onAdFailedToLoad(LoadAdError err)   { rewardedAd = null; }
                });
    }

    private void showAd() {
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdDismissedFullScreenContent() {
                    rewardedAd = null; loadAd();
                }
            });
            rewardedAd.show(this, item -> {});
        } else {
            loadAd();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    @Override protected void onPause()   { super.onPause();   cancelIdleSpin(); stopSound(); }
    @Override protected void onStop()    { super.onStop();    stopSound(); }
    @Override protected void onDestroy() { super.onDestroy(); cancelIdleSpin(); stopSound(); }
}