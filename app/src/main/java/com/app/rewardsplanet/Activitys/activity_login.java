package com.app.rewardsplanet.Activitys;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.models.UserModel;
import com.app.rewardsplanet.network.*;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.*;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.*;

public class activity_login extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth auth;
    private UserPref userPref;

    private LinearLayout btnGoogle;
    private ImageView imgGoogle;
    private TextView txtGoogle;
    private ProgressBar loginProgressBar;
    private TextView txtPrivacy;
    private boolean isLoading = false;
    // Guard against double-navigation (snapshot listener fires multiple times)
    private volatile boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        makeFullScreen();

        auth = FirebaseAuth.getInstance();
        userPref = new UserPref(this);

        btnGoogle = findViewById(R.id.btnGoogle);
        imgGoogle = findViewById(R.id.imgGoogle);
        txtGoogle = findViewById(R.id.txtGoogle);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        txtPrivacy = findViewById(R.id.txtPrivacy);

        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> signIn());

        if (txtPrivacy != null) {
            txtPrivacy.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://rewardsplanet.app/privacy-policy"));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(activity_login.this, "Privacy Policy: https://rewardsplanet.app/privacy-policy", Toast.LENGTH_LONG).show();
                }
            });
        }

        // Start premium entrance and interactive spring animations
        startLoginAnimations();

        // Check if passed account status from intent
        String accountStatus = getIntent().getStringExtra("account_status");
        if ("Pending".equals(accountStatus)) {
            showAccountPendingDialog();
        } else if ("Deleted".equals(accountStatus)) {
            showAccountDeletedDialog();
        }
    }

    private void startLoginAnimations() {
        View logo = findViewById(R.id.logo);
        View heroTitle = findViewById(R.id.heroTitle);
        View heroSubtitle = findViewById(R.id.heroSubtitle);
        View tagContainer = findViewById(R.id.tagContainer);
        View bottomSheet = findViewById(R.id.bottom_sheet);
        View glowCircleTop = findViewById(R.id.glowCircleTop);
        View glowCircleBottom = findViewById(R.id.glowCircleBottom);
        View floatingCoin1 = findViewById(R.id.floatingCoin1);
        View floatingCoin2 = findViewById(R.id.floatingCoin2);

        // 0. Floating Background Coins Animations (2 Coins)
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

        // 1. Ambient Glow Pulse Animations
        if (glowCircleTop != null) {
            glowCircleTop.setAlpha(0.08f);
            glowCircleTop.animate()
                    .alpha(0.20f)
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
                    .setStartDelay(400)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();
        }

        // 2. App Logo Entrance + Continuous Breathing Floating Effect
        if (logo != null) {
            logo.setScaleX(0.4f);
            logo.setScaleY(0.4f);
            logo.setAlpha(0f);
            logo.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(750)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .withEndAction(() -> {
                        // Start gentle floating breathing animation
                        if (!isFinishing() && !isDestroyed()) {
                            logo.animate()
                                    .translationY(-10f)
                                    .setDuration(1800)
                                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing() && !isDestroyed()) {
                                                logo.animate()
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

        // 3. Hero Text & Subtitle Slide Up
        if (heroTitle != null) {
            heroTitle.setTranslationY(40f);
            heroTitle.setAlpha(0f);
            heroTitle.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(600)
                    .setStartDelay(120)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (heroSubtitle != null) {
            heroSubtitle.setTranslationY(30f);
            heroSubtitle.setAlpha(0f);
            heroSubtitle.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 4. Feature Badges Pop In
        if (tagContainer != null) {
            tagContainer.setScaleX(0.7f);
            tagContainer.setScaleY(0.7f);
            tagContainer.setAlpha(0f);
            tagContainer.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(650)
                    .setStartDelay(280)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }

        // 5. Bottom Sheet Card Slide Up
        if (bottomSheet != null) {
            bottomSheet.setTranslationY(300f);
            bottomSheet.setAlpha(0f);
            bottomSheet.animate()
                    .translationY(0f)
                    .alpha(1.0f)
                    .setDuration(700)
                    .setStartDelay(150)
                    .setInterpolator(new DecelerateInterpolator(2.0f))
                    .start();
        }

        // 6. Google Button Interactive Spring Touch Feedback
        if (btnGoogle != null) {
            btnGoogle.setOnTouchListener((v, event) -> {
                if (isLoading) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160)
                                .setInterpolator(new OvershootInterpolator(2.2f)).start();
                        break;
                }
                return false;
            });
        }
    }

    private void showLoading(boolean show) {
        isLoading = show;
        if (btnGoogle != null) {
            btnGoogle.setEnabled(!show);
            btnGoogle.setClickable(!show);
        }

        if (show) {
            // Fade out Google Icon & Text, Fade in Progress Bar
            if (imgGoogle != null) {
                imgGoogle.animate().alpha(0f).scaleX(0.6f).scaleY(0.6f).setDuration(180)
                        .withEndAction(() -> imgGoogle.setVisibility(View.GONE)).start();
            }
            if (txtGoogle != null) {
                txtGoogle.animate().alpha(0f).setDuration(180)
                        .withEndAction(() -> txtGoogle.setVisibility(View.GONE)).start();
            }
            if (loginProgressBar != null) {
                loginProgressBar.setVisibility(View.VISIBLE);
                loginProgressBar.setAlpha(0f);
                loginProgressBar.setScaleX(0.5f);
                loginProgressBar.setScaleY(0.5f);
                loginProgressBar.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).setStartDelay(100).start();
            }
        } else {
            // Fade out Progress Bar, Fade in Google Icon & Text
            if (loginProgressBar != null) {
                loginProgressBar.animate().alpha(0f).setDuration(150)
                        .withEndAction(() -> loginProgressBar.setVisibility(View.GONE)).start();
            }
            if (imgGoogle != null) {
                imgGoogle.setVisibility(View.VISIBLE);
                imgGoogle.setAlpha(0f);
                imgGoogle.setScaleX(0.6f);
                imgGoogle.setScaleY(0.6f);
                imgGoogle.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).setStartDelay(100).start();
            }
            if (txtGoogle != null) {
                txtGoogle.setVisibility(View.VISIBLE);
                txtGoogle.setAlpha(0f);
                txtGoogle.animate().alpha(1f).setDuration(200).setStartDelay(100).start();
            }
        }
    }

    private void signIn() {
        if (isLoading) return;
        showLoading(true);
        startActivityForResult(googleSignInClient.getSignInIntent(), 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            try {
                GoogleSignInAccount account =
                        GoogleSignIn.getSignedInAccountFromIntent(data)
                                .getResult(ApiException.class);

                if (account != null) {
                    firebaseAuth(account);
                } else {
                    Log.e("LOGIN_DEBUG", "Google account is NULL");
                    showLoading(false);
                    Toast.makeText(this, "Login Failed: Account data is null", Toast.LENGTH_LONG).show();
                }

            } catch (ApiException e) {
                Log.e("LOGIN_DEBUG", "Google Sign-In ApiException: code=" + e.getStatusCode() + ", msg=" + e.getMessage(), e);
                showLoading(false);
                String msg;
                if (e.getStatusCode() == 7) {
                    msg = "Google Error 7 (Network/Auth): 1) Add SHA-1 to com.dailykash.app in Firebase Console. 2) Set phone Date & Time to Automatic. 3) Clear Google Play Services cache.";
                } else if (e.getStatusCode() == 10) {
                    msg = "Google Sign-In Error 10: SHA-1 fingerprint missing in Firebase Console for com.dailykash.app";
                } else if (e.getStatusCode() == 12500) {
                    msg = "Google Sign-In Error 12500: Check Firebase Support Email and Google Play Services";
                } else if (e.getStatusCode() == 12501) {
                    msg = "Sign-in cancelled by user";
                } else {
                    msg = "Google Sign-In Failed (Code " + e.getStatusCode() + "): " + e.getMessage();
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e("LOGIN_DEBUG", "Google Sign-In failed: " + e.getMessage(), e);
                showLoading(false);
                Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuth(GoogleSignInAccount account) {

        Log.d("LOGIN_DEBUG", "Firebase Auth started");

        AuthCredential credential =
                GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {

                    Log.d("LOGIN_DEBUG", "Firebase Auth SUCCESS");

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Log.e("LOGIN_DEBUG", "Firebase user NULL");
                        showLoading(false);
                        Toast.makeText(this, "Authentication failed: User is null", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.getIdToken(true).addOnSuccessListener(result -> {

                        String token = result.getToken();
                        Log.d("LOGIN_DEBUG", "Firebase Token: " + token);

                        sendToBackend(token);
                    }).addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Failed to retrieve auth token: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("LOGIN_DEBUG", "Firebase Auth FAILED: " + e.getMessage(), e);
                    showLoading(false);
                    Toast.makeText(this, "Firebase Auth Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void sendToBackend(String token) {

        Log.d("API_DEBUG", "Sending token to backend: " + token);

        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.auth(new LoginRequest(token)).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                Log.d("API_DEBUG", "Auth Response Code: " + response.code());

                if (response.isSuccessful()) {
                    Log.d("API_DEBUG", "Auth SUCCESS");
                    fetchUser(token);

                } else if (response.code() == 403) {
                    showLoading(false);

                    // Read the body to determine which 403 case
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException ignored) {}

                    Log.d("API_DEBUG", "Auth 403 body: " + errorBody);

                    // Full sign-out: revokes Google session so silent re-auth can't bypass the check
                    forceLogout();

                    if (errorBody != null && errorBody.contains("ACCOUNT_PENDING")) {
                        showAccountPendingDialog();
                    } else if (errorBody != null && errorBody.contains("ACCOUNT_DELETED")) {
                        showAccountDeletedDialog();
                    } else {
                        Toast.makeText(activity_login.this, "Access denied", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Log.e("API_DEBUG", "Auth FAILED: " + response.code() + " " + response.message());
                    showLoading(false);
                    Toast.makeText(activity_login.this, "Backend Auth Failed (" + response.code() + "): " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API_DEBUG", "Auth ERROR: " + t.getMessage(), t);
                showLoading(false);
                Toast.makeText(activity_login.this, "Backend Server Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // =========================================================
    // FULL LOGOUT (Firebase + Google session + UserPref)
    // =========================================================

    private void forceLogout() {
        // 1. Clear UserRepository listener + LiveData
        com.app.rewardsplanet.repository.UserRepository.getInstance(this).clearUser();

        // 2. Sign out Firebase Auth
        auth.signOut();

        // 3. Revoke Google Sign-In (prevents silent re-auth on next launch)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleClient = GoogleSignIn.getClient(this, gso);
        googleClient.signOut();
        googleClient.revokeAccess();

        // 4. Clear local prefs
        new UserPref(this).logout();
    }

    // =========================================================
    // DIALOG: ACCOUNT PENDING (Under 7-Day Review)
    // =========================================================

    private void showAccountPendingDialog() {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_account_pending, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvEmail = view.findViewById(R.id.tvAccountPendingEmail);
        if (tvEmail != null) {
            tvEmail.setOnClickListener(v -> openSupportEmail());
        }

        MaterialButton btnOk = view.findViewById(R.id.btnAccountPendingOk);
        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // =========================================================
    // DIALOG: ACCOUNT DELETED (Permanent)
    // =========================================================

    private void showAccountDeletedDialog() {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_account_deleted, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        MaterialButton btnSupport = view.findViewById(R.id.btnContactSupport);
        TextView btnDismiss = view.findViewById(R.id.btnAccountDeletedDismiss);

        // Open email/support link
        btnSupport.setOnClickListener(v -> {
            openSupportEmail();
            dialog.dismiss();
        });

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openSupportEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:loco209832@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Account Support Request");
            startActivity(emailIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Contact: loco209832@gmail.com", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchUser(String token) {

        Log.d("API_DEBUG", "Fetching user...");

        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.getUser(token).enqueue(new Callback<UserModel>() {
            @Override
            public void onResponse(Call<UserModel> call, Response<UserModel> response) {

                Log.d("API_DEBUG", "User Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {

                    UserModel user = response.body();

                    userPref.setUid(user.getUid());
                    userPref.setName(user.getName());
                    userPref.setEmail(user.getEmail());
                    userPref.setProfileImage(user.getProfile_pic());
                    userPref.setReferralCode(user.getReferralCode());
                    userPref.setCoins(user.getCoins());
                    userPref.setTickets((int) user.getTickets());
                    userPref.setWalletToken((int) user.getTickets());
                    userPref.setLogin(true);

                    // Initialize Centralized UserRepository snapshot listener
                    String uid = (user.getUid() == null || user.getUid().isEmpty()) && auth.getCurrentUser() != null
                            ? auth.getCurrentUser().getUid() : user.getUid();

                    com.app.rewardsplanet.repository.UserRepository.getInstance(activity_login.this)
                            .loadCurrentUser(uid, new com.app.rewardsplanet.repository.UserRepository.UserLoadCallback() {
                                @Override
                                public void onSuccess(UserModel userModel) {
                                    // Ensure local prefs are fully synced from Firestore user model
                                    if (userModel != null) {
                                        userPref.setUid(userModel.getUid());
                                        userPref.setName(userModel.getName());
                                        userPref.setEmail(userModel.getEmail());
                                        userPref.setPhone(userModel.getPhone());
                                        userPref.setProfileImage(userModel.getProfile_pic());
                                        userPref.setReferralCode(userModel.getReferralCode());
                                        userPref.setCoins(userModel.getCoins());
                                        userPref.setTickets((int) userModel.getTickets());
                                        userPref.setWalletToken((int) userModel.getTickets());
                                        userPref.setLogin(true);
                                    }
                                    showLoading(false);
                                    openMain();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Log.w("LOGIN_DEBUG", "UserRepository load error (non-fatal): " + errorMessage);
                                    // We already set UserPref above — safe to proceed
                                    showLoading(false);
                                    openMain();
                                }
                            });

                } else {
                    Log.e("API_DEBUG", "User fetch API returned non-200. Falling back to direct Firestore loading.");
                    fallbackDirectUserLoad();
                }
            }

            @Override
            public void onFailure(Call<UserModel> call, Throwable t) {
                Log.e("API_DEBUG", "User API ERROR: " + t.getMessage() + ". Falling back to direct Firestore loading.");
                fallbackDirectUserLoad();
            }
        });
    }

    private void fallbackDirectUserLoad() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            userPref.setUid(uid);
            userPref.setEmail(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            userPref.setName(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
            userPref.setProfileImage(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");
            userPref.setLogin(true);

            com.app.rewardsplanet.repository.UserRepository.getInstance(activity_login.this)
                    .loadCurrentUser(uid, new com.app.rewardsplanet.repository.UserRepository.UserLoadCallback() {
                        @Override
                        public void onSuccess(UserModel userModel) {
                            if (userModel != null) {
                                userPref.setUid(userModel.getUid());
                                userPref.setName(userModel.getName());
                                userPref.setEmail(userModel.getEmail());
                                userPref.setPhone(userModel.getPhone());
                                userPref.setProfileImage(userModel.getProfile_pic());
                                userPref.setReferralCode(userModel.getReferralCode());
                                userPref.setCoins(userModel.getCoins());
                                userPref.setTickets((int) userModel.getTickets());
                                userPref.setWalletToken((int) userModel.getTickets());
                                userPref.setLogin(true);
                            }
                            showLoading(false);
                            openMain();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.w("LOGIN_DEBUG", "Fallback user load error: " + errorMessage);
                            showLoading(false);
                            openMain();
                        }
                    });
        } else {
            showLoading(false);
            Toast.makeText(activity_login.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
        }
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

    private void openMain() {
        if (isNavigating) {
            Log.d("LOGIN_DEBUG", "openMain() already called — ignoring duplicate");
            return;
        }
        isNavigating = true;

        // Record user registration attribution if arriving via a tracking campaign
        com.app.rewardsplanet.share_earn.ui.InstallAttributionHelper.recordRegistrationIfNeeded(this);

        Log.d("LOGIN_DEBUG", "Opening MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        // Clear the entire back stack so pressing Back exits the app, not returning to login
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}