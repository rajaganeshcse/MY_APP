package com.app.rewardsplanet.models;

import com.google.gson.annotations.SerializedName;

public class ScratchResponse {

    @SerializedName("allowed")
    private boolean allowed;

    @SerializedName("reward")
    private int reward;

    @SerializedName("coins")
    private int coins;

    @SerializedName("remaining")
    private int remaining;

    @SerializedName("message")
    private String message;

    public boolean isAllowed() {
        return allowed;
    }

    public int getReward() {
        return reward;
    }

    public int getCoins() {
        return coins;
    }

    public int getRemaining() {
        return remaining;
    }

    public String getMessage() {
        return message;
    }
}