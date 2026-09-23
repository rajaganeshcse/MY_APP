package com.app.rewardsplanet.Activitys;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ContactUsActivity extends AppCompatActivity {

    private EditText edtContactEmail, edtContactSubject, edtContactMessage;
    private MaterialButton btnSubmitTicket;
    private ProgressBar progressBarContact;
    private AdView adViewContact;

    private TextView chipReward, chipWithdraw, chipAccount, chipBug, chipOther;
    private String selectedCategory = "Reward Issue";

    private String uid = "ANONYMOUS";
    private String userName = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        setupStatusBar();
        initViews();
        setupUserContext();
        setupCategoryChips();
        setupQuickChannels();
        setupAd();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.parseColor("#F8FAFC"));
        }
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        edtContactEmail = findViewById(R.id.edtContactEmail);
        edtContactSubject = findViewById(R.id.edtContactSubject);
        edtContactMessage = findViewById(R.id.edtContactMessage);
        btnSubmitTicket = findViewById(R.id.btnSubmitTicket);
        progressBarContact = findViewById(R.id.progressBarContact);
        adViewContact = findViewById(R.id.adViewContact);

        chipReward = findViewById(R.id.chipCategoryReward);
        chipWithdraw = findViewById(R.id.chipCategoryWithdraw);
        chipAccount = findViewById(R.id.chipCategoryAccount);
        chipBug = findViewById(R.id.chipCategoryBug);
        chipOther = findViewById(R.id.chipCategoryOther);

        btnSubmitTicket.setOnClickListener(v -> validateAndSubmitTicket());
    }

    private void setupUserContext() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                edtContactEmail.setText(currentUser.getEmail());
            }
            if (currentUser.getDisplayName() != null) {
                userName = currentUser.getDisplayName();
            }
        }
    }

    private void setupCategoryChips() {
        chipReward.setOnClickListener(v -> selectCategory("Reward Issue", chipReward));
        chipWithdraw.setOnClickListener(v -> selectCategory("Withdrawal Status", chipWithdraw));
        chipAccount.setOnClickListener(v -> selectCategory("Account & Login", chipAccount));
        chipBug.setOnClickListener(v -> selectCategory("Bug Report", chipBug));
        chipOther.setOnClickListener(v -> selectCategory("Other", chipOther));
    }

    private void selectCategory(String category, TextView selectedView) {
        selectedCategory = category;

        updateChip(chipReward, chipReward == selectedView);
        updateChip(chipWithdraw, chipWithdraw == selectedView);
        updateChip(chipAccount, chipAccount == selectedView);
        updateChip(chipBug, chipBug == selectedView);
        updateChip(chipOther, chipOther == selectedView);
    }

    private void updateChip(TextView chip, boolean isSelected) {
        if (chip == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_tab_selected);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_tab_unselected);
            chip.setTextColor(Color.parseColor("#475569"));
        }
    }

    private void setupQuickChannels() {
        MaterialCardView cardChannelEmail = findViewById(R.id.cardChannelEmail);
        if (cardChannelEmail != null) {
            cardChannelEmail.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:support@rgamer.app"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[RGamer Support] Inquiry");
                emailIntent.putExtra(Intent.EXTRA_TEXT,
                        "\n\n---\nAccount ID: " + uid + "\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL + "\nOS: Android " + Build.VERSION.RELEASE);
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                } catch (Exception e) {
                    Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        MaterialCardView cardChannelTelegram = findViewById(R.id.cardChannelTelegram);
        if (cardChannelTelegram != null) {
            cardChannelTelegram.setOnClickListener(v -> {
                try {
                    Intent tgIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/rgamerapp"));
                    startActivity(tgIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open Telegram link", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void validateAndSubmitTicket() {
        String email = edtContactEmail.getText().toString().trim();
        String subject = edtContactSubject.getText().toString().trim();
        String message = edtContactMessage.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtContactEmail.setError("Please enter a valid email address");
            edtContactEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(subject)) {
            edtContactSubject.setError("Please enter a subject");
            edtContactSubject.requestFocus();
            return;
        }

        if (message.length() < 10) {
            edtContactMessage.setError("Message must be at least 10 characters");
            edtContactMessage.requestFocus();
            return;
        }

        setLoading(true);

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("userId", uid);
        ticket.put("userName", userName);
        ticket.put("userEmail", email);
        ticket.put("category", selectedCategory);
        ticket.put("subject", subject);
        ticket.put("message", message);
        ticket.put("status", "OPEN");
        ticket.put("createdAt", FieldValue.serverTimestamp());
        ticket.put("device", Build.MANUFACTURER + " " + Build.MODEL);
        ticket.put("osVersion", "Android " + Build.VERSION.RELEASE);
        ticket.put("appVersion", "1.0");

        FirebaseFirestore.getInstance().collection("support_tickets")
                .add(ticket)
                .addOnSuccessListener(docRef -> {
                    setLoading(false);
                    showSuccessDialog(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to submit ticket: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        if (btnSubmitTicket != null) {
            btnSubmitTicket.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
        if (progressBarContact != null) {
            progressBarContact.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showSuccessDialog(String ticketId) {
        new AlertDialog.Builder(this)
                .setTitle("Ticket Submitted!")
                .setMessage("Your support ticket #" + ticketId.substring(0, Math.min(8, ticketId.length())).toUpperCase()
                        + " has been received.\n\nOur team will review your query and reply to your email within 12-24 hours.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void setupAd() {
        if (adViewContact != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewContact.loadAd(adRequest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adViewContact != null) adViewContact.resume();
    }

    @Override
    protected void onPause() {
        if (adViewContact != null) adViewContact.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adViewContact != null) adViewContact.destroy();
        super.onDestroy();
    }
}
