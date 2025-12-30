package com.example.rgamer;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class FreeFireTournamentModel {

    private String id;
    private String game;
    private long coin;
    private long entryTickets;   // 🔥 use long
    private long totalSlots;     // 🔥 use long
    private long joinedSlots;    // 🔥 use long
    private long startTimeMillis;
    private long created_at;

    // REQUIRED empty constructor
    public FreeFireTournamentModel() {}

    /* ================= ID ================= */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /* ================= GAME ================= */
    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    /* ================= COINS ================= */
    public long getCoin() {
        return coin;
    }

    public void setCoin(long coin) {
        this.coin = coin;
    }

    /* ================= ENTRY ================= */
    public int getEntryTickets() {
        return (int) entryTickets;
    }

    public void setEntryTickets(long entryTickets) {
        this.entryTickets = entryTickets;
    }

    /* ================= SLOTS ================= */
    public int getTotalSlots() {
        return (int) totalSlots;
    }

    public void setTotalSlots(long totalSlots) {
        this.totalSlots = totalSlots;
    }

    public int getJoinedSlots() {
        return (int) joinedSlots;
    }

    public void setJoinedSlots(long joinedSlots) {
        this.joinedSlots = joinedSlots;
    }

    /* ================= TIME ================= */
    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }
}
