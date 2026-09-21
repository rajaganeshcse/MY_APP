package com.app.rewardsplanet.withdraws;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class WithdrawHistoryAdapter
        extends RecyclerView.Adapter<WithdrawHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(WithdrawHistoryModel model);
    }

    private final List<WithdrawHistoryModel> list;
    private final OnItemClickListener listener;

    public WithdrawHistoryAdapter(
            List<WithdrawHistoryModel> list,
            OnItemClickListener listener
    ) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdraw_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        WithdrawHistoryModel model = list.get(position);

        /* ================= TYPE ================= */
        String type = model.getType();
        holder.txtType.setText(formatTypeTitle(type));

        String date = model.getFormattedDate();
        holder.txtDate.setText(date != null && !date.isEmpty() ? date : "Recent Request");

        /* ================= AMOUNT ================= */
        NumberFormat format = NumberFormat.getInstance(new Locale("en", "IN"));
        holder.txtAmount.setText("₹" + format.format(model.getAmount()));

        /* ================= STATUS ================= */
        String status = model.getStatus() != null ? model.getStatus().toLowerCase() : "pending";

        switch (status) {
            case "success":
            case "approved":
            case "completed":
                holder.txtStatus.setText("SUCCESS");
                holder.txtStatus.setTextColor(Color.parseColor("#03543F"));
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_approved);
                break;

            case "pending":
                holder.txtStatus.setText("PENDING");
                holder.txtStatus.setTextColor(Color.parseColor("#92400E"));
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;

            default:
                holder.txtStatus.setText("REJECTED");
                holder.txtStatus.setTextColor(Color.parseColor("#9B1C1C"));
                holder.txtStatus.setBackgroundResource(R.drawable.bg_status_failed);
                break;
        }

        /* ================= ICON ================= */
        setMethodIcon(holder.imgMethod, type);

        /* ================= CLICK ================= */
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    private String formatTypeTitle(String type) {
        if (type == null) return "Reward Payout";
        switch (type.toLowerCase()) {
            case "google":
            case "google_play":
            case "googleplay":
                return "Google Play Voucher";
            case "amazon": return "Amazon Gift Voucher";
            case "phonepe": return "PhonePe Voucher";
            case "upi": return "UPI Cash Transfer";
            case "bank": return "Direct Bank Payout";
            default: return capitalize(type) + " Payout";
        }
    }

    /* ================= ICON LOGIC ================= */
    private void setMethodIcon(ImageView img, String type) {
        if (type == null) {
            img.setImageResource(R.drawable.wallet_icon);
            return;
        }

        switch (type.toLowerCase()) {
            case "google":
            case "google_play":
            case "googleplay":
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
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /* ================= VIEW HOLDER ================= */
    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgMethod;
        TextView txtType, txtAmount, txtStatus, txtDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMethod = itemView.findViewById(R.id.imgMethod);
            txtType = itemView.findViewById(R.id.txtType);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }
}

