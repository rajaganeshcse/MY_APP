package com.app.rewardsplanet.share_earn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.share_earn.model.ShareEarnEarningsResponse;
import com.bumptech.glide.Glide;

import java.util.List;

public class OfferPerformanceAdapter extends RecyclerView.Adapter<OfferPerformanceAdapter.PerfViewHolder> {

    private final Context context;
    private final List<ShareEarnEarningsResponse.OfferPerformance> list;

    public OfferPerformanceAdapter(Context context, List<ShareEarnEarningsResponse.OfferPerformance> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public PerfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_offer_performance, parent, false);
        return new PerfViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PerfViewHolder holder, int position) {
        ShareEarnEarningsResponse.OfferPerformance perf = list.get(position);

        holder.txtTitle.setText(perf.getTitle() != null ? perf.getTitle() : "Offer");
        holder.txtStats.setText("Clicks: " + perf.getClicks() + " | Conversions: " + perf.getConversions());
        holder.txtCoins.setText(String.format("%,d", perf.getEarnedCoins()) + " Coins");

        if (perf.getLogoUrl() != null && !perf.getLogoUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(perf.getLogoUrl())
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(holder.imgLogo);
        } else {
            holder.imgLogo.setImageResource(R.drawable.logo);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class PerfViewHolder extends RecyclerView.ViewHolder {
        ImageView imgLogo;
        TextView txtTitle, txtStats, txtCoins;

        public PerfViewHolder(@NonNull View itemView) {
            super(itemView);
            imgLogo = itemView.findViewById(R.id.imgPerfLogo);
            txtTitle = itemView.findViewById(R.id.txtPerfTitle);
            txtStats = itemView.findViewById(R.id.txtPerfStats);
            txtCoins = itemView.findViewById(R.id.txtPerfCoins);
        }
    }
}
