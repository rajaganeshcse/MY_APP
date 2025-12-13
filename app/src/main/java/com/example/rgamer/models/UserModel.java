package com.example.rgamer.models;

import java.util.HashMap;
import java.util.Map;

public class UserModel {

    private String uid;
    private String name;
    private String email;
    private int coins;
    private String token;
    private String profile_image;   // ✅ NEW
    private long created_at;

    // 🔹 Empty constructor (REQUIRED for Firestore)
    public UserModel() {}

    // 🔹 Full constructor
    public UserModel(
            String uid,
            String name,
            String email,
            int coins,
            String token,
            String profile_image,
            long created_at
    ) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.coins = coins;
        this.token = token;
        this.profile_image = profile_image;
        this.created_at = created_at;
    }

    // ---------------- GETTERS ----------------
    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public int getCoins() {
        return coins;
    }

    public String getToken() {
        return token;
    }

    public String getProfileImage() {
        return profile_image;
    }

    public long getCreatedAt() {
        return created_at;
    }

    // ---------------- SETTERS ----------------
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setProfileImage(String profile_image) {
        this.profile_image = profile_image;
    }

    public void setCreatedAt(long created_at) {
        this.created_at = created_at;
    }

    // ---------------- FIRESTORE MAP ----------------
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);
        map.put("coins", coins);
        map.put("token", token);
        map.put("profile_image", profile_image);
        map.put("created_at", created_at);

        return map;
    }
}
