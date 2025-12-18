package com.example.rgamer.models;

import java.util.HashMap;
import java.util.Map;

public class UserModel {

    /* ================= BASIC INFO ================= */
    private String uid;
    private String name;
    private String email;

    /* ================= WALLET ================= */
    private long coins;         // ✅ FIXED (was int)
    private int tickets;
    private int walletToken;

    /* ================= NOTIFICATIONS ================= */
    private String fcmToken;

    /* ================= PROFILE ================= */
    private String profile_image;
    private long created_at;

    /* ================= DAILY BONUS ================= */
    private String dailyBonusClaimedDate;

    /* ================= REFERRAL SYSTEM ================= */
    private String referralCode;
    private String referredBy;
    private boolean referralUsed;

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
            long coins,              // ✅ FIXED
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

    public long getCoins() { return coins; }          // ✅ FIXED
    public int getTickets() { return tickets; }
    public int getWalletToken() { return walletToken; }

    public String getFcmToken() { return fcmToken; }
    public String getProfileImage() { return profile_image; }
    public String getDailyBonusClaimedDate() { return dailyBonusClaimedDate; }

    public String getReferralCode() { return referralCode; }
    public String getReferredBy() { return referredBy; }
    public boolean isReferralUsed() { return referralUsed; }

    public long getTotalReferralCoins() { return totalReferralCoins; }
    public long getTotalReferralTickets() { return totalReferralTickets; }

    public long getCreatedAt() { return created_at; }

    /* ================= SETTERS ================= */
    public void setCoins(long coins) { this.coins = coins; }   // ✅ FIXED

    /* ================= FIRESTORE MAP ================= */
    public Map<String, Object> toMap() {

        Map<String, Object> map = new HashMap<>();

        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);

        map.put("coins", coins);              // ✅ long
        map.put("tickets", tickets);
        map.put("walletToken", walletToken);

        map.put("fcmToken", fcmToken);
        map.put("profile_image", profile_image);

        map.put("dailyBonusClaimedDate", dailyBonusClaimedDate);

        map.put("referralCode", referralCode);
        map.put("referredBy", referredBy);
        map.put("referralUsed", referralUsed);
        map.put("totalReferralCoins", totalReferralCoins);
        map.put("totalReferralTickets", totalReferralTickets);

        map.put("created_at", created_at);

        return map;
    }
}
