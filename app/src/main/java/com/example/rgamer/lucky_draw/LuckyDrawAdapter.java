package com.example.rgamer.lucky_draw;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.LuckyDrawModel;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LuckyDrawAdapter
        extends RecyclerView.Adapter<LuckyDrawAdapter.ViewHolder> {

    public interface Listener {
        void onJoin(LuckyDrawModel model);
        void onJoinWithTickets(LuckyDrawModel model);
        void onCheckWinners(LuckyDrawModel model);
    }

    private final List<LuckyDrawModel> list;
    private final Listener listener;

    private final Set<String> loadingIds = new HashSet<>();

    public LuckyDrawAdapter(List<LuckyDrawModel> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lucky_draw, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        LuckyDrawModel model = list.get(position);
        String id = model.getId();

        int total = Math.max(model.getTotalSlots(), 1);
        int filled = model.getFilledSlots();

        h.txtReward.setText("Win " + model.getRewardCoins() + " Coins 🎉");
        h.txtSlots.setText("Filled : " + filled + "/" + total);
        h.txtPercent.setText((filled * 100 / total) + "% Filled");

        h.progressSlots.setMax(total);
        h.progressSlots.setProgress(filled);

        boolean loading = loadingIds.contains(id);

        if (loading) {
            h.btnJoin.setText("Joining...");
            h.btnJoin.setEnabled(false);
            h.btnticket.setEnabled(false);
            return;
        }

        if (model.isJoinedByMe()) {

            h.btnJoin.setText("Joined");
            h.btnJoin.setEnabled(false);
            h.btnticket.setEnabled(false);

            h.btnJoin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#2E7D32")));

        } else if (model.isFull() || !"OPEN".equals(model.getStatus())) {

            h.btnJoin.setText("FULL");
            h.btnJoin.setEnabled(false);
            h.btnticket.setEnabled(false);

            h.btnJoin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#D32F2F")));

        } else {

            h.btnJoin.setText("Free Entry");
            h.btnJoin.setEnabled(true);
            h.btnticket.setEnabled(true);

            h.btnJoin.setOnClickListener(v -> {
                loadingIds.add(id);
                notifyItemChanged(position);
                listener.onJoin(model);
            });

            h.btnticket.setOnClickListener(v -> {
                loadingIds.add(id);
                notifyItemChanged(position);
                listener.onJoinWithTickets(model);
            });
        }

        h.btnticket.setOnLongClickListener(v -> {
            listener.onCheckWinners(model);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void clearLoading(String drawId) {
        loadingIds.remove(drawId);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtReward, txtSlots, txtPercent;
        ProgressBar progressSlots;
        MaterialButton btnJoin, btnticket;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtReward = itemView.findViewById(R.id.txtReward);
            txtSlots = itemView.findViewById(R.id.txtSlots);
            txtPercent = itemView.findViewById(R.id.txtPercent);
            progressSlots = itemView.findViewById(R.id.progressSlots);

            btnJoin = itemView.findViewById(R.id.btnFreeEntry);
            btnticket = itemView.findViewById(R.id.btnticketEntry);
        }
    }
}