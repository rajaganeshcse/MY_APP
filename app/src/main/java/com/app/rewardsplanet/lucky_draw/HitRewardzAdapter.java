package com.app.rewardsplanet.lucky_draw;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.models.HitRewardzModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class HitRewardzAdapter extends RecyclerView.Adapter<HitRewardzAdapter.ViewHolder> {

    public interface OnHitzClickListener {
        void onHitzClick(HitRewardzModel item);
    }

    private final List<HitRewardzModel> list;
    private final OnHitzClickListener listener;

    public HitRewardzAdapter(List<HitRewardzModel> list, OnHitzClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hit_rewardz, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HitRewardzModel item = list.get(position);

        holder.txtTitle.setText(item.getTitle());
        holder.txtCoins.setText("+" + item.getCoins() + " Coins");
        holder.txtDuration.setText(item.getDurationText());

        if (item.isCompleted()) {
            holder.btnWatch.setText("COMPLETED ✅");
            holder.btnWatch.setEnabled(false);
            holder.btnWatch.setBackgroundColor(Color.parseColor("#332A52"));
            holder.btnWatch.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            holder.btnWatch.setText("WATCH AD ⚡");
            holder.btnWatch.setEnabled(true);
            holder.btnWatch.setBackgroundResource(R.drawable.bg_btn_quiz_watch_ad);
            holder.btnWatch.setTextColor(Color.WHITE);

            holder.btnWatch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHitzClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtCoins, txtDuration;
        MaterialButton btnWatch;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtHitzTitle);
            txtCoins = itemView.findViewById(R.id.txtHitzRewardCoins);
            txtDuration = itemView.findViewById(R.id.txtHitzDuration);
            btnWatch = itemView.findViewById(R.id.btnWatchHitzItem);
        }
    }
}
