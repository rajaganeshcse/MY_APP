package com.app.rewardsplanet.share_earn.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ShareEarnOffersResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<ShareEarnOffer> data;

    public boolean isSuccess() { return success; }
    public List<ShareEarnOffer> getData() { return data; }
}
