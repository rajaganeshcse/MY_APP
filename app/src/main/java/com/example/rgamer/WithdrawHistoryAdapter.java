package com.example.rgamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        // -------- TEXT --------
        holder.txtType.setText(model.getType());
        holder.txtAmount.setText(model.getAmount());
        holder.txtStatus.setText(model.getStatus());

        // -------- STATUS UI --------
        switch (model.getStatus().toLowerCase()) {
            case "success":
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_approved);
                break;

            case "pending":
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_rejected);
                break;

            default:
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);
                break;
        }

        // -------- METHOD ICON --------
        setMethodIcon(holder.imgMethod, model.getType());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(model));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    /* ================= ICON LOGIC ================= */
    private void setMethodIcon(ImageView img, String type) {

        if (type == null) {
            img.setImageResource(R.drawable.wallet_icon);
            return;
        }

        switch (type.toLowerCase()) {
            case "google":
                img.setImageResource(R.drawable.ic_google_play);
                break;

            case "amazon":
                img.setImageResource(R.drawable.ic_amazon);
                break;

            case "phonepe":
                img.setImageResource(R.drawable.ic_phonepe);
                break;

            case "upi":
                img.setImageResource(R.drawable.ic_upi);
                break;

            case "bank":
                img.setImageResource(R.drawable.ic_bank);
                break;

            default:
                img.setImageResource(R.drawable.wallet_icon);
                break;
        }
    }

    /* ================= VIEW HOLDER ================= */
    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMethod;
        TextView txtType, txtAmount, txtStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            imgMethod = itemView.findViewById(R.id.imgMethod);
            txtType = itemView.findViewById(R.id.txtType);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}
