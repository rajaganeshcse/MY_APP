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

        // Create FCM Notification Channel early
        com.app.rewardsplanet.notifications.MyFirebaseMessagingService.createNotificationChannel(this);

        // Enable Firestore network mode
        try {
            FirebaseFirestore.getInstance().enableNetwork();
        } catch (Exception ignored) {}

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            // No authenticated user → Navigate to OnBoarding after brief splash display
            timeoutHandler.postDelayed(this::navigateToLogin, 1000);
            return;
        }

        String uid = firebaseUser.getUid();

        // Safety timeout: If Firestore network takes longer than 5 seconds, fall back to cached user or continue
        timeoutRunnable = () -> {
            if (!isNavigated) {
                if (userPref.isUserValid()) {
                    navigateToMain();
                } else {
                    navigateToLogin();
                }
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 5000);

        // Check account status in Firestore BEFORE loading into UserRepository
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

                    // Normal user — load into UserRepository and go to Main
                    userRepository.loadCurrentUser(uid, new UserRepository.UserLoadCallback() {
                        @Override
                        public void onSuccess(UserModel user) {
                            if (!isNavigated) navigateToMain();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (!isNavigated) {
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
                        // Network issue — fall back to cached data
                        if (userPref.isUserValid()) {
                            navigateToMain();
                        } else {
                            navigateToLogin();
                        }
                    }
                });
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
