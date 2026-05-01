package com.example.rgamer.Game;

import android.content.Context;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
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
                    new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(d)
            );

            h.txtDateLabel.setText(
                    new SimpleDateFormat("dd MMM", Locale.getDefault()).format(d)
            );
        } else {
            h.txtTimeLabel.setText("-");
            h.txtDateLabel.setText("-");
        }

        /* ================= COUNTDOWN ================= */
        startCountdown(h, m.getStartTimeMillis());

        /* ================= SLOTS ================= */
        int total = m.getTotalSlots();
        int joined = m.getJoinedSlots();
        int left = Math.max(0, total - joined);

        h.txtSlots.setText("Slots Left : " + left + "/" + total);

        if (h.progressSlots != null) {
            h.progressSlots.setMax(100);
            h.progressSlots.setProgress(
                    total > 0 ? (joined * 100) / total : 0
            );
        }

        /* ================= DEFAULT VISIBILITY ================= */
        h.txtGameId.setVisibility(View.GONE);
        h.txtGamePassword.setVisibility(View.GONE);

        h.btnJoin.setOnClickListener(null);

        /* ================= JOIN STATE ================= */
        if (m.isJoined()) {

            h.btnJoin.setEnabled(false);
            h.btnJoin.setText("Joined");

            if (m.getJoinedUsername() != null) {
                h.txtGameId.setText("User : " + m.getJoinedUsername());
                h.txtGameId.setVisibility(View.VISIBLE);
            }

            if (m.getJoinedGameId() != null) {
                h.txtGamePassword.setText("Game ID : " + m.getJoinedGameId());
                h.txtGamePassword.setVisibility(View.VISIBLE);
            }

        } else if (left <= 0) {

            h.btnJoin.setEnabled(false);
            h.btnJoin.setText("Full");

        } else {

            h.btnJoin.setEnabled(true);
            h.btnJoin.setText("Join (" + m.getEntryTickets() + " Tickets)");
            h.btnJoin.setOnClickListener(v -> listener.onJoin(m));
        }

        /* ================= WINNERS ================= */
        h.btnWinners.setOnClickListener(v ->
                listener.onCheckWinners(m));
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    /* ================= COUNTDOWN FUNCTION ================= */
    private void startCountdown(ViewHolder holder, long startTimeMillis) {

        // cancel old timer (IMPORTANT)
        if (holder.timer != null) {
            holder.timer.cancel();
        }

        if (startTimeMillis <= System.currentTimeMillis()) {
            holder.txtCountdown.setText("Match Started");
            holder.txtCountdown.setVisibility(View.VISIBLE);

            // 🔥 show room details when started
            holder.txtGameId.setVisibility(View.VISIBLE);
            holder.txtGamePassword.setVisibility(View.VISIBLE);
            return;
        }

        holder.txtCountdown.setVisibility(View.VISIBLE);

        holder.timer = new CountDownTimer(
                startTimeMillis - System.currentTimeMillis(),
                1000
        ) {
            @Override
            public void onTick(long millisUntilFinished) {

                long seconds = millisUntilFinished / 1000;

                long hrs = seconds / 3600;
                long mins = (seconds % 3600) / 60;
                long secs = seconds % 60;

                holder.txtCountdown.setText(
                        "Starts in " +
                                String.format("%02d:%02d:%02d", hrs, mins, secs)
                );
            }

            @Override
            public void onFinish() {
                holder.txtCountdown.setText("Match Started");

                // 🔥 reveal room details
                holder.txtGameId.setVisibility(View.VISIBLE);
                holder.txtGamePassword.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    /* ================= VIEW HOLDER ================= */
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtPrize, txtSlots,
                txtTimeLabel, txtDateLabel,
                txtGameId, txtGamePassword,
                txtCountdown;

        MaterialButton btnJoin, btnWinners;
        ProgressBar progressSlots;

        CountDownTimer timer; // 🔥 important

        ViewHolder(View v) {
            super(v);

            txtPrize = v.findViewById(R.id.txtPrize);
            txtSlots = v.findViewById(R.id.txtSlots);
            txtTimeLabel = v.findViewById(R.id.txtTimeLabel);
            txtDateLabel = v.findViewById(R.id.txtDateLabel);
            txtGameId = v.findViewById(R.id.roomid);
            txtGamePassword = v.findViewById(R.id.roomPassword);
            txtCountdown = v.findViewById(R.id.txtCountdown);

            btnJoin = v.findViewById(R.id.btnJoin);
            btnWinners = v.findViewById(R.id.btnWinners);
            progressSlots = v.findViewById(R.id.progSlots);
        }
    }
}