package com.app.rewardsplanet.withdraws;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.Fragements.RedeemFragment;
import com.app.rewardsplanet.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class activity_withdraw_success extends AppCompatActivity {

    /* ================= EXTRAS ================= */
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_DATE = "created_at";
    public static final String EXTRA_REQUEST_ID = "request_id";

    /* ================= UI ================= */
    private ImageView btnBack, imgSuccess, imgMethod;
    private TextView txtTitle, txtMessage,txtDateTime;
    private TextView txtRewardType, txtAmount;
    private TextView txtVoucherCode, txtWithdrawDetails, btnDone;

    /* ================= FIREBASE ================= */
    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_success);
        makeFullScreen();

        db = FirebaseFirestore.getInstance();

        /* BIND UI */
        btnBack = findViewById(R.id.btnBack);
        imgSuccess = findViewById(R.id.imgSuccess);
        imgMethod = findViewById(R.id.imgMethod);

        txtTitle = findViewById(R.id.txtTitle);
        txtMessage = findViewById(R.id.txtMessage);
        txtRewardType = findViewById(R.id.txtRewardType);
        txtAmount = findViewById(R.id.txtAmount);
        txtVoucherCode = findViewById(R.id.txtVoucherCode);
        txtWithdrawDetails = findViewById(R.id.txtdetail);
        txtDateTime=findViewById(R.id.txtDateTime);
        btnDone = findViewById(R.id.btnDone);

        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());

        /* GET DATA */
        type = getIntent().getStringExtra(EXTRA_TYPE).toLowerCase();
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        String date=getIntent().getStringExtra(EXTRA_DATE);
        txtAmount.setText(amount);
        txtDateTime.setText(date);

        setMethodUI(type);
        observeRequestStatus(requestId);
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

    /* ================= METHOD ICON ================= */
    private void setMethodUI(String type) {

        if (type == null) return;

        switch (type.toLowerCase()) {

            case RedeemFragment.GOOGLE:
            case "google_play":
            case "googleplay":
                imgMethod.setImageResource(R.drawable.ic_google_play);
                txtRewardType.setText("Reward: Google Play Voucher");
                break;

            case RedeemFragment.AMAZON:
                imgMethod.setImageResource(R.drawable.ic_amazon);
                txtRewardType.setText("Reward: Amazon Gift Voucher");
                break;

            case RedeemFragment.PHONEPE:
                imgMethod.setImageResource(R.drawable.ic_phonepe);
                txtRewardType.setText("Reward: PhonePe Gift Voucher");
                break;

            case RedeemFragment.UPI:
                imgMethod.setImageResource(R.drawable.ic_upi);
                txtRewardType.setText("Payment Method: UPI");
                break;

            case RedeemFragment.BANK:
                imgMethod.setImageResource(R.drawable.ic_bank);
                txtRewardType.setText("Payment Method: Bank");
                break;
        }
    }

    /* ================= FIRESTORE STATUS (LIVE) ================= */
    private void observeRequestStatus(String requestId) {

        if (requestId == null) return;

        listener = db.collection("redeem_requests")
                .document(requestId)
                .addSnapshotListener((doc, e) -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (e != null || doc == null || !doc.exists()) return;

                    /* 🔄 RESET UI EVERY TIME */
                    txtVoucherCode.setVisibility(View.GONE);
                    txtWithdrawDetails.setVisibility(View.GONE);
                    txtVoucherCode.setOnClickListener(null);

                    String status = doc.getString("status");
                    String voucher = doc.getString("voucher_code");
                    String liveWithdrawDetails = doc.getString("withdraw_details");

                    if ("pending".equals(status)) {

                        imgSuccess.setImageResource(R.drawable.ic_processing);

                        txtTitle.setText("Processing ⏳");
                        txtTitle.setTextColor(Color.parseColor("#D97706"));
                        txtMessage.setText("Please wait while we process your request.");

                        if (RedeemFragment.UPI.equals(type)) {
                            txtMessage.setText("UPI amount will be credited within 24 hours.");
                        } else if (RedeemFragment.BANK.equals(type)) {
                            txtMessage.setText("Bank transfer will complete within 24–48 hours.");
                        }

                        showWithdrawDetailsIfNeeded(type, liveWithdrawDetails);
                    }

                    else if ("success".equals(status)) {

                        imgSuccess.setImageResource(R.drawable.ic_success);
                        txtTitle.setTextColor(Color.parseColor("#059669"));

                        if (isVoucherType(type)) {

                            txtTitle.setText("Redeem Successful 🎉");
                            txtMessage.setText("Your voucher code is ready below! Tap to copy.");

                            if (voucher != null && !voucher.trim().isEmpty()) {
                                txtVoucherCode.setVisibility(View.VISIBLE);
                                txtVoucherCode.setText("CODE: " + voucher + "  📋");
                                enableCopy(voucher);
                            }

                        } else {

                            txtTitle.setText("Withdraw Successful 🎉");
                            txtMessage.setText("Amount credited successfully.");
                            showWithdrawDetailsIfNeeded(type, liveWithdrawDetails);
                        }
                    }

                    else if ("failed".equals(status)) {

                        imgSuccess.setImageResource(R.drawable.ic_failed);
                        txtTitle.setText("Failed ❌");
                        txtTitle.setTextColor(Color.parseColor("#DC2626"));
                        txtMessage.setText("Coins will be refunded automatically.");
                    }
                });
    }

    /* ================= HELPERS ================= */
    private boolean isVoucherType(String type) {
        if (type == null) return false;
        String t = type.toLowerCase().trim();
        return RedeemFragment.GOOGLE.equals(t)
                || "google_play".equals(t)
                || "googleplay".equals(t)
                || RedeemFragment.AMAZON.equals(t)
                || RedeemFragment.PHONEPE.equals(t);
    }

    private void showWithdrawDetailsIfNeeded(String type, String details) {

        if (!(RedeemFragment.UPI.equals(type) || RedeemFragment.BANK.equals(type)))
            return;

        if (details == null || details.trim().isEmpty()) return;

        txtWithdrawDetails.setVisibility(View.VISIBLE);
        txtWithdrawDetails.setText(
                RedeemFragment.UPI.equals(type)
                        ? "UPI ID:\n" + details
                        : "Bank Details:\n" + details
        );

        txtWithdrawDetails.setOnClickListener(v -> {
            ClipboardManager cm =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(
                    ClipData.newPlainText("Withdraw Details", details)
            );
            Toast.makeText(this, "Details copied", Toast.LENGTH_SHORT).show();
        });
    }

    private void enableCopy(String code) {
        txtVoucherCode.setOnClickListener(v -> {
            ClipboardManager cm =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(
                    ClipData.newPlainText("Voucher Code", code)
            );
            Toast.makeText(this, "Voucher copied", Toast.LENGTH_SHORT).show();
        });
    }
    /* ================= CLEANUP ================= */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            listener.remove(); // 🔥 IMPORTANT
        }
    }
}