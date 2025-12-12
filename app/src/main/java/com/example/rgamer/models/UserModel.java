package com.example.rgamer.models;

import java.util.HashMap;
import java.util.Map;

public class UserModel {

    private String uid;
    private String name;
    private String email;
    private int coins;
    private String token;
    private long created_at;

    // Empty constructor required for Firestore
    public UserModel() {}

    public UserModel(String uid, String name, String email, int coins, String token, long created_at) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.coins = coins;
        this.token = token;
        this.created_at = created_at;
    }

    // Getters */
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getCoins() { return coins; }
    public String getToken() { return token; }
    public long getCreatedAt() { return created_at; }

    // Setters */
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setToken(String token) { this.token = token; }
    public void setCreatedAt(long created_at) { this.created_at = created_at; }

    // Convert Model → Firestore Map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);
        map.put("coins", coins);
        map.put("token", token);
        map.put("created_at", created_at);
        return map;
    }
}
