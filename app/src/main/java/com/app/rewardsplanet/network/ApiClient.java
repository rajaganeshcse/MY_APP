package com.app.rewardsplanet.network;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String TAG      = "ApiClient";
    private static final String BASE_URL = "https://app-backend-lutn.onrender.com/";

    public static Retrofit getClient() {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                // ────────────────────────────────────────────────────────────────
                // RAW-RESPONSE INTERCEPTOR
                // Logs the exact JSON the server returns for every spin/scratch
                // request so you can verify field names without guessing.
                // Remove or disable in production builds if preferred.
                // ────────────────────────────────────────────────────────────────
                .addInterceptor(chain -> {
                    Request  request  = chain.request();
                    Response response = chain.proceed(request);

                    // Only log spin and scratch endpoints (avoid logging auth tokens)
                    String url = request.url().toString();
                    if (url.contains("/api/spin") || url.contains("/api/scratch")) {
                        try {
                            ResponseBody body = response.body();
                            if (body != null) {
                                BufferedSource source = body.source();
                                source.request(Long.MAX_VALUE);          // buffer entire body
                                Buffer buffer = source.getBuffer().clone();
                                String rawJson = buffer.readUtf8();
                                Log.d(TAG, "RESPONSE [" + url + "] → " + rawJson);

                                // Rebuild response so Retrofit can still parse it
                                response = response.newBuilder()
                                        .body(ResponseBody.create(
                                                body.contentType(),
                                                rawJson.getBytes()))
                                        .build();
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Could not log response body: " + e.getMessage());
                        }
                    }
                    return response;
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}