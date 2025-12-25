package com.example.rgamer;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LuckyDrawModel {

    private String id;
    private String status;
    private Long rewardCoins;
    private Long filledSlots;
    private Long totalSlots;

    // local only
    private boolean joinedByMe;

    public LuckyDrawModel() {}

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

    public boolean isJoinedByMe() {
        return joinedByMe;
    }

    public void setJoinedByMe(boolean joinedByMe) {
        this.joinedByMe = joinedByMe;
    }

    public boolean isFull() {
        return getTotalSlots() > 0
                && getFilledSlots() >= getTotalSlots();
    }
}
