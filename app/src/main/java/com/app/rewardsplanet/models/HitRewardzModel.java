package com.app.rewardsplanet.models;

public class HitRewardzModel {

    private int id;
    private String title;
    private int coins;
    private int tickets;
    private boolean isCompleted;
    private boolean isLocked;

    public HitRewardzModel(int id, String title, int coins, int tickets, boolean isCompleted, boolean isLocked) {
        this.id = id;
        this.title = title;
        this.coins = coins;
        this.tickets = tickets;
        this.isCompleted = isCompleted;
        this.isLocked = isLocked;
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

    public int getTickets() {
        return tickets;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }
}
