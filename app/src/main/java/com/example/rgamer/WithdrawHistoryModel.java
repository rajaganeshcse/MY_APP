package com.example.rgamer;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class WithdrawHistoryModel {

    /* ================= FIRESTORE FIELDS ================= */
    private String uid;
    private String type;
    private String amount;
    private String status;
    private String voucher_code;
    private Timestamp created_at;

    /* ================= LOCAL ONLY ================= */
    private String request_id;

    /* ================= REQUIRED ================= */
    public WithdrawHistoryModel() {}

    /* ================= FULL CONSTRUCTOR ================= */
    public WithdrawHistoryModel(
            String uid,
            String type,
            String amount,
            String status,
            String voucher_code,
            Timestamp created_at
    ) {
        this.uid = uid;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.voucher_code = voucher_code;
        this.created_at = created_at;
    }

    /* ================= GETTERS ================= */
    public String getUid() {
        return uid;
    }

    public String getType() {
        return type;
    }

    public String getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getVoucher_code() {
        return voucher_code;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public String getRequest_id() {
        return request_id;
    }

    /* ================= SETTERS ================= */
    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    // 🔥🔥🔥 THIS WAS MISSING (MAIN FIX)
    public void setType(String type) {
        this.type = type;
    }

    /* ================= HELPERS ================= */
    public long getCreatedAtMillis() {
        return created_at != null
                ? created_at.toDate().getTime()
                : 0;
    }

    /* ================= FIRESTORE MAP ================= */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("type", type);
        map.put("amount", amount);
        map.put("status", status);
        map.put("voucher_code", voucher_code);
        map.put("created_at", created_at);
        return map;
    }
}
