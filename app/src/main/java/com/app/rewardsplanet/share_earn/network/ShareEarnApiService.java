package com.app.rewardsplanet.share_earn.network;

import com.app.rewardsplanet.share_earn.model.ClickTrackingResponse;
import com.app.rewardsplanet.share_earn.model.OfferHistoryResponse;
import com.app.rewardsplanet.share_earn.model.ShareEarnEarningsResponse;
import com.app.rewardsplanet.share_earn.model.ShareEarnOfferDetailResponse;
import com.app.rewardsplanet.share_earn.model.ShareEarnOffersResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ShareEarnApiService {

    @GET("api/v1/offers")
    Call<ShareEarnOffersResponse> getOffers(
            @Query("category") String category,
            @Query("search") String search
    );

    @GET("api/v1/offers/{offerId}")
    Call<ShareEarnOfferDetailResponse> getOfferDetails(
            @Path("offerId") String offerId
    );

    @POST("api/v1/tracking/click")
    Call<ClickTrackingResponse> createTrackingClick(
            @Header("Authorization") String token,
            @Body Map<String, Object> req
    );

    @GET("api/v1/me/offers")
    Call<OfferHistoryResponse> getMyOffers(
            @Header("Authorization") String token,
            @Query("status") String status
    );

    @GET("api/v1/me/earnings")
    Call<ShareEarnEarningsResponse> getMyEarnings(
            @Header("Authorization") String token
    );
}
