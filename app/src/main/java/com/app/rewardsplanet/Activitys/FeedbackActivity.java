package com.app.rewardsplanet.Activitys;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private ImageView star1, star2, star3, star4, star5;
    private TextView txtRatingReaction;
    private EditText edtFeedbackComment;
    private TextView txtCharCount;
    private MaterialButton btnSubmitFeedback;
    private ProgressBar progressBarFeedback;
    private AdView adViewFeedback;

    private TextView chipCoins, chipWithdraw, chipUI, chipFeatures, chipBug, chipOther;

    private int selectedRating = 0;
    private String selectedTopic = "Coin Rewards";

    private String uid = "ANONYMOUS";
    private String userName = "User";
    private String userEmail = "unknown@mail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        setupStatusBar();
        initViews();
        setupUserContext();
        setupStarRating();
        setupTopicChips();
        setupCommentWatcher();
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

        star1 = findViewById(R.id.star1);
        star2 = findViewById(R.id.star2);
        star3 = findViewById(R.id.star3);
        star4 = findViewById(R.id.star4);
        star5 = findViewById(R.id.star5);
        txtRatingReaction = findViewById(R.id.txtRatingReaction);

        chipCoins = findViewById(R.id.chipTopicCoins);
        chipWithdraw = findViewById(R.id.chipTopicWithdraw);
        chipUI = findViewById(R.id.chipTopicUI);
        chipFeatures = findViewById(R.id.chipTopicFeatures);
        chipBug = findViewById(R.id.chipTopicBug);
        chipOther = findViewById(R.id.chipTopicOther);

        edtFeedbackComment = findViewById(R.id.edtFeedbackComment);
        txtCharCount = findViewById(R.id.txtCharCount);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);
        progressBarFeedback = findViewById(R.id.progressBarFeedback);
        adViewFeedback = findViewById(R.id.adViewFeedback);

        btnSubmitFeedback.setOnClickListener(v -> validateAndSubmitFeedback());
    }

    private void setupUserContext() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
            if (currentUser.getEmail() != null) userEmail = currentUser.getEmail();
            if (currentUser.getDisplayName() != null) userName = currentUser.getDisplayName();
        }
    }

    private void setupStarRating() {
        star1.setOnClickListener(v -> setRating(1));
        star2.setOnClickListener(v -> setRating(2));
        star3.setOnClickListener(v -> setRating(3));
        star4.setOnClickListener(v -> setRating(4));
        star5.setOnClickListener(v -> setRating(5));
    }

    private void setRating(int rating) {
        selectedRating = rating;

        star1.setImageResource(rating >= 1 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        star2.setImageResource(rating >= 2 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        star3.setImageResource(rating >= 3 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        star4.setImageResource(rating >= 4 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
        star5.setImageResource(rating >= 5 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);

        switch (rating) {
            case 1:
                txtRatingReaction.setText("Very Poor 😞");
                txtRatingReaction.setTextColor(Color.parseColor("#EF4444"));
                break;
            case 2:
                txtRatingReaction.setText("Needs Improvement 😕");
                txtRatingReaction.setTextColor(Color.parseColor("#F59E0B"));
                break;
            case 3:
                txtRatingReaction.setText("Average / Okay 🙂");
                txtRatingReaction.setTextColor(Color.parseColor("#3B82F6"));
                break;
            case 4:
                txtRatingReaction.setText("Great Experience! 😃");
                txtRatingReaction.setTextColor(Color.parseColor("#10B981"));
                break;
            case 5:
                txtRatingReaction.setText("Loved it! Best Gaming App! 🌟🔥");
                txtRatingReaction.setTextColor(Color.parseColor("#D97706"));
                break;
        }
    }

    private void setupTopicChips() {
        chipCoins.setOnClickListener(v -> selectTopic("Coin Rewards", chipCoins));
        chipWithdraw.setOnClickListener(v -> selectTopic("Withdrawal Speed", chipWithdraw));
        chipUI.setOnClickListener(v -> selectTopic("App Design & Speed", chipUI));
        chipFeatures.setOnClickListener(v -> selectTopic("New Feature Ideas", chipFeatures));
        chipBug.setOnClickListener(v -> selectTopic("Bug Report", chipBug));
        chipOther.setOnClickListener(v -> selectTopic("Other", chipOther));
    }

    private void selectTopic(String topic, TextView selectedView) {
        selectedTopic = topic;

        updateChip(chipCoins, chipCoins == selectedView);
        updateChip(chipWithdraw, chipWithdraw == selectedView);
        updateChip(chipUI, chipUI == selectedView);
        updateChip(chipFeatures, chipFeatures == selectedView);
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

    private void setupCommentWatcher() {
        edtFeedbackComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                txtCharCount.setText(length + " / 500");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateAndSubmitFeedback() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a star rating first", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = edtFeedbackComment.getText().toString().trim();
        if (comment.length() < 5) {
            edtFeedbackComment.setError("Please write at least a few words (min 5 characters)");
            edtFeedbackComment.requestFocus();
            return;
        }

        setLoading(true);

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("userId", uid);
        feedback.put("userName", userName);
        feedback.put("userEmail", userEmail);
        feedback.put("rating", selectedRating);
        feedback.put("topic", selectedTopic);
        feedback.put("comment", comment);
        feedback.put("createdAt", FieldValue.serverTimestamp());
        feedback.put("device", Build.MANUFACTURER + " " + Build.MODEL);
        feedback.put("osVersion", "Android " + Build.VERSION.RELEASE);
        feedback.put("appVersion", "1.0");

        FirebaseFirestore.getInstance().collection("feedbacks")
                .add(feedback)
                .addOnSuccessListener(docRef -> {
                    setLoading(false);
                    handleSuccessFlow();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        if (btnSubmitFeedback != null) {
            btnSubmitFeedback.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
        if (progressBarFeedback != null) {
            progressBarFeedback.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void handleSuccessFlow() {
        if (selectedRating == 5) {
            new AlertDialog.Builder(this)
                    .setTitle("Thank You So Much! 🌟")
                    .setMessage("We are thrilled to know you love RGamer! Would you mind taking 10 seconds to leave us a quick review on Google Play?")
                    .setPositiveButton("Rate on Play Store", (dialog, which) -> {
                        openPlayStore();
                        finish();
                    })
                    .setNegativeButton("Maybe Later", (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Feedback Received!")
                    .setMessage("Thank you for sharing your thoughts with us. Our team reads every piece of feedback to make RGamer better for everyone!")
                    .setPositiveButton("Done", (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void openPlayStore() {
        String packageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void setupAd() {
        if (adViewFeedback != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewFeedback.loadAd(adRequest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adViewFeedback != null) adViewFeedback.resume();
    }

    @Override
    protected void onPause() {
        if (adViewFeedback != null) adViewFeedback.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adViewFeedback != null) adViewFeedback.destroy();
        super.onDestroy();
    }
}
