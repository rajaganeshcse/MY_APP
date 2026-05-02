package com.example.rgamer.withdraws;

import com.google.firebase.Timestamp;

public class CoinModel {

    private int amount;
    private String istype;
    private Timestamp created_at;
    private String status;
    private String type;

    // 🔥 Required empty constructor for Firestore
    public CoinModel() {}

    // ✅ Getters
    public int getAmount() {
        return amount;
    }

    public Timestamp getCreatedAt() {
        return created_at;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getIstype(){
        return istype;
    }

    // 🔥 Safe time conversion (IMPORTANT)
    public long getTimeMillis() {
        if (created_at != null) {
            return created_at.toDate().getTime();
        } else {
            return System.currentTimeMillis(); // fallback (prevents crash)
        }
    }
}