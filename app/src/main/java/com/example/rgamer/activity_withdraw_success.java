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

    /* ================= EXTRAS ================= */
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_WITHDRAW_DETAILS = "withdraw_details";

    /* ================= UI ================= */
    ImageView btnBack, imgSuccess, imgMethod;
    TextView txtHeader, txtTitle, txtMessage;
    TextView txtRewardType, txtAmount;
    TextView txtVoucherCode, txtWithdrawDetails;
    TextView btnDone;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔹 FULL SCREEN STATUS BAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_success);

        db = FirebaseFirestore.getInstance();

        /* ================= BIND UI ================= */
        btnBack = findViewById(R.id.btnBack);
        imgSuccess = findViewById(R.id.imgSuccess);
        imgMethod = findViewById(R.id.imgMethod);

        txtHeader = findViewById(R.id.txtHeader);
        txtTitle = findViewById(R.id.txtTitle);
        txtMessage = findViewById(R.id.txtMessage);
        txtRewardType = findViewById(R.id.txtRewardType);
        txtAmount = findViewById(R.id.txtAmount);
        txtVoucherCode = findViewById(R.id.txtVoucherCode);
        txtWithdrawDetails = findViewById(R.id.txtAmount);
        btnDone = findViewById(R.id.btnDone);

        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());

        /* ================= GET DATA ================= */
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        String withdrawDetails =
                getIntent().getStringExtra(EXTRA_WITHDRAW_DETAILS);

        txtAmount.setText(amount);
        txtVoucherCode.setVisibility(View.GONE);
        txtWithdrawDetails.setVisibility(View.GONE);

        setMethodUI(type);

        /* ================= HANDLE TYPE ================= */
        if (RedeemFragment.UPI.equals(type)) {

            setSuccessUI();
            txtTitle.setText("Withdraw Submitted 🎉");
            txtMessage.setText("UPI amount will be credited within 24 hours.");

            showWithdrawDetails("UPI ID", withdrawDetails);

        } else if (RedeemFragment.BANK.equals(type)) {

            setSuccessUI();
            txtTitle.setText("Withdraw Submitted 🎉");
            txtMessage.setText("Bank transfer will complete within 24–48 hours.");

            showWithdrawDetails("Bank Details", withdrawDetails);

        } else {
            observeRedeemRequest(requestId);
        }
    }

    /* ================= METHOD UI ================= */
    private void setMethodUI(String type) {

        if (type == null) return;

        switch (type) {

            case RedeemFragment.GOOGLE:
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

    /* ================= SHOW WITHDRAW DETAILS ================= */
    private void showWithdrawDetails(String title, String details) {

        if (details == null || details.isEmpty()) return;

        txtWithdrawDetails.setVisibility(View.VISIBLE);
        txtWithdrawDetails.setText(title + ":\n" + details);

        // copy on tap
        txtWithdrawDetails.setOnClickListener(v -> {
            ClipboardManager cm =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(
                    ClipData.newPlainText(title, details)
            );
            Toast.makeText(this,
                    "Details copied",
                    Toast.LENGTH_SHORT).show();
        });
    }

    /* ================= FIRESTORE ================= */
    private void observeRedeemRequest(String requestId) {

        if (requestId == null) return;

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

    /* ================= UI STATES ================= */
    private void setProcessingUI() {
        imgSuccess.setImageResource(R.drawable.ic_processing);
        txtTitle.setText("Processing ⏳");
        txtMessage.setText("Please wait while we process your request.");
    }

    private void setSuccessUI() {
        imgSuccess.setImageResource(R.drawable.ic_success);
    }

    private void setFailedUI() {
        imgSuccess.setImageResource(R.drawable.ic_failed);
        txtTitle.setText("Failed ❌");
        txtMessage.setText("Coins will be refunded automatically.");
    }

    /* ================= COPY ================= */
    private void enableCopy(String code) {
        txtVoucherCode.setOnClickListener(v -> {
            ClipboardManager cm =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(
                    ClipData.newPlainText("Voucher Code", code)
            );
            Toast.makeText(this,
                    "Voucher copied",
                    Toast.LENGTH_SHORT).show();
        });
    }
}
