package com.example.rgamer;

public class FreeFireTournamentModel {

    private String id;
    private String game;
    private long coin;
    private int entryTickets;
    private int totalSlots;
    private int joinedSlots;
    private long startTimeMillis;
    private long created_at;

    public FreeFireTournamentModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGame() { return game; }
    public long getCoin() { return coin; }
    public int getEntryTickets() { return entryTickets; }
    public int getTotalSlots() { return totalSlots; }
    public int getJoinedSlots() { return joinedSlots; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getCreated_at() { return created_at; }
}
