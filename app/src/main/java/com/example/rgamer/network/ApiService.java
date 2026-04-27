package com.example.rgamer.network;

import android.util.Log;

import com.example.rgamer.RedeemResponse;
import com.example.rgamer.models.SpinResponse;
import com.example.rgamer.models.UserModel;
import com.example.rgamer.models.DrawResponse;
import com.example.rgamer.models.JoinResponse;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    /* ================= AUTH ================= */

    @POST("api/auth")
    Call<ResponseBody> auth(@Body LoginRequest request);

    @GET("api/user")
    Call<UserModel> getUser(@Header("Authorization") String token);

    /* ================= ADS ================= */

    @POST("api/reward-ad")
    Call<ResponseBody> rewardAd(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );

    /* ================= SPIN ================= */

    @POST("api/spin")
    Call<SpinResponse> spin(
            @Header("Authorization") String token,
            @Query("userId") String userId
    );

    @GET("api/spin-status")
    Call<SpinResponse> spinStatus(
            @Query("userId") String userId
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
    //withdraw

    //reedem
    @POST("/api/withdraw/request")
    Call<RedeemResponse> redeemRequest(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );
}