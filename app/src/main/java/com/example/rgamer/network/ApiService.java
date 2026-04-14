package com.example.rgamer.network;

import com.example.rgamer.models.UserModel;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("api/auth")   // ✅ REMOVE "/"
    Call<ResponseBody> auth(@Body LoginRequest request);

    @GET("api/user")   // ✅ REMOVE "/"
    Call<UserModel> getUser(@Header("Authorization") String token);

    @POST("api/reward-ad")
    Call<ResponseBody> rewardAd(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );
}