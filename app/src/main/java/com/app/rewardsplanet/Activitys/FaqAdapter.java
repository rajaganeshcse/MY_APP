package com.app.rewardsplanet.Activitys;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;

import java.util.List;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

    private final List<FaqItem> items;

    public FaqAdapter(List<FaqItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faq, parent, false);
        return new FaqViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        FaqItem item = items.get(position);

        holder.txtFaqCategory.setText(item.getCategory());
        holder.txtFaqQuestion.setText(item.getQuestion());
        holder.txtFaqAnswer.setText(item.getAnswer());

        if (item.isExpanded()) {
            holder.layoutFaqAnswer.setVisibility(View.VISIBLE);
            holder.imgFaqChevron.setImageResource(R.drawable.ic_chevron_up);
        } else {
            holder.layoutFaqAnswer.setVisibility(View.GONE);
            holder.imgFaqChevron.setImageResource(R.drawable.ic_chevron_down);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                item.toggleExpanded();
                notifyItemChanged(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class FaqViewHolder extends RecyclerView.ViewHolder {
        TextView txtFaqCategory, txtFaqQuestion, txtFaqAnswer;
        ImageView imgFaqChevron;
        LinearLayout layoutFaqAnswer;

        FaqViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFaqCategory = itemView.findViewById(R.id.txtFaqCategory);
            txtFaqQuestion = itemView.findViewById(R.id.txtFaqQuestion);
            txtFaqAnswer = itemView.findViewById(R.id.txtFaqAnswer);
            imgFaqChevron = itemView.findViewById(R.id.imgFaqChevron);
            layoutFaqAnswer = itemView.findViewById(R.id.layoutFaqAnswer);
        }
    }
}
