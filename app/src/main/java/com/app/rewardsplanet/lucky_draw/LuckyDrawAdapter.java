package com.app.rewardsplanet.lucky_draw;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.models.LuckyDrawModel;
import com.google.android.material.button.MaterialButton;

import java.util.*;

public class LuckyDrawAdapter
        extends RecyclerView.Adapter<LuckyDrawAdapter.ViewHolder> {

    public interface Listener {
        void onJoin(LuckyDrawModel model);
        void onJoinWithTickets(LuckyDrawModel model);
        void onCheckWinners(LuckyDrawModel model);
        void onViewMyTokens(LuckyDrawModel model);
    }

    private final List<LuckyDrawModel> list;
    private final Listener listener;
    private final Set<String> loadingIds = new HashSet<>();
    private int userTickets;

    public LuckyDrawAdapter(List<LuckyDrawModel> list,
                            Listener listener,
                            int userTickets) {
        this.list = list;
        this.listener = listener;
        this.userTickets = userTickets;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lucky_draw, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        LuckyDrawModel model = list.get(position);
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
        if (model.getMyTicketsCount() > 0) {
            h.btnticket.setText("🎟️ Submitted (" + model.getMyTicketsCount() + ")");
        } else {
            h.btnticket.setText("🎟️ Ticket Entry");
        }
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

    @Override
    public int getItemCount() {
        return list.size();
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

    static class ViewHolder extends RecyclerView.ViewHolder {

        com.google.android.material.card.MaterialCardView cardRoot;
        TextView txtReward, txtSlots, txtPercent, token;
        ProgressBar progressSlots;
        LinearLayout joined;
        MaterialButton btnJoin, btnticket;

        ViewHolder(@NonNull View itemView) {
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
}