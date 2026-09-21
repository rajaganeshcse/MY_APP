package com.app.rewardsplanet.Activitys;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.models.UserModel;
import com.app.rewardsplanet.repository.UserRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private UserPref userPref;
    private UserRepository userRepository;
    private boolean isNavigated = false;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        userPref = new UserPref(this);
        userRepository = UserRepository.getInstance(this);
        makeFullScreen();

        startSplashAnimations();

        // Create FCM Notification Channel early
        com.app.rewardsplanet.notifications.MyFirebaseMessagingService.createNotificationChannel(this);

        // Enable Firestore network mode
        try {
            FirebaseFirestore.getInstance().enableNetwork();
        } catch (Exception ignored) {}

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            // No authenticated user → Navigate to OnBoarding after brief splash display
            timeoutHandler.postDelayed(this::navigateToLogin, 1200);
            return;
        }

        String uid = firebaseUser.getUid();

        // Safety timeout: If Firestore takes longer than 6 seconds, fall back to cached local data
        timeoutRunnable = () -> {
            if (!isNavigated) {
                android.util.Log.w("SplashActivity", "Firestore timeout — falling back to local cache");
                if (userPref.isUserValid()) {
                    navigateToMain();
                } else {
                    navigateToLogin();
                }
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 6000);

        // Fetch Firestore user document to:
        //  1. Check account status (Pending / Deleted)
        //  2. Pre-populate UserPref so MainActivity has local data IMMEDIATELY on first render
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    cancelTimeout();
                    if (isNavigated) return;

                    if (!doc.exists()) {
                        // User doc was deleted — force logout & go to login
                        forceLogout();
                        navigateToLogin();
                        return;
                    }

                    String accountStatus = doc.getString("account");

                    if ("Pending".equals(accountStatus) || "Deleted".equals(accountStatus)) {
                        // Account is flagged — sign out and pass status to login screen
                        forceLogout();
                        Intent intent = new Intent(SplashActivity.this, OnBoardingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("account_status", accountStatus);
                        isNavigated = true;
                        startActivity(intent);
                        finish();
                        return;
                    }

                    // ─── PRE-POPULATE UserPref FROM FIRESTORE ────────────────────────
                    // This ensures MainActivity.loadUserFromPref() has real data immediately
                    // when the activity renders — no empty-drawer flash on first open.
                    try {
                        userPref.setUid(uid);
                        userPref.setLogin(true);
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) userPref.setName(name);
                        String email = doc.getString("email");
                        if (email != null && !email.isEmpty()) userPref.setEmail(email);
                        String phone = doc.getString("phone");
                        if (phone != null && !phone.isEmpty()) userPref.setPhone(phone);
                        String pic = doc.getString("profile_pic");
                        if (pic == null) pic = doc.getString("profilePic");
                        if (pic == null) pic = doc.getString("photoUrl");
                        if (pic != null && !pic.isEmpty()) userPref.setProfileImage(pic);
                        String refCode = doc.getString("referralCode");
                        if (refCode != null && !refCode.isEmpty()) userPref.setReferralCode(refCode);
                        Object coinsObj = doc.get("coins");
                        if (coinsObj instanceof Long) userPref.setCoins((Long) coinsObj);
                        else if (coinsObj instanceof Double) userPref.setCoins(((Double) coinsObj).longValue());
                        Object ticketsObj = doc.get("tickets");
                        if (ticketsObj instanceof Long) userPref.setTickets(((Long) ticketsObj).intValue());
                        else if (ticketsObj instanceof Double) userPref.setTickets(((Double) ticketsObj).intValue());
                        String gender = doc.getString("gender");
                        if (gender != null && !gender.isEmpty()) userPref.setGender(gender);
                        String dob = doc.getString("dob");
                        if (dob != null && !dob.isEmpty()) userPref.setDob(dob);
                    } catch (Exception ex) {
                        android.util.Log.w("SplashActivity", "UserPref pre-populate partial failure: " + ex.getMessage());
                    }
                    // ─────────────────────────────────────────────────────────────────

                    // Start the Firestore realtime listener in UserRepository
                    userRepository.loadCurrentUser(uid, new UserRepository.UserLoadCallback() {
                        @Override
                        public void onSuccess(UserModel user) {
                            if (!isNavigated) navigateToMain();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (!isNavigated) {
                                // UserPref is already populated above — safe to proceed
                                if (userPref.isUserValid()) {
                                    navigateToMain();
                                } else {
                                    Toast.makeText(SplashActivity.this, "Unable to load profile. Please sign in.", Toast.LENGTH_SHORT).show();
                                    navigateToLogin();
                                }
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    cancelTimeout();
                    if (!isNavigated) {
                        // Network issue — fall back to cached local data
                        android.util.Log.w("SplashActivity", "Firestore fetch failed: " + e.getMessage());
                        if (userPref.isUserValid()) {
                            navigateToMain();
                        } else {
                            navigateToLogin();
                        }
                    }
                });
    }

    private void startSplashAnimations() {
        View splashLogo = findViewById(R.id.splashLogo);
        View appTitle = findViewById(R.id.appTitle);
        View appSubtitle = findViewById(R.id.appSubtitle);
        View tagContainer = findViewById(R.id.tagContainer);
        View progressBar = findViewById(R.id.progressBar);
        View txtLoadingStatus = findViewById(R.id.txtLoadingStatus);
        View glowCircleTop = findViewById(R.id.glowCircleTop);
        View glowCircleBottom = findViewById(R.id.glowCircleBottom);
        View floatingCoin1 = findViewById(R.id.floatingCoin1);
        View floatingCoin2 = findViewById(R.id.floatingCoin2);

        // 1. Ambient Glow Pulse Animations
        if (glowCircleTop != null) {
            glowCircleTop.setAlpha(0.08f);
            glowCircleTop.animate()
                    .alpha(0.22f)
                    .setDuration(2800)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            glowCircleTop.animate().alpha(0.08f).setDuration(2800).start();
                        }
                    })
                    .start();
        }

        if (glowCircleBottom != null) {
            glowCircleBottom.setAlpha(0.05f);
            glowCircleBottom.animate()
                    .alpha(0.18f)
                    .setDuration(3200)
                    .setStartDelay(350)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();
        }

        // 2. Floating Background Coins Animations
        if (floatingCoin1 != null) {
            floatingCoin1.setTranslationY(-20f);
            floatingCoin1.setAlpha(0f);
            floatingCoin1.animate()
                    .translationY(0f)
                    .alpha(0.85f)
                    .setDuration(800)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .withEndAction(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            floatingCoin1.animate()
                                    .translationY(-14f)
                                    .rotation(12f)
                                    .setDuration(2200)
                                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing() && !isDestroyed()) {
                                                floatingCoin1.animate()
                                                        .translationY(0f)
                                                        .rotation(0f)
                                                        .setDuration(2200)
                                                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                                        .withEndAction(this)
                                                        .start();
                                            }
                                        }
                                    })
                                    .start();
                        }
                    })
                    .start();
        }

        if (floatingCoin2 != null) {
            floatingCoin2.setTranslationY(20f);
            floatingCoin2.setAlpha(0f);
            floatingCoin2.animate()
                    .translationY(0f)
                    .alpha(0.80f)
                    .setDuration(800)
                    .setStartDelay(200)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .withEndAction(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            floatingCoin2.animate()
                                    .translationY(14f)
                                    .rotation(-15f)
                                    .setDuration(2600)
                                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing() && !isDestroyed()) {
                                                floatingCoin2.animate()
                                                        .translationY(0f)
                                                        .rotation(0f)
                                                        .setDuration(2600)
                                                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                                        .withEndAction(this)
                                                        .start();
                                            }
                                        }
                                    })
                                    .start();
                        }
                    })
                    .start();
        }

        // 3. Logo Entrance & Breathing Motion
        if (splashLogo != null) {
            splashLogo.setScaleX(0.4f);
            splashLogo.setScaleY(0.4f);
            splashLogo.setAlpha(0f);
            splashLogo.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(750)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .withEndAction(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            splashLogo.animate()
                                    .translationY(-10f)
                                    .setDuration(1800)
                                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing() && !isDestroyed()) {
                                                splashLogo.animate()
                                                        .translationY(0f)
                                                        .setDuration(1800)
                                                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                                        .withEndAction(this)
                                                        .start();
                                            }
                                        }
                                    })
                                    .start();
                        }
                    })
                    .start();
        }

        // 4. Staggered Text Slide-Up
        if (appTitle != null) {
            appTitle.setTranslationY(40f);
            appTitle.setAlpha(0f);
            appTitle.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(600)
                    .setStartDelay(120)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        if (appSubtitle != null) {
            appSubtitle.setTranslationY(30f);
            appSubtitle.setAlpha(0f);
            appSubtitle.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(600)
                    .setStartDelay(220)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        if (tagContainer != null) {
            tagContainer.setScaleX(0.7f);
            tagContainer.setScaleY(0.7f);
            tagContainer.setAlpha(0f);
            tagContainer.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(650)
                    .setStartDelay(300)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }

        // 5. Loading Progress Bar & Status Text Fade-In
        if (progressBar != null) {
            progressBar.setScaleX(0.5f);
            progressBar.setScaleY(0.5f);
            progressBar.setAlpha(0f);
            progressBar.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(500)
                    .setStartDelay(400)
                    .start();
        }

        if (txtLoadingStatus != null) {
            txtLoadingStatus.setAlpha(0f);
            txtLoadingStatus.animate()
                    .alpha(1.0f)
                    .setDuration(500)
                    .setStartDelay(450)
                    .start();
        }
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    private void navigateToMain() {
        isNavigated = true;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        isNavigated = true;
        Intent intent = new Intent(SplashActivity.this, OnBoardingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimeout();
        timeoutHandler.removeCallbacksAndMessages(null);
    }

    // Full sign-out: Firebase Auth + Google session + UserRepository + UserPref
    private void forceLogout() {
        // 1. Detach Firestore listener + clear LiveData
        UserRepository.getInstance(this).clearUser();

        // 2. Sign out Firebase
        FirebaseAuth.getInstance().signOut();

        // 3. Revoke Google Sign-In session (prevents silent re-auth)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleClient = GoogleSignIn.getClient(this, gso);
        googleClient.signOut();
        googleClient.revokeAccess();

        // 4. Clear local prefs
        new UserPref(this).logout();
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
}
