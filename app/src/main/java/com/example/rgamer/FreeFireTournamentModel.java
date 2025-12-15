package com.example.rgamer;

import java.util.HashMap;
import java.util.Map;

public class FreeFireTournamentModel {

    private String tournament_id;
    private String game_name;          // FREE FIRE
    private String banner_image;       // image url
    private int prize_coins;
    private int total_slots;
    private int joined_slots;
    private int entry_tickets;
    private String status;             // OPEN / CLOSED / COMPLETED
    private long start_time;
    private long created_at;

    // 🔹 Empty constructor (REQUIRED for Firestore)
    public FreeFireTournamentModel() {}

    // 🔹 Full constructor
    public FreeFireTournamentModel(
            String tournament_id,
            String game_name,
            String banner_image,
            int prize_coins,
            int total_slots,
            int joined_slots,
            int entry_tickets,
            String status,
            long start_time,
            long created_at
    ) {
        this.tournament_id = tournament_id;
        this.game_name = game_name;
        this.banner_image = banner_image;
        this.prize_coins = prize_coins;
        this.total_slots = total_slots;
        this.joined_slots = joined_slots;
        this.entry_tickets = entry_tickets;
        this.status = status;
        this.start_time = start_time;
        this.created_at = created_at;
    }

    // ---------------- GETTERS ----------------
    public String getTournamentId() {
        return tournament_id;
    }

    public String getGameName() {
        return game_name;
    }

    public String getBannerImage() {
        return banner_image;
    }

    public int getPrizeCoins() {
        return prize_coins;
    }

    public int getTotalSlots() {
        return total_slots;
    }

    public int getJoinedSlots() {
        return joined_slots;
    }

    public int getEntryTickets() {
        return entry_tickets;
    }

    public String getStatus() {
        return status;
    }

    public long getStartTime() {
        return start_time;
    }

    public long getCreatedAt() {
        return created_at;
    }

    // ---------------- SETTERS ----------------
    public void setTournamentId(String tournament_id) {
        this.tournament_id = tournament_id;
    }

    public void setGameName(String game_name) {
        this.game_name = game_name;
    }

    public void setBannerImage(String banner_image) {
        this.banner_image = banner_image;
    }

    public void setPrizeCoins(int prize_coins) {
        this.prize_coins = prize_coins;
    }

    public void setTotalSlots(int total_slots) {
        this.total_slots = total_slots;
    }

    public void setJoinedSlots(int joined_slots) {
        this.joined_slots = joined_slots;
    }

    public void setEntryTickets(int entry_tickets) {
        this.entry_tickets = entry_tickets;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStartTime(long start_time) {
        this.start_time = start_time;
    }

    public void setCreatedAt(long created_at) {
        this.created_at = created_at;
    }

    // ---------------- FIRESTORE MAP ----------------
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();
        map.put("tournament_id", tournament_id);
        map.put("game_name", game_name);
        map.put("banner_image", banner_image);
        map.put("prize_coins", prize_coins);
        map.put("total_slots", total_slots);
        map.put("joined_slots", joined_slots);
        map.put("entry_tickets", entry_tickets);
        map.put("status", status);
        map.put("start_time", start_time);
        map.put("created_at", created_at);

        return map;
    }
}
