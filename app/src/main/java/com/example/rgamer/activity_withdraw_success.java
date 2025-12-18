package com.example.rgamer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_success   );

        db = FirebaseFirestore.getInstance();

        // Bind UI
        imgSuccess = findViewById(R.id.imgSuccess);
        txtTitle = findViewById(R.id.txtTitle);
        txtMessage = findViewById(R.id.txtMessage);
        txtRewardType = findViewById(R.id.txtRewardType);
        txtAmount = findViewById(R.id.txtAmount);
        txtVoucherCode = findViewById(R.id.txtVoucherCode);
        btnDone = findViewById(R.id.btnDone);

        // Intent data
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        String amount = getIntent().getStringExtra(EXTRA_AMOUNT);
        String requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);

        txtAmount.setText(amount);

        // Default hide voucher
        txtVoucherCode.setVisibility(View.GONE);

        // -------- UPI / BANK (NO VOUCHER) --------
        if (RedeemFragment.UPI.equals(type)) {

            txtTitle.setText("Withdraw Submitted 🎉");
            txtMessage.setText("UPI amount will be credited within 24 hours.");
            txtRewardType.setText("Payment Method: UPI");

        } else if (RedeemFragment.BANK.equals(type)) {

            txtTitle.setText("Withdraw Submitted 🎉");
            txtMessage.setText("Bank transfer will complete within 24–48 hours.");
            txtRewardType.setText("Payment Method: Bank");

        } else {
            // -------- VOUCHER BASED --------
            txtRewardType.setText("Reward: " + getRewardName(type));
            observeRedeemRequest(requestId, type);
        }

        btnDone.setOnClickListener(v -> finish());
    }

    // ================= FIRESTORE LISTENER =================
    private void observeRedeemRequest(String requestId, String type) {

        txtTitle.setText("Redeem Status");
        txtMessage.setText("Your reward is being processed…");

        db.collection("redeem_requests")
                .document(requestId)
                .addSnapshotListener((doc, e) -> {

                    if (doc == null || !doc.exists()) return;

                    String status = doc.getString("status");
                    String voucher = doc.getString("voucher_code");

                    if ("pending".equals(status)) {

                        txtTitle.setText("Processing ⏳");
                        txtMessage.setText("Please wait while we process your reward.");
                        txtVoucherCode.setVisibility(View.GONE);

                    } else if ("success".equals(status)) {

                        txtTitle.setText("Redeem Successful 🎉");
                        txtMessage.setText("Your voucher is ready!");
                        txtVoucherCode.setVisibility(View.VISIBLE);

                        if (voucher == null || voucher.isEmpty()) {
                            txtVoucherCode.setText("CODE: N/A");
                        } else {
                            txtVoucherCode.setText("CODE: " + voucher);
                            enableCopy(voucher);
                        }

                    } else if ("failed".equals(status)) {

                        txtTitle.setText("Redeem Failed ❌");
                        txtMessage.setText("Coins will be refunded automatically.");
                        txtVoucherCode.setVisibility(View.GONE);
                    }
                });
    }

    // ================= COPY TO CLIPBOARD =================
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
