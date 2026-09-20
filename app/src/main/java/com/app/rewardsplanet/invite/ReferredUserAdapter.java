package com.app.rewardsplanet.invite;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.models.ReferredUserModel;

import java.util.Date;
import java.util.List;

public class ReferredUserAdapter extends RecyclerView.Adapter<ReferredUserAdapter.ViewHolder> {

    private final List<ReferredUserModel> list;

    public ReferredUserAdapter(List<ReferredUserModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_referred_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReferredUserModel item = list.get(position);

        String name = item.getName();
        holder.txtUserName.setText(name);

        String initial = "U";
        if (name != null && !name.trim().isEmpty()) {
            initial = name.trim().substring(0, 1).toUpperCase();
        }
        holder.txtAvatarInitial.setText(initial);

        if (item.getJoinedAt() > 0) {
            String dateStr = DateFormat.format("dd MMM yyyy", new Date(item.getJoinedAt())).toString();
            holder.txtJoinedDate.setText("Joined: " + dateStr);
        } else {
            holder.txtJoinedDate.setText("Joined via Referral");
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAvatarInitial, txtUserName, txtJoinedDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAvatarInitial = itemView.findViewById(R.id.txtAvatarInitial);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtJoinedDate = itemView.findViewById(R.id.txtJoinedDate);
        }
    }
}
