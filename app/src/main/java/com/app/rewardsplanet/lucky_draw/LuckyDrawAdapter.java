package com.app.rewardsplanet.lucky_draw;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.models.LuckyDrawModel;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.button.MaterialButton;

import java.util.*;

public class LuckyDrawAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DRAW = 0;
    private static final int TYPE_AD = 1;

    public interface Listener {
        void onJoin(LuckyDrawModel model);
        void onJoinWithTickets(LuckyDrawModel model);
        void onCheckWinners(LuckyDrawModel model);
        void onViewMyTokens(LuckyDrawModel model);
    }

    private final List<LuckyDrawModel> list;
    private final Listener listener;
    private final Set<String> loadingIds = new HashSet<>();
    private final Map<Integer, NativeAd> nativeAdMap = new HashMap<>();
    private int userTickets;

    public LuckyDrawAdapter(List<LuckyDrawModel> list,
                            Listener listener,
                            int userTickets) {
        this.list = list;
        this.listener = listener;
        this.userTickets = userTickets;
    }

    @Override
    public int getItemViewType(int position) {
        if ((position + 1) % 4 == 0) {
            return TYPE_AD;
        }
        return TYPE_DRAW;
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
                    .inflate(R.layout.item_lucky_draw, parent, false);
            return new DrawViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_AD) {
            NativeAdViewHolder adHolder = (NativeAdViewHolder) holder;
            bindNativeAd(adHolder, position);
        } else {
            DrawViewHolder h = (DrawViewHolder) holder;
            int drawIndex = getDrawIndex(position);
            if (drawIndex < 0 || drawIndex >= list.size()) return;
            LuckyDrawModel model = list.get(drawIndex);
            bindDrawItem(h, model);
        }
    }

    private int getDrawIndex(int adapterPosition) {
        return adapterPosition - (adapterPosition / 4);
    }

    @Override
    public int getItemCount() {
        int drawCount = list.size();
        if (drawCount == 0) return 0;
        return drawCount + (drawCount / 3);
    }

    /* ================= BIND DRAW ITEM ================= */

    private void bindDrawItem(DrawViewHolder h, LuckyDrawModel model) {
        String id = model.getId();

        int total = Math.max(model.getTotalSlots(), 1);
        int filled = model.getFilledSlots();

        h.txtReward.setText("Win " + model.getRewardCoins() + " Coins 🎉");
        h.txtSlots.setText("Filled: " + filled + " / " + total + " Slots");
        h.txtPercent.setText((filled * 100 / total) + "% Filled");

        h.progressSlots.setMax(total);
        h.progressSlots.setProgress(filled);

        int totalJoined = model.getMyTicketsCount() + (model.isAdJoined() ? 1 : 0);
        
        if (totalJoined > 0) {
            String tokenText = "✓ " + totalJoined + (totalJoined == 1 ? " Token" : " Tokens");
            h.token.setText(tokenText);
            h.token.setTextColor(Color.WHITE);
            h.joined.setBackgroundResource(R.drawable.bg_joined_chip_glow);
            h.joined.setVisibility(View.VISIBLE);
            h.joined.setOnClickListener(v -> listener.onViewMyTokens(model));
            if (h.cardRoot != null) {
                h.cardRoot.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#10B981")));
                h.cardRoot.setStrokeWidth(4);
            }
        } else {
            h.joined.setVisibility(View.GONE);
            if (h.cardRoot != null) {
                h.cardRoot.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EEF2FF")));
                h.cardRoot.setStrokeWidth(2);
            }
        }

        /* RESET */
        h.btnJoin.setEnabled(true);
        h.btnticket.setEnabled(true);

        /* TICKET ENTRY BUTTON TEXT */
        h.btnticket.setText("🎟️ Ticket Entry");
        h.btnticket.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F46E5")));

        /* AD STATE */
        if (model.isAdJoined()) {
            h.btnJoin.setText("✓ Free Used");
            h.btnJoin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            h.btnJoin.setEnabled(false);
        } else {
            h.btnJoin.setText("📺 Free Entry");
            h.btnJoin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
        }

        /* FULL / CLOSED STATE */
        if (model.isFull() || !"OPEN".equals(model.getStatus())) {
            h.btnJoin.setText("🔒 Draw Closed");
            h.btnticket.setText("🔒 Draw Closed");
            h.btnJoin.setEnabled(false);
            h.btnticket.setEnabled(false);
            h.btnJoin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            h.btnticket.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
        }

        /* FREE ENTRY (ONLY ONCE) */
        h.btnJoin.setOnClickListener(v -> {

            if (model.isAdJoined() || model.isFull()) {
                Toast.makeText(v.getContext(),
                        "Already used free entry",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (loadingIds.contains(id)) return;

            loadingIds.add(id);
            listener.onJoin(model);
        });

        /* TICKET ENTRY (MULTIPLE) */
        h.btnticket.setOnClickListener(v -> {

            if (model.isFull()) {
                Toast.makeText(v.getContext(),
                        "Draw full",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (loadingIds.contains(id)) return;

            if (userTickets <= 0) {
                Toast.makeText(v.getContext(),
                        "No tickets available",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            loadingIds.add(id);
            listener.onJoinWithTickets(model);
        });

        /* LONG PRESS */
        h.btnticket.setOnLongClickListener(v -> {
            listener.onCheckWinners(model);
            return true;
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
                            // Silently handle ad load failure
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

    public void setLoading(String id, boolean value) {
        if (value) loadingIds.add(id);
        else loadingIds.remove(id);
    }

    public void clearLoading(String id) {
        loadingIds.remove(id);
    }

    public void updateUserTickets(int tickets) {
        this.userTickets = tickets;
        notifyDataSetChanged();
    }

    /* ================= VIEW HOLDERS ================= */

    static class DrawViewHolder extends RecyclerView.ViewHolder {

        com.google.android.material.card.MaterialCardView cardRoot;
        TextView txtReward, txtSlots, txtPercent, token;
        ProgressBar progressSlots;
        LinearLayout joined;
        MaterialButton btnJoin, btnticket;

        DrawViewHolder(@NonNull View itemView) {
            super(itemView);

            cardRoot = itemView.findViewById(R.id.cardLuckyDrawRoot);
            txtReward = itemView.findViewById(R.id.txtReward);
            txtSlots = itemView.findViewById(R.id.txtSlots);
            txtPercent = itemView.findViewById(R.id.txtPercent);
            progressSlots = itemView.findViewById(R.id.progressSlots);
            joined = itemView.findViewById(R.id.joined);
            btnJoin = itemView.findViewById(R.id.btnFreeEntry);
            token = itemView.findViewById(R.id.token);
            btnticket = itemView.findViewById(R.id.btnticketEntry);
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