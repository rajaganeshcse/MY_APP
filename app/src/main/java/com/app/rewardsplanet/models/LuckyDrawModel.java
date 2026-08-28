package com.app.rewardsplanet.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@IgnoreExtraProperties
public class LuckyDrawModel {

    private boolean adJoined;
    private int myTicketsCount;

    private String id;
    private String status;
    private Long rewardCoins;
    private Long filledSlots;
    private Long totalSlots;

    private String winnerUid;
    private Timestamp createdAt;
    private Timestamp completedAt;

    private boolean joinedByMe;

    public LuckyDrawModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status == null ? "" : status; }

    public int getRewardCoins() {
        return rewardCoins == null ? 0 : rewardCoins.intValue();
    }

    public int getFilledSlots() {
        return filledSlots == null ? 0 : filledSlots.intValue();
    }

    public int getTotalSlots() {
        return totalSlots == null ? 0 : totalSlots.intValue();
    }

    public boolean isJoinedByMe() { return joinedByMe; }
    public void setJoinedByMe(boolean joinedByMe) {
        this.joinedByMe = joinedByMe;
    }

    public String getWinnerUid() {
        return winnerUid == null ? "" : winnerUid;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getCompletedAt() { return completedAt; }

    public boolean isFull() {
        return getTotalSlots() > 0 &&
                getFilledSlots() >= getTotalSlots();
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(getStatus());
    }

    public String getCompletedAtFormatted() {
        Timestamp time = (completedAt != null) ? completedAt : createdAt;
        if (time == null) return "N/A";

        Date date = time.toDate();

        return new SimpleDateFormat(
                "dd MMM yyyy hh:mm a",
                Locale.getDefault()
        ).format(date);
    }

    public String getCreatedAtFormatted() {
        if (createdAt == null) return "N/A";

        Date date = createdAt.toDate();

        return new SimpleDateFormat(
                "dd MMM yyyy hh:mm a",
                Locale.getDefault()
        ).format(date);
    }

    public boolean isAdJoined() { return adJoined; }
    public void setAdJoined(boolean adJoined) {
        this.adJoined = adJoined;
    }

    public int getMyTicketsCount() { return myTicketsCount; }
    public void setMyTicketsCount(int count) {
        this.myTicketsCount = count;
    }
}