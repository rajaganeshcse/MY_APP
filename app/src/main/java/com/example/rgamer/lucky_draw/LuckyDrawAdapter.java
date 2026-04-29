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
        h.txtSlots.setText("Filled : " + filled + "/" + total);
        h.txtPercent.setText((filled * 100 / total) + "% Filled");

        h.progressSlots.setMax(total);
        h.progressSlots.setProgress(filled);

        int totalJoined = model.getMyTicketsCount() + (model.isAdJoined() ? 1 : 0);
        h.token.setText(totalJoined + " Joined 👍");
        if(totalJoined>0){
            h.joined.setVisibility(View.VISIBLE);
        }else{
            h.joined.setVisibility(View.GONE);
        }

        /* RESET */
        h.btnJoin.setEnabled(true);
        h.btnticket.setEnabled(true);

        /* AD STATE */
        if (model.isAdJoined()) {
            h.btnJoin.setText("Used");
            h.btnJoin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0974f1")));
            h.btnJoin.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#0974f1")));
            h.btnJoin.setEnabled(false);
        } else {
            h.btnJoin.setText("Free");
            h.btnJoin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2a8d08")));
            h.btnJoin.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2a8d08")));

        }

        /* FULL */
        if (model.isFull() || !"OPEN".equals(model.getStatus())) {
            h.btnJoin.setText("FULL");
            h.btnJoin.setEnabled(false);
            h.btnticket.setEnabled(false);
            h.btnJoin.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#e01f2d")));
            h.btnticket.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#e01f2d")));
            h.btnticket.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#e01f2d")));
        }

        /* TICKET COUNT UI */
        h.btnticket.setText("1 Tickets");

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

        TextView txtReward, txtSlots, txtPercent,token;
        ProgressBar progressSlots;
        LinearLayout joined;
        MaterialButton btnJoin, btnticket;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtReward = itemView.findViewById(R.id.txtReward);
            txtSlots = itemView.findViewById(R.id.txtSlots);
            txtPercent = itemView.findViewById(R.id.txtPercent);
            progressSlots = itemView.findViewById(R.id.progressSlots);
            joined=itemView.findViewById(R.id.joined);
            btnJoin = itemView.findViewById(R.id.btnFreeEntry);
            token=itemView.findViewById(R.id.token);
            btnticket = itemView.findViewById(R.id.btnticketEntry);
        }
    }
}