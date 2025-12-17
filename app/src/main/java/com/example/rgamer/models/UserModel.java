package com.example.rgamer.models;

import java.util.HashMap;
import java.util.Map;

public class UserModel {

    // ================= BASIC INFO =================
    private String uid;
    private String name;
    private String email;

    // Wallet
    private int coins;          // wallet coins
    private int tickets;        // wallet tickets
    private int walletToken;    // 🎮 GAME TOKEN (SHOW IN UI)

    // Push notifications
    private String fcmToken;    // 🔔 FCM TOKEN (DO NOT SHOW IN UI)

    private String profile_image;
    private long created_at;

    // ================= REFERRAL SYSTEM =================
    private String referralCode;
    private String referredBy;
    private boolean referralUsed;

    // Pending referral earnings (CLAIMABLE)
    private long totalReferralCoins;
    private long totalReferralTickets;

    // ================= EMPTY CONSTRUCTOR =================
    // REQUIRED by Firestore
    public UserModel() {
        // Firestore uses reflection
    }

    // ================= FULL CONSTRUCTOR =================
    public UserModel(
            String uid,
            String name,
            String email,
            int coins,
            int tickets,
            int walletToken,
            String fcmToken,
            String profile_image,
            String referralCode,
            String referredBy,
            boolean referralUsed,
            long totalReferralCoins,
            long totalReferralTickets,
            long created_at
    ) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.coins = coins;
        this.tickets = tickets;
        this.walletToken = walletToken;
        this.fcmToken = fcmToken;
        this.profile_image = profile_image;
        this.referralCode = referralCode;
        this.referredBy = referredBy;
        this.referralUsed = referralUsed;
        this.totalReferralCoins = totalReferralCoins;
        this.totalReferralTickets = totalReferralTickets;
        this.created_at = created_at;
    }

    // ================= GETTERS =================

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

    public int getTickets() {
        return tickets;
    }

    // 🎮 Wallet token (for UI)
    public int getWalletToken() {
        return walletToken;
    }

    // 🔔 FCM token (notifications)
    public String getFcmToken() {
        return fcmToken;
    }

    public String getProfileImage() {
        return profile_image;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public String getReferredBy() {
        return referredBy;
    }

    public boolean isReferralUsed() {
        return referralUsed;
    }

    public long getTotalReferralCoins() {
        return totalReferralCoins;
    }

    public long getTotalReferralTickets() {
        return totalReferralTickets;
    }

    public long getCreatedAt() {
        return created_at;
    }

    // ================= SETTERS =================

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

    public void setTickets(int tickets) {
        this.tickets = tickets;
    }

    // 🎮 Wallet token
    public void setWalletToken(int walletToken) {
        this.walletToken = walletToken;
    }

    // 🔔 FCM token
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void setProfileImage(String profile_image) {
        this.profile_image = profile_image;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public void setReferredBy(String referredBy) {
        this.referredBy = referredBy;
    }

    public void setReferralUsed(boolean referralUsed) {
        this.referralUsed = referralUsed;
    }

    public void setTotalReferralCoins(long totalReferralCoins) {
        this.totalReferralCoins = totalReferralCoins;
    }

    public void setTotalReferralTickets(long totalReferralTickets) {
        this.totalReferralTickets = totalReferralTickets;
    }

    public void setCreatedAt(long created_at) {
        this.created_at = created_at;
    }

    // ================= FIRESTORE MAP =================
    // Used when creating / updating user document
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);

        map.put("coins", coins);
        map.put("tickets", tickets);
        map.put("walletToken", walletToken);
        map.put("fcmToken", fcmToken);

        map.put("profile_image", profile_image);

        // Referral
        map.put("referralCode", referralCode);
        map.put("referredBy", referredBy);
        map.put("referralUsed", referralUsed);
        map.put("totalReferralCoins", totalReferralCoins);
        map.put("totalReferralTickets", totalReferralTickets);

        map.put("created_at", created_at);

        return map;
    }
}
