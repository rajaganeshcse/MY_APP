package com.app.rewardsplanet.lucky_draw;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.models.WatchVideoModel;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchVideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TASK = 0;
    private static final int TYPE_AD = 1;

    public interface OnWatchClickListener {
        void onWatchClick(WatchVideoModel item, int adapterPosition);
    }

    private final List<WatchVideoModel> list;
    private final OnWatchClickListener listener;
    private final Map<Integer, NativeAd> nativeAdMap = new HashMap<>();

    public WatchVideoAdapter(List<WatchVideoModel> list, OnWatchClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if ((position + 1) % 4 == 0) {
            return TYPE_AD;
        }
        return TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_AD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.native_ad_layout, parent, false);
            return new NativeAdViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_watch_video, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_AD) {
            bindNativeAd((NativeAdViewHolder) holder, position);
        } else {
            int taskIndex = getTaskIndex(position);
            if (taskIndex < 0 || taskIndex >= list.size()) return;
            WatchVideoModel model = list.get(taskIndex);
            bindTaskItem((TaskViewHolder) holder, model, position);
        }
    }

    private int getTaskIndex(int adapterPosition) {
        return adapterPosition - (adapterPosition / 4);
    }

    @Override
    public int getItemCount() {
        int count = list.size();
        if (count == 0) return 0;
        return count + (count / 3);
    }

    /* ================= BIND TASK ITEM ================= */

    private void bindTaskItem(TaskViewHolder holder, WatchVideoModel model, int adapterPosition) {
        holder.txtVideoTitle.setText(model.getTitle());
        holder.txtVideoRewardCoins.setText("+" + model.getCoinReward() + " Coins");
        holder.txtVideoRewardTickets.setText("+" + model.getTicketReward() + " Ticket" + (model.getTicketReward() > 1 ? "s" : ""));

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
            if (holder.cardRoot != null) holder.cardRoot.setAlpha(0.85f);
        } else if (model.isLoading()) {
            holder.btnWatchVideoItem.setText("Loading...");
            holder.btnWatchVideoItem.setEnabled(false);
            holder.btnWatchVideoItem.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#64748B")));
            holder.btnWatchVideoItem.setTextColor(Color.WHITE);
            if (holder.cardRoot != null) holder.cardRoot.setAlpha(1.0f);
        } else if (model.isLocked()) {
            holder.btnWatchVideoItem.setText("Locked 🔒");
            holder.btnWatchVideoItem.setEnabled(false);
            holder.btnWatchVideoItem.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
            holder.btnWatchVideoItem.setTextColor(Color.parseColor("#64748B"));
            if (holder.cardRoot != null) holder.cardRoot.setAlpha(0.7f);
        } else {
            holder.btnWatchVideoItem.setText("Watch 📺");
            holder.btnWatchVideoItem.setEnabled(true);
            holder.btnWatchVideoItem.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
            holder.btnWatchVideoItem.setTextColor(Color.WHITE);
            if (holder.cardRoot != null) holder.cardRoot.setAlpha(1.0f);
        }

        holder.btnWatchVideoItem.setOnClickListener(v -> {
            if (!model.isCompleted() && !model.isLocked() && !model.isLoading() && listener != null) {
                listener.onWatchClick(model, adapterPosition);
            }
        });
    }

    /* ================= BIND NATIVE AD ================= */

    private void bindNativeAd(NativeAdViewHolder holder, int position) {
        if (nativeAdMap.containsKey(position)) {
            NativeAd nativeAd = nativeAdMap.get(position);
            if (nativeAd != null) {
                populateNativeAd(nativeAd, holder.nativeAdView);
            }
            return;
        }

        try {
            AdLoader adLoader = new AdLoader.Builder(holder.itemView.getContext(), AdsManager.NATIVE_AD_ID)
                    .forNativeAd(nativeAd -> {
                        nativeAdMap.put(position, nativeAd);
                        populateNativeAd(nativeAd, holder.nativeAdView);
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                            // Handle ad load failure silently
                        }
                    })
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());
        } catch (Exception ignored) {}
    }

    private void populateNativeAd(NativeAd nativeAd, NativeAdView adView) {
        if (nativeAd == null || adView == null) return;

        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        TextView headline = (TextView) adView.getHeadlineView();
        if (headline != null) {
            headline.setText(nativeAd.getHeadline());
        }

        TextView body = (TextView) adView.getBodyView();
        if (body != null) {
            if (nativeAd.getBody() != null && !nativeAd.getBody().isEmpty()) {
                body.setText(nativeAd.getBody());
                body.setVisibility(View.VISIBLE);
            } else {
                body.setVisibility(View.GONE);
            }
        }

        Button cta = (Button) adView.getCallToActionView();
        if (cta != null) {
            if (nativeAd.getCallToAction() != null && !nativeAd.getCallToAction().isEmpty()) {
                cta.setText(nativeAd.getCallToAction());
                cta.setVisibility(View.VISIBLE);
            } else {
                cta.setVisibility(View.GONE);
            }
        }

        ImageView icon = (ImageView) adView.getIconView();
        if (icon != null) {
            if (nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
                icon.setImageDrawable(nativeAd.getIcon().getDrawable());
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
        }

        adView.setNativeAd(nativeAd);
    }

    /* ================= VIEW HOLDERS ================= */

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView txtVideoTitle, txtVideoRewardCoins, txtVideoRewardTickets, badgeSuperBonus;
        MaterialButton btnWatchVideoItem;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardWatchVideoRoot);
            txtVideoTitle = itemView.findViewById(R.id.txtVideoTitle);
            txtVideoRewardCoins = itemView.findViewById(R.id.txtVideoRewardCoins);
            txtVideoRewardTickets = itemView.findViewById(R.id.txtVideoRewardTickets);
            badgeSuperBonus = itemView.findViewById(R.id.badgeSuperBonus);
            btnWatchVideoItem = itemView.findViewById(R.id.btnWatchVideoItem);
        }
    }

    static class NativeAdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView nativeAdView;

        NativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            nativeAdView = itemView.findViewById(R.id.nativeAdView);
        }
    }
}
