package com.example.rgamer.network;

import com.example.rgamer.models.SpinResponse;
import com.example.rgamer.models.UserModel;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("api/auth")
    Call<ResponseBody> auth(@Body LoginRequest request);

    @GET("api/user")
    Call<UserModel> getUser(@Header("Authorization") String token);

    @POST("api/reward-ad")
    Call<ResponseBody> rewardAd(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );
    @POST("api/spin")
    Call<SpinResponse> spin(
            @Header("Authorization") String token,
            @Query("userId") String userId
    );

    @GET("api/spin-status")
    Call<SpinResponse> spinStatus(@Query("userId") String userId);

}