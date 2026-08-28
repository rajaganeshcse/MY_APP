package com.app.rewardsplanet.Game;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class FreeFireTournamentModel{

    /* ================= BASIC ================= */
    private String id;
    private String game;
    private long coin;
    private long entryTickets;
    private long totalSlots;
    private long joinedSlots;
    private long startTimeMillis;
    private long created_at;


    // 🔥 FROM TOURNAMENT DB
    private String roomId;
    private String roomPassword;

    // 🔥 USER STATE
    private boolean isJoined = false;


    /* ================= JOIN STATE (CURRENT USER) ================= */
    private boolean joined;
    private String joinedUsername;
    private String joinedGameId;

    /* ================= ALL JOINED USERS ================= */
    // key = uid
    private Map<String, JoinedUser> joinedUsers;

    // REQUIRED empty constructor



    public String getRoomId() { return roomId; }
    public String getRoomPassword() { return roomPassword; }

    public FreeFireTournamentModel(){   }

    /* ================= BASIC GETTERS ================= */
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getGame()
    {
        return game;
    }
    public void setGame(String game) { this.game = game; }
    public long getCoin() { return coin; }
    public void setCoin(long coin) { this.coin = coin; }

    public int getEntryTickets() { return (int) entryTickets; }
    public void setEntryTickets(long entryTickets) {
        this.entryTickets = entryTickets;
    }

    public int getTotalSlots() { return (int) totalSlots; }
    public void setTotalSlots(long totalSlots) {
        this.totalSlots = totalSlots;
    }

    public int getJoinedSlots() { return (int) joinedSlots; }
    public void setJoinedSlots(long joinedSlots) {
        this.joinedSlots = joinedSlots;
    }

    public long getStartTimeMillis() { return startTimeMillis; }
    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getCreated_at() { return created_at; }
    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    /* ================= CURRENT USER JOIN ================= */
    public boolean isJoined() { return joined; }
    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    public String getJoinedUsername() { return joinedUsername; }
    public void setJoinedUsername(String joinedUsername) {
        this.joinedUsername = joinedUsername;
    }

    public String getJoinedGameId() { return joinedGameId; }
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

        private String uid;        // 👤 user id
        private String username;
        private String gameId;
        private boolean winner;    // 🏆 winner flag
        private long joinedAt;     // ⏱ optional (millis)

        public JoinedUser() {}

        public JoinedUser(
                String uid,
                String username,
                String gameId,
                boolean winner,
                long joinedAt
        )
        {
            this.uid = uid;
            this.username = username;
            this.gameId = gameId;
            this.winner = winner;
            this.joinedAt = joinedAt;
        }
        public String getUid() {
            return uid;
        }
        public void setUid(String uid){
            this.uid = uid;
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

        public boolean isWinner() {
            return winner;
        }

        public void setWinner(boolean winner) {
            this.winner = winner;
        }

        public long getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(long joinedAt) {
            this.joinedAt = joinedAt;
        }
    }
}
