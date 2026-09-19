package com.app.rewardsplanet;

public class User {
    public String uid, name, image;
    public int streak_count;
    public String profile_pic;
    public long coins;          // total coins — used for leaderboard ranking
    public int rank;            // optional pre-computed rank from backend

    public User() {}
}