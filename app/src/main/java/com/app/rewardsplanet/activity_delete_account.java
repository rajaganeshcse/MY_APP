package com.app.rewardsplanet;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.app.rewardsplanet.repository.UserRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class activity_delete_account extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private ImageView btnBack;
    private EditText edtConfirmation;
    private MaterialButton btnDeleteAccount;

    // =========================================================
    // FIREBASE & API & PREFS
    // =========================================================

    private FirebaseAuth auth;
    private ApiService apiService;
    private UserPref userPref;

    private String uid;

    // =========================================================
    // REQUIRED CONFIRMATION TEXT
    // =========================================================

    private static final String DELETE_CONFIRMATION = "Delete my account";

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_delete_account);

        initViews();
        makeFullScreen();

        auth = FirebaseAuth.getInstance();
        apiService = ApiClient.getClient().create(ApiService.class);
        userPref = new UserPref(this);

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        uid = currentUser.getUid();

        setupListeners();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtConfirmation = findViewById(R.id.edtConfirmation);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    // =========================================================
    // SETUP LISTENERS
    // =========================================================

    private void setupListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnDeleteAccount.setOnClickListener(v -> {

            String confirmation = edtConfirmation.getText().toString().trim();

            if (!confirmation.equals(DELETE_CONFIRMATION)) {
                edtConfirmation.setError("Type exactly: Delete my account");
                edtConfirmation.requestFocus();
                return;
            }

            showFinalDeleteWarning();
        });
    }

    // =========================================================
    // DIALOG 1: CONFIRM DELETE (Cancel / Delete)
    // =========================================================

    private void showFinalDeleteWarning() {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView btnCancel = view.findViewById(R.id.btnDialogCancel);
        MaterialButton btnDelete = view.findViewById(R.id.btnDialogDelete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            submitDeleteRequest();
        });

        dialog.show();
    }

    // =========================================================
    // DIALOG 2: SUCCESS (Request Submitted — OK)
    // =========================================================

    private void showSuccessDialog() {

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_success, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvEmail = view.findViewById(R.id.tvDeleteSuccessEmail);
        if (tvEmail != null) {
            tvEmail.setOnClickListener(v -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:loco209832@gmail.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Account Deletion Request Support");
                    startActivity(emailIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Contact: loco209832@gmail.com", Toast.LENGTH_LONG).show();
                }
            });
        }

        View btnOk = view.findViewById(R.id.btnDialogOk);
        if (btnOk != null) {
            btnOk.setOnClickListener(v -> {
                dialog.dismiss();
                logoutUser();
            });
        }

        com.app.rewardsplanet.utils.SuccessAnimationHelper.animate(dialog);

        dialog.show();
    }

    // =========================================================
    // LOGOUT USER
    // =========================================================

    private void logoutUser() {
        try {
            if (UserRepository.getInstance(this) != null) {
                UserRepository.getInstance(this).clearUser();
            }
        } catch (Exception ignored) {}

        try {
            if (userPref != null) {
                userPref.logout();
            }
        } catch (Exception ignored) {}

        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {}

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity_delete_account.this, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> navigateToLogin());
        } catch (Exception e) {
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        try {
            Toast.makeText(activity_delete_account.this, "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(activity_delete_account.this, com.app.rewardsplanet.Activitys.activity_login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            finish();
        }
    }

    // =========================================================
    // SUBMIT DELETE REQUEST TO BACKEND
    // =========================================================

    private void submitDeleteRequest() {

        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        btnDeleteAccount.setEnabled(false);
        btnDeleteAccount.setText("Submitting...");

        // 1. Get fresh Firebase ID token
        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> {

                    String idToken = result.getToken();

                    if (idToken == null) {
                        Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                        btnDeleteAccount.setEnabled(true);
                        btnDeleteAccount.setText("Delete Account");
                        return;
                    }

                    // 2. Call backend API: POST /api/account/delete-request
                    Map<String, String> body = new HashMap<>();
                    body.put("token", idToken);

                    apiService.requestDeleteAccount(body)
                            .enqueue(new Callback<ResponseBody>() {

                                @Override
                                public void onResponse(Call<ResponseBody> call,
                                                       Response<ResponseBody> response) {

                                    if (response.isSuccessful()) {

                                        // Backend stored the delete request successfully.
                                        // Do NOT sign out here — MainActivity observers are still alive.
                                        // Full logout happens in showSuccessDialog OK button (after CLEAR_TASK kills back stack).
                                        showSuccessDialog();

                                    } else {

                                        btnDeleteAccount.setEnabled(true);
                                        btnDeleteAccount.setText("Delete Account");

                                        Toast.makeText(
                                                activity_delete_account.this,
                                                "Failed to submit request. Please try again.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {

                                    btnDeleteAccount.setEnabled(true);
                                    btnDeleteAccount.setText("Delete Account");

                                    Toast.makeText(
                                            activity_delete_account.this,
                                            "Network error: " + t.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    btnDeleteAccount.setEnabled(true);
                    btnDeleteAccount.setText("Delete Account");
                    Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // =========================================================
    // GO TO LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent = new Intent(
                activity_delete_account.this,
                com.app.rewardsplanet.Activitys.activity_login.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // =========================================================
    // FULL SCREEN
    // =========================================================

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
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        window.setStatusBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    // =========================================================
    // SYSTEM BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
