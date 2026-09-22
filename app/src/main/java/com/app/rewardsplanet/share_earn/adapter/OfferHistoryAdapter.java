package com.app.rewardsplanet.share_earn.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.share_earn.model.OfferHistoryResponse;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OfferHistoryAdapter extends RecyclerView.Adapter<OfferHistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<OfferHistoryResponse.HistoryItem> historyItems;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public OfferHistoryAdapter(Context context, List<OfferHistoryResponse.HistoryItem> historyItems) {
        this.context = context;
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_offer_history, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        OfferHistoryResponse.HistoryItem item = historyItems.get(position);

        holder.txtTitle.setText(item.getTitle() != null ? item.getTitle() : "Offer");
        holder.txtCoins.setText(String.format("%,d", item.getRewardCoins()) + " Coins");

        if (item.getCreatedAt() > 0) {
            holder.txtDate.setText(dateFormat.format(new Date(item.getCreatedAt())));
        } else {
            holder.txtDate.setText("-");
        }

        String status = item.getStatus() != null ? item.getStatus() : "Pending";
        holder.txtStatus.setText(status);

        if ("APPROVED".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            holder.txtStatus.setBackgroundColor(Color.parseColor("#DCFCE7"));
            holder.txtStatus.setTextColor(Color.parseColor("#166534"));
        } else if ("REJECTED".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
            holder.txtStatus.setBackgroundColor(Color.parseColor("#FEE2E2"));
            holder.txtStatus.setTextColor(Color.parseColor("#991B1B"));
        } else {
            holder.txtStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
            holder.txtStatus.setTextColor(Color.parseColor("#B45309"));
        }

        if (item.getLogoUrl() != null && !item.getLogoUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(item.getLogoUrl())
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(holder.imgLogo);
        } else {
            holder.imgLogo.setImageResource(R.drawable.logo);
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgLogo;
        TextView txtTitle, txtDate, txtCoins, txtStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgLogo = itemView.findViewById(R.id.imgHistoryLogo);
            txtTitle = itemView.findViewById(R.id.txtHistoryTitle);
            txtDate = itemView.findViewById(R.id.txtHistoryDate);
            txtCoins = itemView.findViewById(R.id.txtHistoryCoins);
            txtStatus = itemView.findViewById(R.id.txtHistoryStatus);
        }
    }
}
