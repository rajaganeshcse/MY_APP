package com.example.rgamer.lucky_draw;

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

import retrofit2.*;

public class activity_lucky_draw extends AppCompatActivity
        implements LuckyDrawAdapter.Listener {

    private FirebaseFirestore db;
    private ApiService api;

    private final List<LuckyDrawModel> list = new ArrayList<>();
    private LuckyDrawAdapter adapter;

    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        db = FirebaseFirestore.getInstance();
        api = ApiClient.getClient().create(ApiService.class);

        uid = FirebaseAuth.getInstance().getUid();

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

                        // 🔥 detect if user used FREE entry
                        checkIfJoined(m);

                        list.add(m);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void checkIfJoined(LuckyDrawModel model) {

        db.collection("lucky_draw_tickets")
                .document(model.getId())
                .collection("tickets")
                .whereEqualTo("uid", uid)
                .whereEqualTo("type", "AD")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {
                        model.setJoinedByMe(true);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onJoin(LuckyDrawModel model) {
        showAdAndJoin(model.getId());
    }

    @Override
    public void onJoinWithTickets(LuckyDrawModel model) {
        join(model.getId(), "TICKET");
    }

    private void showAdAndJoin(String drawId) {
        // 👉 integrate rewarded ad here
        join(drawId, "AD");
    }

    private void join(String drawId, String type) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter.setLoading(drawId, true);

        Log.d("JOIN", "drawId=" + drawId + " type=" + type);

        FirebaseAuth.getInstance().getCurrentUser()
                .getIdToken(true)
                .addOnSuccessListener(result -> {

                    String token = result.getToken();

                    Map<String, Object> body = new HashMap<>();
                    body.put("drawId", drawId);
                    body.put("type", type); // ✅ correct

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

                                        Toast.makeText(
                                                activity_lucky_draw.this,
                                                "Join failed",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JoinResponse> call, Throwable t) {

                                    adapter.clearLoading(drawId);

                                    Log.e("API", "Error: " + t.getMessage());

                                    Toast.makeText(
                                            activity_lucky_draw.this,
                                            t.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                });
    }

    @Override
    public void onCheckWinners(LuckyDrawModel model) {
        Toast.makeText(this, "Winner screen coming soon", Toast.LENGTH_SHORT).show();
    }
}