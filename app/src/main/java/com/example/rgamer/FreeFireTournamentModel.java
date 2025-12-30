package com.example.rgamer;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class FreeFireTournamentModel {

    /* ================= BASIC ================= */
    private String id;
    private String game;
    private long coin;
    private long entryTickets;
    private long totalSlots;
    private long joinedSlots;
    private long startTimeMillis;
    private long created_at;

    /* ================= JOIN STATE (CURRENT USER) ================= */
    private boolean joined;              // current user joined?
    private String joinedUsername;       // current user's username
    private String joinedGameId;         // current user's game ID

    /* ================= ALL JOINED USERS (NEW) ================= */
    // key = uid
    private Map<String, JoinedUser> joinedUsers;

    // REQUIRED empty constructor (Firestore)
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

    /* ================= CURRENT USER JOIN ================= */
    public boolean isJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    public String getJoinedUsername() {
        return joinedUsername;
    }

    public void setJoinedUsername(String joinedUsername) {
        this.joinedUsername = joinedUsername;
    }

    public String getJoinedGameId() {
        return joinedGameId;
    }

    public void setJoinedGameId(String joinedGameId) {
        this.joinedGameId = joinedGameId;
    }

    /* ================= ALL JOINED USERS ================= */
    public Map<String, JoinedUser> getJoinedUsers() {
        if (joinedUsers == null) {
            joinedUsers = new HashMap<>();
        }
        return joinedUsers;
    }

    public void setJoinedUsers(Map<String, JoinedUser> joinedUsers) {
        this.joinedUsers = joinedUsers;
    }

    /* ================= INNER MODEL ================= */
    @IgnoreExtraProperties
    public static class JoinedUser {

        private String username;
        private String gameId;

        public JoinedUser() {}

        public JoinedUser(String username, String gameId) {
            this.username = username;
            this.gameId = gameId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }
    }
}
