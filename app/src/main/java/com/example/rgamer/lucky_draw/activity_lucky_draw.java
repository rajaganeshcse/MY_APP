package com.example.rgamer.lucky_draw;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.JoinResponse;
import com.example.rgamer.models.LuckyDrawModel;
import com.example.rgamer.network.ApiClient;
import com.example.rgamer.network.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

import okhttp3.ResponseBody;
import retrofit2.*;

public class activity_lucky_draw extends AppCompatActivity
        implements LuckyDrawAdapter.Listener {

    FirebaseFirestore db;
    ApiService api;

    List<LuckyDrawModel> list = new ArrayList<>();
    LuckyDrawAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        db = FirebaseFirestore.getInstance();
        api = ApiClient.getClient().create(ApiService.class);

        RecyclerView rv = findViewById(R.id.luckyDrawRecycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LuckyDrawAdapter(list, this);
        rv.setAdapter(adapter);

        loadDraws();
    }

    private void loadDraws() {
        db.collection("lucky_draws")
                .whereEqualTo("status", "OPEN")
                .addSnapshotListener((snap, e) -> {

                    if (snap == null) return;

                    list.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        LuckyDrawModel m = d.toObject(LuckyDrawModel.class);
                        if (m == null) continue;

                        m.setId(d.getId());
                        list.add(m);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onJoin(LuckyDrawModel model) {
        join(model.getId(), 1);
    }

    @Override
    public void onJoinWithTickets(LuckyDrawModel model) {
        join(model.getId(), 2);
    }

    private void join(String drawId, int tickets) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().getCurrentUser()
                .getIdToken(true)
                .addOnSuccessListener(result -> {

                    String token = result.getToken();

                    Map<String, Object> body = new HashMap<>();
                    body.put("drawId", drawId);
                    body.put("ticketCount", tickets);
                    api.joinDraw("Bearer " + token, body)
                            .enqueue(new Callback<JoinResponse>() {

                                @Override
                                public void onResponse(Call<JoinResponse> call,
                                                       Response<JoinResponse> response) {

                                    adapter.clearLoading(drawId);

                                    Log.d("API", "Code: " + response.code());

                                    if (response.isSuccessful() && response.body() != null) {

                                        Toast.makeText(
                                                activity_lucky_draw.this,
                                                response.body().message,
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    } else {

                                        try {
                                            String err = response.errorBody().string();
                                            Log.e("API", err);

                                            Toast.makeText(
                                                    activity_lucky_draw.this,
                                                    err,
                                                    Toast.LENGTH_LONG
                                            ).show();

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<JoinResponse> call, Throwable t) {

                                    adapter.clearLoading(drawId);

                                    Log.e("API", "Fail: " + t.getMessage());

                                    Toast.makeText(
                                            activity_lucky_draw.this,
                                            "Network: " + t.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                });
    }

    @Override
    public void onCheckWinners(LuckyDrawModel model) {
        startActivity(new Intent(this, activity_lucky_draw_winner.class));
    }
}