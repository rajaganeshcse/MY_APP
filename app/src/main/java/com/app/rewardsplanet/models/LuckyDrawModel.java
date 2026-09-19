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
    private String drawId;
    private Long drawNumber;
    private String status;
    private Long rewardCoins;
    private Long ticketCost;
    private Long filledSlots;
    private Long totalSlots;
    private Long participationLimit;
    private Long currentParticipation;

    private String winnerUid;
    private String winningToken;
    private String winnerToken;
    private String winnerName;
    private Timestamp createdAt;
    private Timestamp completedAt;

    private boolean joinedByMe;

    public LuckyDrawModel() {}

    public String getWinningToken() {
        if (winningToken != null && !winningToken.isEmpty()) return winningToken;
        if (winnerToken != null && !winnerToken.isEmpty()) return winnerToken;
        return "";
    }

    public void setWinningToken(String winningToken) {
        this.winningToken = winningToken;
    }

    public String getWinnerName() {
        return winnerName == null ? "" : winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public String getId() {
        if (drawId != null && !drawId.isEmpty()) return drawId;
        return id != null ? id : "";
    }
    public void setId(String id) { this.id = id; }

    public String getDrawId() {
        return drawId != null ? drawId : (id != null ? id : "");
    }
    public void setDrawId(String drawId) { this.drawId = drawId; }

    public Long getDrawNumber() { return drawNumber; }
    public void setDrawNumber(Long drawNumber) { this.drawNumber = drawNumber; }

    public String getStatus() { return status == null ? "" : status; }

    public int getRewardCoins() {
        return rewardCoins == null ? 0 : rewardCoins.intValue();
    }
    public void setRewardCoins(Long rewardCoins) { this.rewardCoins = rewardCoins; }

    public int getTicketCost() {
        return ticketCost == null ? 1 : ticketCost.intValue();
    }
    public void setTicketCost(Long ticketCost) { this.ticketCost = ticketCost; }

    public int getFilledSlots() {
        if (filledSlots != null) return filledSlots.intValue();
        if (currentParticipation != null) return currentParticipation.intValue();
        return 0;
    }

    public void setFilledSlots(Long filledSlots) {
        this.filledSlots = filledSlots;
    }
    public void setFilledSlots(long filledSlots) {
        this.filledSlots = filledSlots;
    }

    public int getTotalSlots() {
        if (totalSlots != null) return totalSlots.intValue();
        if (participationLimit != null) return participationLimit.intValue();
        return 10;
    }

    public void setTotalSlots(Long totalSlots) {
        this.totalSlots = totalSlots;
    }
    public void setTotalSlots(long totalSlots) {
        this.totalSlots = totalSlots;
    }

    public Long getCurrentParticipation() { return currentParticipation; }
    public void setCurrentParticipation(Long currentParticipation) {
        this.currentParticipation = currentParticipation;
    }

    public Long getParticipationLimit() { return participationLimit; }
    public void setParticipationLimit(Long participationLimit) {
        this.participationLimit = participationLimit;
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