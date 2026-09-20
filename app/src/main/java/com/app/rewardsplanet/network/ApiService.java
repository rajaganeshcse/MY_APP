package com.app.rewardsplanet.network;

import com.app.rewardsplanet.models.ScratchResponse;
import com.app.rewardsplanet.RedeemResponse;
import com.app.rewardsplanet.models.SpinResponse;
import com.app.rewardsplanet.models.UserModel;
import com.app.rewardsplanet.models.DrawResponse;
import com.app.rewardsplanet.models.JoinResponse;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    /* ================= SCRATCH ================= */

    @POST("api/scratch/play")
    Call<ScratchResponse> playScratch(
            @Header("Authorization") String token
    );

    @GET("api/scratch/status")
    Call<ScratchResponse> getScratchStatus(
            @Header("Authorization") String token
    );

    /* ================= STREAK ================= */

    @POST("api/claim-streak")
    Call<ResponseBody> claimStreak(
            @Header("Authorization") String token
    );

    @GET("api/streak-status")
    Call<ResponseBody> getStreakStatus(
            @Header("Authorization") String token
    );

    /* ================= AUTH ================= */

    @POST("api/auth")
    Call<ResponseBody> auth(
            @Body LoginRequest request
    );

    @GET("api/user")
    Call<UserModel> getUser(
            @Header("Authorization") String token
    );

    /* ================= ADS ================= */

    @POST("api/reward-ad")
    Call<ResponseBody> rewardAd(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    @GET("api/hitz-rewards/config")
    Call<ResponseBody> getHitzRewardsConfig(
            @Header("Authorization") String token
    );

    /* ================= DAILY BONUS ================= */

    @POST("/api/daily-bonus")
    Call<ResponseBody> claimDailyBonus(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    /* ================= SPIN ================= */

    @POST("api/spin")
    Call<SpinResponse> spin(
            @Header("Authorization") String token
    );

    @GET("api/spin-status")
    Call<SpinResponse> spinStatus(
            @Header("Authorization") String token
    );

    /* ================= LUCKY DRAW ================= */

    @POST("/api/draw/join")
    Call<JoinResponse> joinDraw(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @GET("api/draw/status")
    Call<DrawResponse> drawStatus(
            @Header("Authorization") String token,
            @Query("drawId") String drawId
    );

    @GET("api/draw/winner")
    Call<DrawResponse> getWinner(
            @Header("Authorization") String token,
            @Query("drawId") String drawId
    );

    /* ================= WITHDRAW / REDEEM ================= */

    @POST("/api/withdraw/request")
    Call<RedeemResponse> redeemRequest(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @GET("api/reedem1")
    Call<ResponseBody> getreedem1(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    /* ================= DELETE ACCOUNT ================= */

    @POST("api/account/delete-request")
    Call<ResponseBody> requestDeleteAccount(
            @Body Map<String, String> body
    );

    @POST("api/account/cancel-delete")
    Call<ResponseBody> cancelDeleteAccount(
            @Body Map<String, String> body
    );

    /* ================= REFERRAL ================= */

    @GET("api/referral/code")
    Call<ResponseBody> getReferralCode(
            @Header("Authorization") String token
    );

    @POST("api/referral/apply")
    Call<ResponseBody> applyReferralCode(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );
}