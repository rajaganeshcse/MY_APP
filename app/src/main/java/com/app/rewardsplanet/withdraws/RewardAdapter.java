package com.app.rewardsplanet.withdraws;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CoinModel model);
    }

    private List<ListItem> list;
    private OnItemClickListener listener;

    public RewardAdapter(List<ListItem> list) {
        this.list = list;
    }

    public RewardAdapter(List<ListItem> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).type;
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reward, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = list.get(position);

        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).headerText.setText(item.header);
        } else if (holder instanceof ItemVH) {
            CoinModel m = item.coin;
            ItemVH vh = (ItemVH) holder;

            if (m == null) return;

            String typeStr = m.getType() != null ? m.getType() : "Reward";
            vh.title.setText(formatTypeTitle(typeStr));

            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(new Date(m.getTimeMillis()));
            vh.date.setText(date);

            // Amount & color
            if ("Credit".equalsIgnoreCase(m.getStatus())) {
                vh.coins.setText("+" + m.getAmount());
                vh.coins.setTextColor(Color.parseColor("#10B981"));
            } else {
                vh.coins.setText("-" + m.getAmount());
                vh.coins.setTextColor(Color.parseColor("#EF4444"));
            }

            // Category Icon
            if ("ads".equalsIgnoreCase(typeStr)) {
                vh.icon.setImageResource(R.drawable.ic_watch);
            } else if ("offer".equalsIgnoreCase(typeStr)) {
                vh.icon.setImageResource(R.drawable.ic_money);
            } else if ("referral".equalsIgnoreCase(typeStr)) {
                vh.icon.setImageResource(R.drawable.ic_share);
            } else if ("Daily spin".equalsIgnoreCase(typeStr) || "spin".equalsIgnoreCase(typeStr)) {
                vh.icon.setImageResource(R.drawable.ic_spinner);
            } else if ("Lucky Draw".equalsIgnoreCase(typeStr)) {
                vh.icon.setImageResource(R.drawable.ic_lucky);
            } else if ("scratch".equalsIgnoreCase(typeStr) || "Daily scratch".equalsIgnoreCase(typeStr)) {
                vh.icon.setImageResource(R.drawable.ic_gift);
            } else if ("Withdrawal".equalsIgnoreCase(typeStr)) {
                vh.icon.setImageResource(R.drawable.img_redeem);
            } else {
                vh.icon.setImageResource(R.drawable.ic_reward);
            }

            // Currency Icon
            if ("token".equalsIgnoreCase(m.getIstype())) {
                vh.coinimg.setImageResource(R.drawable.ic_ticket);
            } else {
                vh.coinimg.setImageResource(R.drawable.ic_coin);
            }

            vh.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(m);
                }
            });
        }
    }

    private String formatTypeTitle(String type) {
        if (type == null || type.isEmpty()) return "Reward";
        if (type.equalsIgnoreCase("ads")) return "Ad Reward";
        if (type.equalsIgnoreCase("offer")) return "Offer Task";
        if (type.equalsIgnoreCase("referral")) return "Referral Bonus";
        if (type.equalsIgnoreCase("Daily spin") || type.equalsIgnoreCase("spin")) return "Spin Wheel Reward";
        if (type.equalsIgnoreCase("Lucky Draw")) return "Lucky Draw Contest";
        if (type.equalsIgnoreCase("scratch") || type.equalsIgnoreCase("Daily scratch")) return "Scratch Card Reward";
        if (type.equalsIgnoreCase("Withdrawal")) return "Redeem Payout";
        return type.substring(0, 1).toUpperCase(Locale.getDefault()) + type.substring(1);
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderVH(View v) {
            super(v);
            headerText = v.findViewById(R.id.headerText);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        TextView title, date, coins;
        ImageView icon, coinimg;

        ItemVH(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            date = v.findViewById(R.id.date);
            coins = v.findViewById(R.id.coins);
            icon = v.findViewById(R.id.icon);
            coinimg = v.findViewById(R.id.coin);
        }
    }
}