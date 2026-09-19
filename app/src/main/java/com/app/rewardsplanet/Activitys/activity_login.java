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
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        makeFullScreen();

        auth = FirebaseAuth.getInstance();
        userPref = new UserPref(this);

        btnGoogle = findViewById(R.id.btnGoogle);

        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> signIn());
    }

    private void signIn() {
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
                }

            } catch (Exception e) {
                Log.e("LOGIN_DEBUG", "Google Sign-In failed: " + e.getMessage());
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
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
                        return;
                    }

                    user.getIdToken(true).addOnSuccessListener(result -> {

                        String token = result.getToken();
                        Log.d("LOGIN_DEBUG", "Firebase Token: " + token);

                        sendToBackend(token);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("LOGIN_DEBUG", "Firebase Auth FAILED: " + e.getMessage());
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

                    if ("ACCOUNT_PENDING".equals(errorBody)) {
                        showAccountPendingDialog();
                    } else if ("ACCOUNT_DELETED".equals(errorBody)) {
                        showAccountDeletedDialog();
                    } else {
                        Toast.makeText(activity_login.this, "Access denied", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Log.e("API_DEBUG", "Auth FAILED: " + response.message());
                    Toast.makeText(activity_login.this, "Auth Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API_DEBUG", "Auth ERROR: " + t.getMessage());
                Toast.makeText(activity_login.this, "Server Error", Toast.LENGTH_SHORT).show();
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
                    com.app.rewardsplanet.repository.UserRepository.getInstance(activity_login.this)
                            .loadCurrentUser(user.getUid(), new com.app.rewardsplanet.repository.UserRepository.UserLoadCallback() {
                                @Override
                                public void onSuccess(UserModel userModel) {
                                    openMain();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    openMain();
                                }
                            });

                } else {
                    Log.e("API_DEBUG", "User fetch failed");
                    Toast.makeText(activity_login.this, "User setup failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserModel> call, Throwable t) {
                Log.e("API_DEBUG", "User API ERROR: " + t.getMessage());
                Toast.makeText(activity_login.this, "Server Error", Toast.LENGTH_SHORT).show();
            }
        });
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
        Log.d("LOGIN_DEBUG", "Opening MainActivity");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}