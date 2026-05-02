package com.example.rgamer.withdraws;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RewardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ListItem> list;

    public RewardAdapter(List<ListItem> list) {
        this.list = list;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).type;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        ListItem item = list.get(position);

        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).headerText.setText(item.header);
        } else {
            CoinModel m = item.coin;
            ItemVH vh = (ItemVH) holder;

            vh.title.setText(m.getType().toUpperCase());

            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(new Date(m.getTimeMillis()));
            vh.date.setText(date);

// ✅ Amount color
            if ("Credit".equalsIgnoreCase(m.getStatus())) {
                vh.coins.setText("+" + m.getAmount());
                vh.coins.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                vh.coins.setText("-" + m.getAmount());
                vh.coins.setTextColor(Color.RED);
            }

// ✅ LEFT ICON (activity type)
            if ("ads".equalsIgnoreCase(m.getType())) {
                vh.icon.setImageResource(R.drawable.ic_watch);
            } else if ("offer".equalsIgnoreCase(m.getType())) {
                vh.icon.setImageResource(R.drawable.ic_money);
            } else if ("referral".equalsIgnoreCase(m.getType())) {
                vh.icon.setImageResource(R.drawable.ic_share);
            } else if ("Daily spin".equalsIgnoreCase(m.getType())) {
                vh.icon.setImageResource(R.drawable.ic_spinner);
            } else {
                vh.icon.setImageResource(R.drawable.ic_reward);
            }

// ✅ RIGHT ICON (coin / token)
            if ("token".equalsIgnoreCase(m.getIstype())) {
                vh.coinimg.setImageResource(R.drawable.ic_ticket);
            } else {
                vh.coinimg.setImageResource(R.drawable.ic_coin);
            }
        }
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
        ImageView icon,coinimg;

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