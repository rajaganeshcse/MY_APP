package com.app.rewardsplanet;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class activity_delete_account extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private ImageView btnBack;
    private EditText edtConfirmation;
    private MaterialButton btnDeleteAccount;

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String uid;

    // =========================================================
    // REQUIRED CONFIRMATION TEXT
    // =========================================================

    private static final String DELETE_CONFIRMATION =
            "Delete my account";

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_delete_account);

        // Initialize views
        initViews();

        // Full screen
        makeFullScreen();

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // =====================================================
        // CHECK CURRENT USER
        // =====================================================

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            goToLogin();

            return;
        }

        uid = currentUser.getUid();

        // =====================================================
        // SETUP LISTENERS
        // =====================================================

        setupListeners();

        // IMPORTANT:
        // No warning dialog here.
        //
        // The user must first type:
        // Delete my account
        //
        // Then click the Delete Account button.
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initViews() {

        btnBack = findViewById(R.id.btnBack);

        edtConfirmation = findViewById(
                R.id.edtConfirmation
        );

        btnDeleteAccount = findViewById(
                R.id.btnDeleteAccount
        );
    }

    // =========================================================
    // SETUP LISTENERS
    // =========================================================

    private void setupListeners() {

        // =====================================================
        // BACK BUTTON
        // =====================================================

        btnBack.setOnClickListener(v -> finish());

        // =====================================================
        // DELETE ACCOUNT BUTTON
        // =====================================================

        btnDeleteAccount.setOnClickListener(v -> {

            // Get confirmation text
            String confirmation = edtConfirmation
                    .getText()
                    .toString()
                    .trim();

            // =================================================
            // CHECK EXACT TEXT
            // =================================================

            if (!confirmation.equals(DELETE_CONFIRMATION)) {

                edtConfirmation.setError(
                        "Type exactly: Delete my account"
                );

                edtConfirmation.requestFocus();

                return;
            }

            // =================================================
            // SHOW FINAL CONFIRMATION
            // =================================================

            showFinalDeleteWarning();
        });
    }

    // =========================================================
    // FINAL CONFIRMATION DIALOG
    // =========================================================

    private void showFinalDeleteWarning() {

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Account Permanently?")
                .setMessage(
                        "This action cannot be undone.\n\n"
                                + "Your account data will be deleted "
                                + "and you will be signed out.\n\n"
                                + "Are you sure you want to permanently "
                                + "delete your account?"
                )
                .setNegativeButton(
                        "Cancel",
                        (dialogInterface, which) -> {

                            dialogInterface.dismiss();
                        }
                )
                .setPositiveButton(
                        "Delete",
                        (dialogInterface, which) -> {

                            deleteAccount();
                        }
                )
                .create();

        // =====================================================
        // SHOW DIALOG
        // =====================================================

        dialog.setOnShowListener(dialogInterface -> {

            // Delete button
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setTextColor(Color.RED);

            // Cancel button
            dialog.getButton(
                    AlertDialog.BUTTON_NEGATIVE
            ).setTextColor(Color.BLACK);
        });

        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }

    // =========================================================
    // DELETE ACCOUNT
    // =========================================================

    private void deleteAccount() {

        // =====================================================
        // CHECK UID
        // =====================================================

        if (uid == null || uid.isEmpty()) {

            Toast.makeText(
                    this,
                    "User ID not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =====================================================
        // GET CURRENT USER
        // =====================================================

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            goToLogin();

            return;
        }

        // =====================================================
        // DISABLE DELETE BUTTON
        // =====================================================

        btnDeleteAccount.setEnabled(false);

        Toast.makeText(
                this,
                "Deleting account...",
                Toast.LENGTH_SHORT
        ).show();

        // =====================================================
        // DELETE FIRESTORE USER DOCUMENT
        // =====================================================

        db.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> {

                    // =================================================
                    // DELETE FIREBASE AUTH ACCOUNT
                    // =================================================

                    currentUser.delete()
                            .addOnSuccessListener(authResult -> {

                                Toast.makeText(
                                        activity_delete_account.this,
                                        "Account deleted successfully",
                                        Toast.LENGTH_SHORT
                                ).show();

                                // Go to login
                                goToLogin();
                            })
                            .addOnFailureListener(e -> {

                                btnDeleteAccount.setEnabled(true);

                                Toast.makeText(
                                        activity_delete_account.this,
                                        "Account deletion failed: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    btnDeleteAccount.setEnabled(true);

                    Toast.makeText(
                            activity_delete_account.this,
                            "Failed to delete user data: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
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

        // =====================================================
        // CLEAR ALL PREVIOUS ACTIVITIES
        // =====================================================

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

        // =====================================================
        // ANDROID 11+
        // =====================================================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller =
                    window.getInsetsController();

            if (controller != null) {

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            // =================================================
            // OLD ANDROID
            // =================================================

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // =====================================================
        // STATUS BAR
        // =====================================================

        window.setStatusBarColor(Color.TRANSPARENT);

        // =====================================================
        // NAVIGATION BAR
        // =====================================================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    // =========================================================
    // SYSTEM BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        finish();

        super.onBackPressed();
    }
}
