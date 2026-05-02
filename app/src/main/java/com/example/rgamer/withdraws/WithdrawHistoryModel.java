package com.example.rgamer.withdraws;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class WithdrawHistoryModel {

    /* ================= FIRESTORE FIELDS ================= */
    private String uid;
    private String type;
    private Object amount;        // String OR Long
    private String status;
    private String voucher_code;
    private Timestamp created_at;

    /* ================= LOCAL ONLY ================= */
    private String request_id;

    /* ================= REQUIRED ================= */
    public WithdrawHistoryModel() {}

    /* ================= GETTERS ================= */

    public String getUid() {
        return uid;
    }

    public String getType() {
        return type != null ? type : "withdraw";
    }

    /** 🔥 SAFE AMOUNT PARSER */
    public long getAmount() {
        if (amount == null) return 0;

        if (amount instanceof Long) {
            return (Long) amount;
        }

        if (amount instanceof String) {
            try {
                return Long.parseLong(
                        ((String) amount)
                                .replace("₹", "")
                                .replace(",", "")
                                .trim()
                );
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }
    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }
    public String getStatus() {
        return status != null ? status : "pending";
    }

    public String getVoucher_code() {
        return voucher_code;
    }

    /** ✅ TIMESTAMP → MILLIS */
    public long getCreatedAt() {
        return created_at != null
                ? created_at.toDate().getTime()
                : 0;
    }

    /** ✅ FORMATTED DATE (OPTIONAL UI) */
    public String getFormattedDate() {
        if (created_at == null) return "";

        try {
            return new java.text.SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    java.util.Locale.ENGLISH
            ).format(created_at.toDate());
        } catch (Exception e) {
            return "";
        }
    }

    public String getRequest_id() {
        return request_id;
    }

    /* ================= SETTERS ================= */

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    /* ================= FIRESTORE MAP ================= */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("type", type);
        map.put("amount", amount);
        map.put("status", getStatus());
        map.put("voucher_code", voucher_code);
        map.put("created_at", created_at); // Timestamp
        return map;
    }
}
