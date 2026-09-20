package com.app.rewardsplanet.models;

public class WatchVideoModel {
    private int id;
    private String title;
    private int coinReward;
    private int ticketReward;
    private boolean isCompleted;
    private boolean isLoading;

    public WatchVideoModel(int id, String title, int coinReward, int ticketReward, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.coinReward = coinReward;
        this.ticketReward = ticketReward;
        this.isCompleted = isCompleted;
        this.isLoading = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCoinReward() {
        return coinReward;
    }

    public void setCoinReward(int coinReward) {
        this.coinReward = coinReward;
    }

    public int getTicketReward() {
        return ticketReward;
    }

    public void setTicketReward(int ticketReward) {
        this.ticketReward = ticketReward;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }
}
