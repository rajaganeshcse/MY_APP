package com.example.rgamer;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@IgnoreExtraProperties
public class LuckyDrawModel {

    /* ================= FIRESTORE FIELDS ================= */

    private String id;
    private String status;
    private Long rewardCoins;
    private Long filledSlots;
    private Long totalSlots;

    // winner fields (for COMPLETED draws)
    private String winnerUid;
    private Timestamp createdAt;
    private Timestamp completedAt;

    /* ================= LOCAL ONLY ================= */

    // UI only (not stored in Firestore)
    private boolean joinedByMe;

    /* ================= CONSTRUCTOR ================= */

    public LuckyDrawModel() {}

    /* ================= BASIC GETTERS ================= */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status == null ? "" : status;
    }

    public int getRewardCoins() {
        return rewardCoins == null ? 0 : rewardCoins.intValue();
    }

    public int getFilledSlots() {
        return filledSlots == null ? 0 : filledSlots.intValue();
    }

    public int getTotalSlots() {
        return totalSlots == null ? 0 : totalSlots.intValue();
    }

    /* ================= JOIN STATE ================= */

    public boolean isJoinedByMe() {
        return joinedByMe;
    }

    public void setJoinedByMe(boolean joinedByMe) {
        this.joinedByMe = joinedByMe;
    }

    /* ================= WINNER INFO ================= */

    public String getWinnerUid() {
        return winnerUid == null ? "" : winnerUid;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    /* ================= HELPERS ================= */

    public boolean isFull() {
        return getTotalSlots() > 0
                && getFilledSlots() >= getTotalSlots();
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(getStatus());
    }

    /* ================= DATE FORMATTERS ================= */

    // Example output:
    // 26 December 2025 at 00:55:30
    public String getCompletedAtFormatted() {

        if (completedAt == null) return "N/A";

        Date date = completedAt.toDate();

        SimpleDateFormat sdf = new SimpleDateFormat(
                "dd MMMM yyyy 'at' HH:mm:ss",
                Locale.getDefault()
        );

        return sdf.format(date);
    }

    public String getCreatedAtFormatted() {

        if (createdAt == null) return "N/A";

        Date date = createdAt.toDate();

        SimpleDateFormat sdf = new SimpleDateFormat(
                "dd MMMM yyyy 'at' HH:mm:ss",
                Locale.getDefault()
        );

        return sdf.format(date);
    }
}
