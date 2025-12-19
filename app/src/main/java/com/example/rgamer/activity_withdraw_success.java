package com.example.rgamer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class activity_withdraw_success extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_REQUEST_ID = "request_id";

    // UI
    private ImageView imgSuccess;
    private TextView txtTitle, txtMessage, txtAmount;
    private TextView txtRewardType, txtVoucherCode;
    private TextView btnDone;

    // Firebase
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔥 FULL SCREEN STATUS BAR (ADDED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_success);

        db = FirebaseFirestore.getInstance();

        // Bind UI
        imgSuccess = findViewById(R.id.imgSuccess);
        txtTitle = findViewById(R.id.txtTitle);
        txtMessage = findViewById(R.id.txtMessage);
        txtRewardType = findViewById(R.id.txtRewardType);
        txtAmount = findViewById(R.id.txtAmount);
        txtVoucherCode = findViewById(R.id.txtVoucherCode);
        btnDone = findViewById(R.id.btnDone);

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);

        txtAmount.setText(amount);
        txtVoucherCode.setVisibility(View.GONE);

        // -------- UPI / BANK --------
        if (RedeemFragment.UPI.equals(type)) {

            setSuccessUI();
            txtTitle.setText("Withdraw Submitted 🎉");
            txtMessage.setText("UPI amount will be credited within 24 hours.");
            txtRewardType.setText("Payment Method: UPI");

        } else if (RedeemFragment.BANK.equals(type)) {

            setSuccessUI();
            txtTitle.setText("Withdraw Submitted 🎉");
            txtMessage.setText("Bank transfer will complete within 24–48 hours.");
            txtRewardType.setText("Payment Method: Bank");

        } else {
            // -------- VOUCHER BASED --------
            txtRewardType.setText("Reward: " + getRewardName(type));
            observeRedeemRequest(requestId);
        }

        btnDone.setOnClickListener(v -> finish());
    }

    // ================= FIRESTORE LISTENER =================
    private void observeRedeemRequest(String requestId) {

        setProcessingUI();

        db.collection("redeem_requests")
                .document(requestId)
                .addSnapshotListener((doc, e) -> {

                    if (doc == null || !doc.exists()) return;

                    String status = doc.getString("status");
                    String voucher = doc.getString("voucher_code");

                    if ("pending".equals(status)) {

                        setProcessingUI();
                        txtVoucherCode.setVisibility(View.GONE);

                    } else if ("success".equals(status)) {

                        setSuccessUI();
                        txtVoucherCode.setVisibility(View.VISIBLE);

                        if (voucher == null || voucher.isEmpty()) {
                            txtVoucherCode.setText("CODE: N/A");
                        } else {
                            txtVoucherCode.setText("CODE: " + voucher);
                            enableCopy(voucher);
                        }

                    } else if ("failed".equals(status)) {

                        setFailedUI();
                        txtVoucherCode.setVisibility(View.GONE);
                    }
                });
    }

    // ================= UI STATES =================
    private void setProcessingUI() {
        imgSuccess.setImageResource(R.drawable.ic_processing);
        txtTitle.setText("Processing ⏳");
        txtMessage.setText("Please wait while we process your reward.");
    }

    private void setSuccessUI() {
        imgSuccess.setImageResource(R.drawable.ic_success);
        txtTitle.setText("Redeem Successful 🎉");
        txtMessage.setText("Your reward is ready!");
    }

    private void setFailedUI() {
        imgSuccess.setImageResource(R.drawable.ic_failed);
        txtTitle.setText("Redeem Failed ❌");
        txtMessage.setText("Coins will be refunded automatically.");
    }

    // ================= COPY =================
    private void enableCopy(String code) {
        txtVoucherCode.setOnClickListener(v -> {
            ClipboardManager cm =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(
                    ClipData.newPlainText("Voucher Code", code)
            );
            Toast.makeText(this, "Voucher code copied", Toast.LENGTH_SHORT).show();
        });
    }

    // ================= REWARD NAME =================
    private String getRewardName(String type) {
        switch (type) {
            case RedeemFragment.GOOGLE:
                return "Google Play Voucher";
            case RedeemFragment.AMAZON:
                return "Amazon Gift Voucher";
            case RedeemFragment.PHONEPE:
                return "PhonePe Gift Voucher";
            default:
                return "Reward";
        }
    }
}
