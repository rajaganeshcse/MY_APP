package com.example.rgamer.Activitys;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rgamer.R;
import com.example.rgamer.UserPref;
import com.example.rgamer.models.UserModel;
import com.example.rgamer.network.*;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;
import com.google.firebase.messaging.FirebaseMessaging;

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

    private void fetchUser(String token) {

        Log.d("API_DEBUG", "Fetching user...");

        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.getUser(token).enqueue(new Callback<UserModel>() {
            @Override
            public void onResponse(Call<UserModel> call, Response<UserModel> response) {

                Log.d("API_DEBUG", "User Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {

                    Log.d("API_DEBUG", "User Data Received: " + response.body().getUid());

                    UserModel user = response.body();

                    userPref.setUid(user.getUid());
                    userPref.setName(user.getName());
                    userPref.setEmail(user.getEmail());
                    userPref.setProfileImage(user.getProfileImage());
                    userPref.setProfileImage(user.getReferralCode());
                    userPref.setCoins(user.getCoins());
                    userPref.setTickets(user.getTickets());
                    userPref.setWalletToken(user.getWalletToken());

                    userPref.setLogin(true);

                    openMain();

                } else {
                    Log.e("API_DEBUG", "User fetch failed");
                }
            }

            @Override
            public void onFailure(Call<UserModel> call, Throwable t) {
                Log.e("API_DEBUG", "User API ERROR: " + t.getMessage());
                Toast.makeText(activity_login.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void makeFullScreen() {
        Window window = getWindow();

        // 🔥 Make content go behind system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );

                // Optional: hide bars (remove if you only want transparent top)
                // controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }

        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // 🔥 Make status bar transparent (TOP FIX)
        window.setStatusBarColor(Color.TRANSPARENT);

        // 🔥 Optional: make navigation bar transparent
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