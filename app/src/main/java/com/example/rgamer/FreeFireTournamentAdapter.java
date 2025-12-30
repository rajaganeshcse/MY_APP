package com.example.rgamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FreeFireTournamentAdapter
        extends RecyclerView.Adapter<FreeFireTournamentAdapter.ViewHolder> {

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
        h.txtPrize.setText("Win " + m.getCoin() + " Coins");

        /* ================= TIME & DATE ================= */
        if (m.getStartTimeMillis() > 0) {
            Date d = new Date(m.getStartTimeMillis());

            h.txtTimeLabel.setText(
                    new SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                    ).format(d)
            );

            h.txtDateLabel.setText(
                    new SimpleDateFormat(
                            "dd MMM",
                            Locale.getDefault()
                    ).format(d)
            );
        } else {
            h.txtTimeLabel.setText("-");
            h.txtDateLabel.setText("-");
        }

        /* ================= SLOTS ================= */
        int total = m.getTotalSlots();
        int joined = m.getJoinedSlots();
        int left = Math.max(0, total - joined); // 🔥 FIX

        h.txtSlots.setText(
                "Slots Left : " + left + "/" + total
        );

        h.progressSlots.setMax(100); // 🔥 FIX
        h.progressSlots.setProgress(
                total > 0 ? (joined * 100) / total : 0
        );

        /* ================= HIDE UNUSED ================= */
        h.txtCountdown.setVisibility(View.GONE);
        h.txtGameId.setVisibility(View.GONE);
        h.txtGamePassword.setVisibility(View.GONE);

        /* ================= JOIN BUTTON ================= */
        h.btnJoin.setOnClickListener(null); // 🔥 FIX recycling
        h.btnJoin.setText(
                "Join (" + m.getEntryTickets() + " Tickets)"
        );
        h.btnJoin.setEnabled(true);
        h.btnJoin.setOnClickListener(v ->
                listener.onJoin(m));

        /* ================= WINNERS ================= */
        h.btnWinners.setOnClickListener(null); // 🔥 FIX recycling
        h.btnWinners.setOnClickListener(v ->
                listener.onCheckWinners(m));
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    /* ================= VIEW HOLDER ================= */
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtPrize, txtSlots,
                txtTimeLabel, txtDateLabel,
                txtCountdown, txtGameId,
                txtGamePassword ;
        MaterialButton btnJoin, btnWinners;

        ProgressBar progressSlots;

        ViewHolder(View v) {
            super(v);

            txtPrize = v.findViewById(R.id.txtPrize);
            txtSlots = v.findViewById(R.id.txtSlots);
            txtTimeLabel = v.findViewById(R.id.txtTimeLabel);
            txtDateLabel = v.findViewById(R.id.txtDateLabel);
            txtCountdown = v.findViewById(R.id.txtCountdown);
            txtGameId = v.findViewById(R.id.gameid);
            txtGamePassword = v.findViewById(R.id.gamePassword);
            btnJoin = v.findViewById(R.id.btnJoin);
            btnWinners = v.findViewById(R.id.btnWinners);
            progressSlots = v.findViewById(R.id.progressSlots);
        }
    }
}
