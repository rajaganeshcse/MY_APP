package com.example.rgamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FreeFireTournamentAdapter
        extends RecyclerView.Adapter<FreeFireTournamentAdapter.ViewHolder> {

    /* ================= LISTENER ================= */
    public interface Listener {
        void onJoin(FreeFireTournamentModel model);
        void onCheckWinners(FreeFireTournamentModel model);
    }

    private final Context context;
    private final List<FreeFireTournamentModel> list;
    private final Listener listener;

    public FreeFireTournamentAdapter(
            Context context,
            List<FreeFireTournamentModel> list,
            Listener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    /* ================= VIEW HOLDER ================= */

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_freefire_tournament, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder h,
            int position) {

        FreeFireTournamentModel m = list.get(position);

        /* ================= PRIZE ================= */
        h.txtPrize.setText("Win " + m.getPrizeCoins() + " Coins");

        /* ================= SLOTS ================= */
        int total = m.getTotalSlots();
        int joined = m.getJoinedSlots();
        int left = Math.max(0, total - joined);

        h.txtSlots.setText("Slots Left : " + left + "/" + total);

        /* ================= PROGRESS ================= */
        int percent = 0;
        if (total > 0) {
            percent = (joined * 100) / total;
        }
        h.progress.setMax(100);
        h.progress.setProgress(percent);

        /* ================= JOIN BUTTON ================= */
        h.btnJoin.setOnClickListener(null);

        if (!"OPEN".equalsIgnoreCase(m.getStatus()) || left <= 0) {
            h.btnJoin.setText("CLOSED");
            h.btnJoin.setEnabled(false);
        } else {
            h.btnJoin.setEnabled(true);
            h.btnJoin.setText("Join (" + m.getEntryTickets() + " Tickets)");
            h.btnJoin.setOnClickListener(v -> listener.onJoin(m));
        }

        /* ================= WINNERS BUTTON ================= */
        h.btnWinners.setOnClickListener(v ->
                listener.onCheckWinners(m));
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    /* ================= VIEW HOLDER ================= */

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtPrize, txtSlots, btnJoin, btnWinners;
        ProgressBar progress;

        ViewHolder(View v) {
            super(v);
            txtPrize = v.findViewById(R.id.txtPrize);
            txtSlots = v.findViewById(R.id.txtSlots);
            btnJoin = v.findViewById(R.id.btnJoin);
            btnWinners = v.findViewById(R.id.btnWinners);
            progress = v.findViewById(R.id.progressSlots);
        }
    }
}
