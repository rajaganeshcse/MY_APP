package com.app.rewardsplanet.lucky_draw;

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
import com.app.rewardsplanet.models.HitRewardzModel;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HitRewardzAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TASK = 0;
    private static final int TYPE_AD = 1;

    public interface OnHitzClickListener {
        void onHitzClick(HitRewardzModel item);
    }

    private final List<HitRewardzModel> list;
    private final OnHitzClickListener listener;
    private final Map<Integer, NativeAd> nativeAdMap = new HashMap<>();

    public HitRewardzAdapter(List<HitRewardzModel> list, OnHitzClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if ((position + 1) % 3 == 0) {
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
                    .inflate(R.layout.item_hit_rewardz, parent, false);
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
            HitRewardzModel item = list.get(taskIndex);
            bindTaskItem((TaskViewHolder) holder, item);
        }
    }

    private int getTaskIndex(int adapterPosition) {
        return adapterPosition - (adapterPosition / 3);
    }

    @Override
    public int getItemCount() {
        int count = list != null ? list.size() : 0;
        if (count == 0) return 0;
        return count + (count / 2);
    }

    /* ================= BIND TASK ITEM ================= */

    private void bindTaskItem(TaskViewHolder holder, HitRewardzModel item) {
        holder.txtTitle.setText(item.getTitle());
        holder.txtCoins.setText("+" + item.getCoins() + " Coins");
        if (holder.txtTickets != null) {
            holder.txtTickets.setText("+" + item.getTickets() + " Ticket" + (item.getTickets() > 1 ? "s" : ""));
        }

        if (item.isCompleted()) {
            holder.btnWatch.setText("COMPLETED ✅");
            holder.btnWatch.setEnabled(false);
            holder.btnWatch.setBackgroundColor(Color.parseColor("#E2E8F0"));
            holder.btnWatch.setTextColor(Color.parseColor("#64748B"));
            holder.itemView.setAlpha(0.85f);
        } else if (item.isLocked()) {
            holder.btnWatch.setText("Locked 🔒");
            holder.btnWatch.setEnabled(false);
            holder.btnWatch.setBackgroundColor(Color.parseColor("#F1F5F9"));
            holder.btnWatch.setTextColor(Color.parseColor("#94A3B8"));
            holder.itemView.setAlpha(0.65f);
        } else {
            holder.btnWatch.setText("WATCH AD ⚡");
            holder.btnWatch.setEnabled(true);
            holder.btnWatch.setBackgroundResource(R.drawable.bg_btn_quiz_watch_ad);
            holder.btnWatch.setTextColor(Color.WHITE);
            holder.itemView.setAlpha(1.0f);

            holder.btnWatch.setOnClickListener(v -> {
                if (!item.isCompleted() && !item.isLocked() && listener != null) {
                    listener.onHitzClick(item);
                }
            });
        }
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
        TextView txtTitle, txtCoins, txtTickets;
        MaterialButton btnWatch;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtHitzTitle);
            txtCoins = itemView.findViewById(R.id.txtHitzRewardCoins);
            txtTickets = itemView.findViewById(R.id.txtHitzRewardTickets);
            btnWatch = itemView.findViewById(R.id.btnWatchHitzItem);
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
