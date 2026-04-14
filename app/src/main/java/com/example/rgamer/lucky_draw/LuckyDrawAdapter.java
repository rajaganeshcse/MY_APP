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

import java.util.List;

public class LuckyDrawAdapter
        extends RecyclerView.Adapter<LuckyDrawAdapter.ViewHolder> {

    /* ================= CALLBACK ================= */

    public interface Listener {
        void onJoin(LuckyDrawModel model);      // join draw
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

        // reset
        h.btnJoin.setOnClickListener(null);

        /* ================= TEXT ================= */

        h.txtReward.setText("Win " + model.getRewardCoins() + " Coins");

        int total = Math.max(model.getTotalSlots(), 1);
        int filled = model.getFilledSlots();
        int percent = (int) ((filled * 100f) / total);

        h.txtSlots.setText("Left : " + filled + "/" + total);
        h.txtPercent.setText(percent + "% Filled");

        h.progressSlots.setMax(total);
        h.progressSlots.setProgress(filled);

        /* ================= BUTTON STATE ================= */

        if (model.isJoinedByMe()) {

            // 🟢 JOINED
            h.btnJoin.setText("Joined");
            h.btnJoin.setEnabled(false);
            h.btnJoin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            );
            h.btnJoin.setTextColor(Color.WHITE);

        } else if (model.isFull() || !"OPEN".equals(model.getStatus())) {

            // 🔴 FULL / CLOSED
            h.btnJoin.setText("FULL");
            h.btnJoin.setEnabled(false);
            h.btnJoin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#D32F2F"))
            );
            h.btnJoin.setTextColor(Color.WHITE);

        } else {

            // 🔵 FREE ENTRY
            h.btnJoin.setText("Get Free Entry");
            h.btnJoin.setEnabled(true);
            h.btnJoin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#6A1BFF"))
            );
            h.btnJoin.setTextColor(Color.WHITE);

            h.btnJoin.setOnClickListener(v ->
                    listener.onJoin(model)
            );
        }

        /* ================= WINNERS ================= */

        h.btnticket.setOnClickListener(v ->
                listener.onCheckWinners(model)
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /* ================= VIEW HOLDER ================= */

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtReward, txtSlots, txtPercent;
        MaterialButton btnJoin, btnticket;
        ProgressBar progressSlots;

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
