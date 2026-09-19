package com.app.rewardsplanet.models;

import java.util.List;

public class JoinResponse {
    public boolean success;
    public String message;
    public String error;
    public String drawId;
    public int ticketsEntered;
    public Integer ticketsDeducted;
    public Integer remainingTickets;
    public List<String> tokens;
}