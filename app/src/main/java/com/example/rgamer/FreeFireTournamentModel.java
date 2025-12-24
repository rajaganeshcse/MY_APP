package com.example.rgamer;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

@IgnoreExtraProperties
public class FreeFireTournamentModel {

    // 🔹 Firestore document ID (not stored)
    private String id;

    // 🔹 Firestore fields
    private String game;
    private int prize_coins;
    private int total_slots;
    private int joined_slots;
    private int entry_tickets;
    private String status;
    private long created_at;

    // 🔹 REQUIRED empty constructor
    public FreeFireTournamentModel() {}

    /* ================= GETTERS ================= */

    public String getId() {
        return id;
    }

    public String getGame() {
        return game;
    }

    @PropertyName("prize_coins")
    public int getPrizeCoins() {
        return prize_coins;
    }

    @PropertyName("total_slots")
    public int getTotalSlots() {
        return total_slots;
    }

    @PropertyName("joined_slots")
    public int getJoinedSlots() {
        return joined_slots;
    }

    @PropertyName("entry_tickets")
    public int getEntryTickets() {
        return entry_tickets;
    }

    public String getStatus() {
        return status;
    }

    @PropertyName("created_at")
    public long getCreatedAt() {
        return created_at;
    }

    /* ================= SETTERS ================= */

    // 🔹 Set manually from Firestore document ID
    public void setId(String id) {
        this.id = id;
    }
}
