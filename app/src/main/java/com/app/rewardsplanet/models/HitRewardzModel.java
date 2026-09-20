package com.app.rewardsplanet.models;

public class HitRewardzModel {

    private int id;
    private String title;
    private int coins;
    private String durationText;
    private boolean isCompleted;

    public HitRewardzModel(int id, String title, int coins, String durationText, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.coins = coins;
        this.durationText = durationText;
        this.isCompleted = isCompleted;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getCoins() {
        return coins;
    }

    public String getDurationText() {
        return durationText;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
