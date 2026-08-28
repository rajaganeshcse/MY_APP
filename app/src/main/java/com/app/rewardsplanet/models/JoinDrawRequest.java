package com.app.rewardsplanet.models;

public class JoinDrawRequest {

    public String drawId;
    public int ticketCount;

    public JoinDrawRequest(String drawId, int ticketCount) {
        this.drawId = drawId;
        this.ticketCount = ticketCount;
    }
}