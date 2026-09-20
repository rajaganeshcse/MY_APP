package com.app.rewardsplanet.lucky_draw;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.models.WatchVideoModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class WatchVideoAdapter extends RecyclerView.Adapter<WatchVideoAdapter.ViewHolder> {

    public interface OnWatchClickListener {
        void onWatchClick(WatchVideoModel item, int position);
    }

    private final List<WatchVideoModel> list;
    private final OnWatchClickListener listener;

    public WatchVideoAdapter(List<WatchVideoModel> list, OnWatchClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_watch_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WatchVideoModel model = list.get(position);

        holder.txtVideoTitle.setText(model.getTitle());
        holder.txtVideoRewardCoins.setText("+" + model.getCoinReward() + " Coins");
        holder.txtVideoRewardTickets.setText("🎟️ +" + model.getTicketReward() + " Ticket" + (model.getTicketReward() > 1 ? "s" : ""));

        // SUPER BONUS BADGE FOR ITEM 10
        if (model.getId() == 10) {
            holder.badgeSuperBonus.setVisibility(View.VISIBLE);
            if (holder.cardRoot != null) {
                holder.cardRoot.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
                holder.cardRoot.setStrokeWidth(4);
            }
        } else {
            holder.badgeSuperBonus.setVisibility(View.GONE);
            if (holder.cardRoot != null) {
                int strokeColor;
                if (model.getId() <= 3) {
                    strokeColor = Color.parseColor("#2E2E4A");
                } else if (model.getId() <= 5) {
                    strokeColor = Color.parseColor("#4C1D95");
                } else if (model.getId() <= 7) {
                    strokeColor = Color.parseColor("#BE185D");
                } else {
                    strokeColor = Color.parseColor("#B45309");
                }
                holder.cardRoot.setStrokeColor(ColorStateList.valueOf(strokeColor));
                holder.cardRoot.setStrokeWidth(2);
            }
        }

        if (model.isCompleted()) {
            holder.btnWatchVideoItem.setText("✓ Claimed");
            holder.btnWatchVideoItem.setEnabled(false);
            holder.btnWatchVideoItem.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334155")));
            holder.btnWatchVideoItem.setTextColor(Color.parseColor("#94A3B8"));
        } else if (model.isLoading()) {
            holder.btnWatchVideoItem.setText("Loading...");
            holder.btnWatchVideoItem.setEnabled(false);
            holder.btnWatchVideoItem.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#64748B")));
            holder.btnWatchVideoItem.setTextColor(Color.WHITE);
        } else {
            holder.btnWatchVideoItem.setText("Watch 📺");
            holder.btnWatchVideoItem.setEnabled(true);
            holder.btnWatchVideoItem.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
            holder.btnWatchVideoItem.setTextColor(Color.WHITE);
        }

        holder.btnWatchVideoItem.setOnClickListener(v -> {
            if (!model.isCompleted() && !model.isLoading() && listener != null) {
                listener.onWatchClick(model, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView txtVideoTitle, txtVideoRewardCoins, txtVideoRewardTickets, badgeSuperBonus;
        MaterialButton btnWatchVideoItem;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardWatchVideoRoot);
            txtVideoTitle = itemView.findViewById(R.id.txtVideoTitle);
            txtVideoRewardCoins = itemView.findViewById(R.id.txtVideoRewardCoins);
            txtVideoRewardTickets = itemView.findViewById(R.id.txtVideoRewardTickets);
            badgeSuperBonus = itemView.findViewById(R.id.badgeSuperBonus);
            btnWatchVideoItem = itemView.findViewById(R.id.btnWatchVideoItem);
        }
    }
}
