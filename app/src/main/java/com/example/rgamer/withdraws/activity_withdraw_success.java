package com.example.rgamer.withdraws;

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

import com.example.rgamer.Fragements.RedeemFragment;
import com.example.rgamer.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class activity_withdraw_success extends AppCompatActivity {

    /* ================= EXTRAS ================= */
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_REQUEST_ID = "request_id";

    /* ================= UI ================= */
    private ImageView btnBack, imgSuccess, imgMethod;
    private TextView txtTitle, txtMessage;
    private TextView txtRewardType, txtAmount;
    private TextView txtVoucherCode, txtWithdrawDetails, btnDone;

    /* ================= FIREBASE ================= */
    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /* FULL SCREEN */
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
        btnDone = findViewById(R.id.btnDone);

        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());

        /* GET DATA */
        type = getIntent().getStringExtra(EXTRA_TYPE).toLowerCase();
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);

        txtAmount.setText(amount);

        setMethodUI(type);
        observeRequestStatus(requestId);
    }

    /* ================= METHOD ICON ================= */
    private void setMethodUI(String type) {

        if (type == null) return;

        switch (type.toLowerCase()) {

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

    /* ================= FIRESTORE STATUS (LIVE) ================= */
    private void observeRequestStatus(String requestId) {

        if (requestId == null) return;

        listener = db.collection("redeem_requests")
                .document(requestId)
                .addSnapshotListener((doc, e) -> {

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

                        if (isVoucherType(type)) {

                            txtTitle.setText("Redeem Successful 🎉");
                            txtMessage.setText("Your voucher is ready!");

                            if (voucher != null && !voucher.trim().isEmpty()) {
                                txtVoucherCode.setVisibility(View.VISIBLE);
                                txtVoucherCode.setText("CODE: " + voucher);
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
                        txtMessage.setText("Coins will be refunded automatically.");
                    }
                });
    }

    /* ================= HELPERS ================= */
    private boolean isVoucherType(String type) {
        return RedeemFragment.GOOGLE.equals(type)
                || RedeemFragment.AMAZON.equals(type)
                || RedeemFragment.PHONEPE.equals(type);
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