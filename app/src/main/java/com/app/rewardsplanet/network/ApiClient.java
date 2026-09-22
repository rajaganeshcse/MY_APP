package com.app.rewardsplanet.network;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
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
    private static volatile Retrofit sRetrofit = null;

    public static synchronized Retrofit getClient() {
        if (sRetrofit != null) {
            return sRetrofit;
        }

        // Custom Gson with Firebase Timestamp Deserializer to prevent Retrofit parsing crashes
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Timestamp.class, (JsonDeserializer<Timestamp>) (json, typeOfT, context) -> {
                    if (json != null && json.isJsonObject()) {
                        JsonObject obj = json.getAsJsonObject();
                        long seconds = obj.has("seconds") ? obj.get("seconds").getAsLong() : 0;
                        int nanos = obj.has("nanos") ? obj.get("nanos").getAsInt() : 0;
                        return new Timestamp(seconds, nanos);
                    }
                    return null;
                })
                .create();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request  request  = chain.request();
                    Response response = chain.proceed(request);

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

        sRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return sRetrofit;
    }
}