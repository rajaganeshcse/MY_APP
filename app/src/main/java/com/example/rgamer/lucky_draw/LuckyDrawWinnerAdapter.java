package com.example.rgamer.lucky_draw;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.LuckyDrawModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuckyDrawWinnerAdapter
        extends RecyclerView.Adapter<LuckyDrawWinnerAdapter.VH> {

    private final List<LuckyDrawModel> list;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, String> userCache = new HashMap<>();

    public LuckyDrawWinnerAdapter(List<LuckyDrawModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lucky_draw_winner, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {

        LuckyDrawModel model = list.get(position);

        h.txtReward.setText("🏆 " + model.getRewardCoins() + " Coins");
        h.txtWinner.setText("Loading...");

        String uid = model.getWinnerUid();

        // ✅ Null safety
        if (uid == null || uid.isEmpty()) {
            h.txtWinner.setText("Winner: Unknown");
        } else if (userCache.containsKey(uid)) {
            h.txtWinner.setText("Winner: " + userCache.get(uid));
        } else {
            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        String name = "Unknown";

                        if (snapshot.exists()) {
                            String n = snapshot.getString("name");
                            if (n != null && !n.isEmpty()) {
                                name = n;
                            }
                        }

                        userCache.put(uid, name);

                        int adapterPos = h.getBindingAdapterPosition();
                        if (adapterPos != RecyclerView.NO_POSITION && adapterPos == position) {
                            h.txtWinner.setText("Winner: " + name);
                        }
                    });
        }

        h.txtCompletedAt.setText("Completed at: " + model.getCompletedAtFormatted());

        String status = model.getStatus();
        h.txtStatus.setText("COMPLETED");

        if ("CLOSED".equalsIgnoreCase(status)) {
            h.txtStatus.setBackgroundResource(R.drawable.bg_status_success);
        } else {
            h.txtStatus.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView txtReward, txtWinner, txtCompletedAt, txtStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            txtReward = itemView.findViewById(R.id.txtReward);
            txtWinner = itemView.findViewById(R.id.txtWinner);
            txtCompletedAt = itemView.findViewById(R.id.txtCompletedAt);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}