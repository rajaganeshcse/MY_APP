package com.app.rewardsplanet.models;

import java.util.List;

public class JoinResponse {
    public boolean success;
    public String message;
    public String error;
    public String drawId;
    public int ticketsEntered;
    public List<String> tokens;
}