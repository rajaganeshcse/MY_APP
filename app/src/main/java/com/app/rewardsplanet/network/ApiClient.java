package com.app.rewardsplanet.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // ✅ FIXED (remove baseUrl())
    private static final String BASE_URL = "https://app-backend-lutn.onrender.com/";

    public static Retrofit getClient() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)   // ✅ correct usage
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}