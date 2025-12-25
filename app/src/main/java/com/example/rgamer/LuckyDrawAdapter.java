package com.example.rgamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LuckyDrawAdapter
        extends RecyclerView.Adapter<LuckyDrawAdapter.ViewHolder> {

    public interface Listener {
        void onJoin(LuckyDrawModel model);
        void onCheckWinners(LuckyDrawModel model);
    }

    private final List<LuckyDrawModel> list;
    private final Listener listener;

    public LuckyDrawAdapter(List<LuckyDrawModel> list, Listener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lucky_draw, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder h,
            int position) {

        LuckyDrawModel model = list.get(position);

        h.btnJoin.setOnClickListener(null);

        h.txtReward.setText("Win " + model.getRewardCoins() + " Coins");

        int total = Math.max(model.getTotalSlots(), 1);
        int filled = model.getFilledSlots();
        int percent = (int) ((filled * 100f) / total);

        h.txtSlots.setText("Left : " + filled + "/" + total);
        h.txtPercent.setText(percent + "% Filled");

        h.progressSlots.setMax(total);
        h.progressSlots.setProgress(filled);

        if (model.isJoinedByMe()) {
            h.btnJoin.setText("Joined");
            h.btnJoin.setEnabled(false);

        } else if (model.isFull() || !"OPEN".equals(model.getStatus())) {
            h.btnJoin.setText("FULL");
            h.btnJoin.setEnabled(false);

        } else {
            h.btnJoin.setText("Free Entry");
            h.btnJoin.setEnabled(true);
            h.btnJoin.setOnClickListener(v ->
                    listener.onJoin(model)
            );
        }

        h.btnWinners.setOnClickListener(v ->
                listener.onCheckWinners(model)
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtReward, txtSlots, txtPercent;
        TextView btnJoin, btnWinners;
        ProgressBar progressSlots;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtReward = itemView.findViewById(R.id.txtReward);
            txtSlots = itemView.findViewById(R.id.txtSlots);
            txtPercent = itemView.findViewById(R.id.txtPercent);
            progressSlots = itemView.findViewById(R.id.progressSlots);
            btnJoin = itemView.findViewById(R.id.btnFreeEntry);
            btnWinners = itemView.findViewById(R.id.btnCheckWinners);
        }
    }
}
