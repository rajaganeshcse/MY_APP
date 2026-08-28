package com.app.rewardsplanet.Game;

import android.content.Context;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
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

    public FreeFireTournamentAdapter(Context context,
                                     List<FreeFireTournamentModel> list,
                                     Listener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_freefire_tournament, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        FreeFireTournamentModel m = list.get(position);

        h.txtPrize.setText("Win " + m.getCoin() + " Coins");

        // TIME
        if (m.getStartTimeMillis() > 0) {
            Date d = new Date(m.getStartTimeMillis());

            h.txtTimeLabel.setText(
                    new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(d));

            h.txtDateLabel.setText(
                    new SimpleDateFormat("dd MMM", Locale.getDefault()).format(d));
        }

        // COUNTDOWN
        startCountdown(h, m.getStartTimeMillis());

        // SLOTS
        int total = m.getTotalSlots();
        int joined = m.getJoinedSlots();
        int left = Math.max(0, total - joined);

        h.txtSlots.setText("Slots Left : " + left + "/" + total);

        h.progressSlots.setProgress(
                total > 0 ? (joined * 100) / total : 0);

        // RESET VISIBILITY
        h.txtGameId.setVisibility(View.GONE);
        h.txtGamePassword.setVisibility(View.GONE);

        // JOIN STATE
        if (m.isJoined()) {

            h.btnJoin.setEnabled(false);
            h.btnJoin.setText("Joined");

            // 🔥 SHOW ROOM ID
            if (m.getRoomId() != null) {
                h.txtGameId.setText("Room ID : " + m.getRoomId());
                h.txtGameId.setVisibility(View.VISIBLE);
            }

            // 🔥 SHOW PASSWORD
            if (m.getRoomPassword() != null) {
                h.txtGamePassword.setText("Password : " + m.getRoomPassword());
            } else {
                h.txtGamePassword.setText("Password : Not Available");
            }

            h.txtGamePassword.setVisibility(View.VISIBLE);

        } else if (left <= 0) {

            h.btnJoin.setEnabled(false);
            h.btnJoin.setText("Full");

        } else {

            h.btnJoin.setText("Join (" + m.getEntryTickets() + " Tickets)");
            h.btnJoin.setOnClickListener(v -> listener.onJoin(m));
        }

        h.btnWinners.setOnClickListener(v -> listener.onCheckWinners(m));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ================= COUNTDOWN =================
    private void startCountdown(ViewHolder h, long startTime) {

        if (h.timer != null) h.timer.cancel();

        if (startTime <= System.currentTimeMillis()) {
            h.txtCountdown.setText("Match Started");
            h.txtCountdown.setVisibility(View.VISIBLE);
            return;
        }

        h.txtCountdown.setVisibility(View.VISIBLE);

        h.timer = new CountDownTimer(
                startTime - System.currentTimeMillis(), 1000) {

            public void onTick(long ms) {
                long s = ms / 1000;
                long h1 = s / 3600;
                long m = (s % 3600) / 60;
                long s1 = s % 60;

                h.txtCountdown.setText(
                        "Starts in " + String.format("%02d:%02d:%02d", h1, m, s1));
            }

            public void onFinish() {
                h.txtCountdown.setText("Match Started");
            }
        }.start();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtPrize, txtSlots, txtTimeLabel,
                txtDateLabel, txtGameId, txtGamePassword, txtCountdown;

        MaterialButton btnJoin, btnWinners;
        ProgressBar progressSlots;

        CountDownTimer timer;

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