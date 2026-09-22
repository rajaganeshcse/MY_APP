package com.app.rewardsplanet.share_earn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.share_earn.model.ShareEarnOffer;
import com.bumptech.glide.Glide;

import java.util.List;

public class ShareEarnOfferAdapter extends RecyclerView.Adapter<ShareEarnOfferAdapter.OfferViewHolder> {

    public interface OnOfferClickListener {
        void onOfferClick(ShareEarnOffer offer);
        void onShareClick(ShareEarnOffer offer);
    }

    private final Context context;
    private final List<ShareEarnOffer> offers;
    private final OnOfferClickListener listener;

    public ShareEarnOfferAdapter(Context context, List<ShareEarnOffer> offers, OnOfferClickListener listener) {
        this.context = context;
        this.offers = offers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_share_earn_offer, parent, false);
        return new OfferViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        ShareEarnOffer offer = offers.get(position);

        holder.txtTitle.setText(offer.getTitle() != null ? offer.getTitle() : "Offer");
        holder.txtShortDesc.setText(offer.getShortDescription() != null ? offer.getShortDescription() : "");
        holder.txtRewardCoins.setText("Earn " + String.format("%,d", offer.getRewardCoins()) + " Coins");

        if (offer.getLogoUrl() != null && !offer.getLogoUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(offer.getLogoUrl())
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(holder.imgLogo);
        } else {
            holder.imgLogo.setImageResource(R.drawable.logo);
        }

        if (offer.getPriority() >= 10) {
            holder.txtBadgeTag.setVisibility(View.VISIBLE);
            holder.txtBadgeTag.setText("Popular");
        } else {
            holder.txtBadgeTag.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOfferClick(offer);
        });

        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(offer);
        });
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        ImageView imgLogo;
        TextView txtTitle, txtShortDesc, txtRewardCoins, txtBadgeTag;
        Button btnShare;

        public OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            imgLogo = itemView.findViewById(R.id.imgOfferLogo);
            txtTitle = itemView.findViewById(R.id.txtOfferTitle);
            txtShortDesc = itemView.findViewById(R.id.txtShortDesc);
            txtRewardCoins = itemView.findViewById(R.id.txtRewardCoins);
            txtBadgeTag = itemView.findViewById(R.id.txtBadgeTag);
            btnShare = itemView.findViewById(R.id.btnShareCard);
        }
    }
}
