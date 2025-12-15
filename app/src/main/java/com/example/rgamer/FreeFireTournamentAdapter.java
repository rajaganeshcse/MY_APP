package com.example.rgamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.FreeFireTournamentModel;

import java.util.List;

public class FreeFireTournamentAdapter
        extends RecyclerView.Adapter<FreeFireTournamentAdapter.ViewHolder> {

    public interface OnJoinClick {
        void onJoin(FreeFireTournamentModel model);
    }

    Context context;
    List<FreeFireTournamentModel> list;
    OnJoinClick listener;

    public FreeFireTournamentAdapter(Context context,
                                     List<FreeFireTournamentModel> list,
                                     OnJoinClick listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_freefire_tournament, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        FreeFireTournamentModel m = list.get(position);

        h.txtPrize.setText("Win " + m.getPrizeCoins() + " Coins");
        h.txtSlots.setText("Slots Left : "
                + (m.getTotalSlots() - m.getJoinedSlots())
                + "/" + m.getTotalSlots());

        int percent = (m.getJoinedSlots() * 100) / m.getTotalSlots();
        h.progress.setProgress(percent);

        h.btnJoin.setText("Join (" + m.getEntryTickets() + " Tickets)");

        h.btnJoin.setOnClickListener(v -> listener.onJoin(m));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtPrize, txtSlots, btnJoin;
        ProgressBar progress;

        ViewHolder(View v) {
            super(v);
            txtPrize = v.findViewById(R.id.txtPrize);
            txtSlots = v.findViewById(R.id.txtSlots);
            btnJoin = v.findViewById(R.id.btnJoin);
            progress = v.findViewById(R.id.progressSlots);
        }
    }
}
