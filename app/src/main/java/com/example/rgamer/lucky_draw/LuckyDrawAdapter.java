package com.example.rgamer.lucky_draw;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.LuckyDrawModel;
import com.google.android.material.button.MaterialButton;

import java.util.*;

public class LuckyDrawAdapter
        extends RecyclerView.Adapter<LuckyDrawAdapter.ViewHolder> {

    public interface Listener {
        void onJoin(LuckyDrawModel model);
        void onJoinWithTickets(LuckyDrawModel model);
        void onCheckWinners(LuckyDrawModel model);
    }

    private final List<LuckyDrawModel> list;
    private final Listener listener;

    // 🔥 LOADING STATE
    private final Set<String> loadingIds = new HashSet<>();

    // 🔥 USER TICKETS
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
        h.txtSlots.setText("Filled : " + filled + "/" + total);
        h.txtPercent.setText((filled * 100 / total) + "% Filled");

        h.progressSlots.setMax(total);
        h.progressSlots.setProgress(filled);

        boolean loading = loadingIds.contains(id);

        /* ================= RESET UI ================= */
        h.btnJoin.setText("Free Entry");
        h.btnticket.setText("Ticket Entry");

        h.btnJoin.setEnabled(true);
        h.btnticket.setEnabled(true);

        h.btnJoin.setBackgroundTintList(null);
        h.btnticket.setBackgroundTintList(null);

        /* ================= LOADING ================= */
        if (loading) {
            h.btnJoin.setText("Joining...");
            h.btnticket.setText("Please wait...");
            h.btnJoin.setEnabled(false);
            h.btnticket.setEnabled(false);
            return;
        }

        /* ================= JOINED ================= */
        if (model.isJoinedByMe()) {

            h.btnJoin.setText("Joined");
            h.btnJoin.setEnabled(false);

            h.btnticket.setEnabled(userTickets > 0);

            h.btnJoin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            return;
        }

        /* ================= FULL ================= */
        if (model.isFull() || !"OPEN".equals(model.getStatus())) {

            h.btnJoin.setText("FULL");
            h.btnJoin.setEnabled(false);
            h.btnticket.setEnabled(false);

            h.btnJoin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            return;
        }

        /* ================= ACTIVE ================= */

        // 🔥 FREE ENTRY
        h.btnJoin.setOnClickListener(v -> {

            if (loadingIds.contains(id)) return;

            // set loading immediately
            loadingIds.add(id);
            notifyItemChanged(position);

            listener.onJoin(model);
        });

        // 🔥 TICKET ENTRY
        h.btnticket.setEnabled(userTickets > 0);

        h.btnticket.setOnClickListener(v -> {

            if (loadingIds.contains(id)) return;

            if (userTickets <= 0) {
                Toast.makeText(v.getContext(),
                        "No tickets available",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // DO NOT set loading here (handled in Activity)
            listener.onJoinWithTickets(model);
        });

        /* ================= LONG PRESS ================= */
        h.btnticket.setOnLongClickListener(v -> {
            listener.onCheckWinners(model);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /* ================= LOADING CONTROL ================= */

    public void setLoading(String id, boolean value) {
        if (value) loadingIds.add(id);
        else loadingIds.remove(id);

        notifyDataSetChanged(); // can optimize later
    }

    public void clearLoading(String drawId) {
        loadingIds.remove(drawId);

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(drawId)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    /* ================= UPDATE USER TICKETS ================= */

    public void updateUserTickets(int tickets) {
        this.userTickets = tickets;
        notifyDataSetChanged();
    }

    /* ================= VIEW HOLDER ================= */

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtReward, txtSlots, txtPercent;
        ProgressBar progressSlots;
        MaterialButton btnJoin, btnticket;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtReward = itemView.findViewById(R.id.txtReward);
            txtSlots = itemView.findViewById(R.id.txtSlots);
            txtPercent = itemView.findViewById(R.id.txtPercent);
            progressSlots = itemView.findViewById(R.id.progressSlots);

            btnJoin = itemView.findViewById(R.id.btnFreeEntry);
            btnticket = itemView.findViewById(R.id.btnticketEntry);
        }
    }
}