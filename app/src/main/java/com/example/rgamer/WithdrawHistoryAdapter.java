package com.example.rgamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WithdrawHistoryAdapter
        extends RecyclerView.Adapter<WithdrawHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(WithdrawHistoryModel model);
    }

    private List<WithdrawHistoryModel> list;
    private OnItemClickListener listener;

    public WithdrawHistoryAdapter(List<WithdrawHistoryModel> list,
                                  OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdraw_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WithdrawHistoryModel model = list.get(position);

        holder.txtType.setText(model.getType());
        holder.txtAmount.setText(model.getAmount());
        holder.txtStatus.setText(model.getStatus());

        switch (model.getStatus()) {
            case "success":
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_approved);
                break;
            case "pending":
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_rejected);
                break;
            default:
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(model));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtType, txtAmount, txtStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtType = itemView.findViewById(R.id.txtType);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}
