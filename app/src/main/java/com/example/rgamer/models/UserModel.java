package com.example.rgamer.models;

import java.util.HashMap;
import java.util.Map;

public class UserModel {

    /* ================= BASIC INFO ================= */
    private String uid;
    private String name;
    private String email;

    /* ================= WALLET ================= */
    private int coins;          // Wallet coins
    private int tickets;        // Wallet tickets
    private int walletToken;    // 🎮 Game token (UI)

    /* ================= NOTIFICATIONS ================= */
    private String fcmToken;    // 🔔 Push notifications

    /* ================= PROFILE ================= */
    private String profile_image;   // Google profile image URL
    private long created_at;

    /* ================= DAILY BONUS ================= */
    // yyyy-MM-dd (prevents multiple claims per day)
    private String dailyBonusClaimedDate;

    /* ================= REFERRAL SYSTEM ================= */
    private String referralCode;
    private String referredBy;
    private boolean referralUsed;

    // Pending referral earnings
    private long totalReferralCoins;
    private long totalReferralTickets;

    /* ================= EMPTY CONSTRUCTOR ================= */
    // REQUIRED by Firestore
    public UserModel() {}

    /* ================= FULL CONSTRUCTOR ================= */
    public UserModel(
            String uid,
            String name,
            String email,
            int coins,
            int tickets,
            int walletToken,
            String fcmToken,
            String profile_image,
            String dailyBonusClaimedDate,
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
        this.dailyBonusClaimedDate = dailyBonusClaimedDate;
        this.referralCode = referralCode;
        this.referredBy = referredBy;
        this.referralUsed = referralUsed;
        this.totalReferralCoins = totalReferralCoins;
        this.totalReferralTickets = totalReferralTickets;
        this.created_at = created_at;
    }

    /* ================= GETTERS ================= */

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public int getCoins() { return coins; }
    public int getTickets() { return tickets; }
    public int getWalletToken() { return walletToken; }

    public String getFcmToken() { return fcmToken; }
    public String getProfileImage() { return profile_image; }

    public String getDailyBonusClaimedDate() {
        return dailyBonusClaimedDate;
    }

    public String getReferralCode() { return referralCode; }
    public String getReferredBy() { return referredBy; }
    public boolean isReferralUsed() { return referralUsed; }

    public long getTotalReferralCoins() { return totalReferralCoins; }
    public long getTotalReferralTickets() { return totalReferralTickets; }

    public long getCreatedAt() { return created_at; }

    /* ================= SETTERS ================= */

    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }

    public void setCoins(int coins) { this.coins = coins; }
    public void setTickets(int tickets) { this.tickets = tickets; }
    public void setWalletToken(int walletToken) { this.walletToken = walletToken; }

    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setProfileImage(String profile_image) { this.profile_image = profile_image; }

    public void setDailyBonusClaimedDate(String date) {
        this.dailyBonusClaimedDate = date;
    }

    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }
    public void setReferralUsed(boolean referralUsed) { this.referralUsed = referralUsed; }

    public void setTotalReferralCoins(long totalReferralCoins) {
        this.totalReferralCoins = totalReferralCoins;
    }

    public void setTotalReferralTickets(long totalReferralTickets) {
        this.totalReferralTickets = totalReferralTickets;
    }

    public void setCreatedAt(long created_at) { this.created_at = created_at; }

    /* ================= FIRESTORE MAP ================= */
    // Used when creating / updating Firestore user document
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        // Basic
        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);

        // Wallet
        map.put("coins", coins);
        map.put("tickets", tickets);
        map.put("walletToken", walletToken);

        // Notifications
        map.put("fcmToken", fcmToken);

        // Profile
        map.put("profile_image", profile_image);

        // Daily bonus (nested)
        map.put("daily_bonus.claimed_date", dailyBonusClaimedDate);

        // Referral
        map.put("referralCode", referralCode);
        map.put("referredBy", referredBy);
        map.put("referralUsed", referralUsed);
        map.put("totalReferralCoins", totalReferralCoins);
        map.put("totalReferralTickets", totalReferralTickets);

        // Metadata
        map.put("created_at", created_at);

        return map;
    }
}
