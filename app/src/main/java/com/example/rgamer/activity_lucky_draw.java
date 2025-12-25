package com.example.rgamer;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class activity_lucky_draw extends AppCompatActivity
        implements LuckyDrawAdapter.Listener {

    FirebaseFirestore db;
    String uid;

    RecyclerView recyclerView;
    LuckyDrawAdapter adapter;
    List<LuckyDrawModel> list = new ArrayList<>();

    ListenerRegistration drawListener;
    ListenerRegistration joinedListener;

    Set<String> joinedDrawIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.luckyDrawRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LuckyDrawAdapter(list, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenJoinedDraws();
        loadLuckyDraws();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (drawListener != null) drawListener.remove();
        if (joinedListener != null) joinedListener.remove();
    }

    /* ================= JOINED DRAWS (SAFE) ================= */

    private void listenJoinedDraws() {

        if (uid == null) return;

        joinedListener = db.collection("users")
                .document(uid)
                .collection("joinedLuckyDraws")
                .addSnapshotListener((snap, e) -> {

                    if (snap == null || e != null) return;

                    joinedDrawIds.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        joinedDrawIds.add(d.getId());
                    }

                    applyJoinedState();
                });
    }

    /* ================= READ DRAWS ================= */

    private void loadLuckyDraws() {

        drawListener = db.collection("lucky_draws")
                .whereEqualTo("status", "OPEN")
                .addSnapshotListener((snap, e) -> {

                    if (snap == null || e != null) return;

                    list.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        LuckyDrawModel model =
                                doc.toObject(LuckyDrawModel.class);

                        if (model == null) continue;

                        model.setId(doc.getId());
                        model.setJoinedByMe(
                                joinedDrawIds.contains(doc.getId())
                        );

                        list.add(model);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void applyJoinedState() {
        for (LuckyDrawModel m : list) {
            m.setJoinedByMe(joinedDrawIds.contains(m.getId()));
        }
        adapter.notifyDataSetChanged();
    }

    /* ================= ADAPTER CALLBACKS ================= */

    @Override
    public void onJoin(LuckyDrawModel model) {
        joinDraw(model.getId());
    }

    @Override
    public void onCheckWinners(LuckyDrawModel model) {
        Toast.makeText(
                this,
                "Winner will be announced after draw completes",
                Toast.LENGTH_SHORT
        ).show();
    }

    /* ================= SAFE JOIN ================= */

    private void joinDraw(String drawId) {

        DocumentReference drawRef =
                db.collection("lucky_draws").document(drawId);

        DocumentReference entryRef =
                db.collection("lucky_draw_entries")
                        .document(drawId)
                        .collection("users")
                        .document(uid);

        DocumentReference userJoinRef =
                db.collection("users")
                        .document(uid)
                        .collection("joinedLuckyDraws")
                        .document(drawId);

        db.runTransaction(transaction -> {

            DocumentSnapshot drawSnap = transaction.get(drawRef);

            long filled = drawSnap.getLong("filledSlots");
            long total = drawSnap.getLong("totalSlots");
            String status = drawSnap.getString("status");

            if (!"OPEN".equals(status))
                throw new RuntimeException("Draw closed");

            if (filled >= total)
                throw new RuntimeException("Draw full");

            if (transaction.get(entryRef).exists())
                throw new RuntimeException("Already joined");

            Map<String, Object> data = new HashMap<>();
            data.put("joinedAt", FieldValue.serverTimestamp());

            transaction.set(entryRef, data);
            transaction.set(userJoinRef, data);
            transaction.update(drawRef,
                    "filledSlots", FieldValue.increment(1));

            return null;

        }).addOnSuccessListener(v ->
                Toast.makeText(this,
                        "Joined Lucky Draw",
                        Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this,
                        e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }
}
