package com.app.rewardsplanet.share_earn.model;

import com.google.gson.annotations.SerializedName;

public class ClaimOfferResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("conversionId")
    private String conversionId;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("code")
    private String code;

    public boolean isSuccess() { return success; }
    public String getConversionId() { return conversionId; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getCode() { return code; }
}
