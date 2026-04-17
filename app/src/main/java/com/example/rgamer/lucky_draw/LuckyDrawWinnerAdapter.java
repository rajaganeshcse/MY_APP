package com.example.rgamer.lucky_draw;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rgamer.R;
import com.example.rgamer.models.LuckyDrawModel;

import java.util.List;

public class LuckyDrawWinnerAdapter
        extends RecyclerView.Adapter<LuckyDrawWinnerAdapter.VH> {

    private final List<LuckyDrawModel> list;


    public LuckyDrawWinnerAdapter(List<LuckyDrawModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lucky_draw_winner, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {

        LuckyDrawModel model = list.get(position);

        h.txtReward.setText("Reward: " + model.getRewardCoins() + " Coins");
        h.txtWinner.setText("Winner UID: " + model.getWinnerUid());
        h.txtCompletedAt.setText(
                "Completed at: " + model.getCompletedAtFormatted()
        );
        h.txtStatus.setText("COMPLETED");
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView txtReward, txtWinner, txtCompletedAt, txtStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            txtReward = itemView.findViewById(R.id.txtReward);
            txtWinner = itemView.findViewById(R.id.txtWinner);
            txtCompletedAt = itemView.findViewById(R.id.txtCompletedAt);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}
