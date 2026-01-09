package com.example.rgamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JoinedPlayersAdapter
        extends RecyclerView.Adapter<JoinedPlayersAdapter.ViewHolder> {

    private final List<FreeFireTournamentModel.JoinedUser> list;

    // 🔥 Convert MAP → LIST
    public JoinedPlayersAdapter(
            Map<String, FreeFireTournamentModel.JoinedUser> joinedUsers) {

        if (joinedUsers == null) {
            this.list = new ArrayList<>();
        } else {
            this.list = new ArrayList<>(joinedUsers.values());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_joined_player, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        FreeFireTournamentModel.JoinedUser user = list.get(position);

        holder.txtName.setText(user.getUsername());
        holder.txtGameId.setText("Game ID : " + user.getGameId());
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    /* ================= VIEW HOLDER ================= */
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtName, txtGameId;

        ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtGameId = itemView.findViewById(R.id.txtGameId);
        }
    }
}
