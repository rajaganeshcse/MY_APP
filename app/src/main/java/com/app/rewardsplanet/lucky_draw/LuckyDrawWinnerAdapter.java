package com.app.rewardsplanet.lucky_draw;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.models.LuckyDrawModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LuckyDrawWinnerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class WinnerListItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_ITEM = 1;

        public final int type;
        public final String headerTitle;
        public final LuckyDrawModel model;

        public WinnerListItem(String headerTitle) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
            this.model = null;
        }

        public WinnerListItem(LuckyDrawModel model) {
            this.type = TYPE_ITEM;
            this.headerTitle = null;
            this.model = model;
        }
    }

    private final List<WinnerListItem> items = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, String> userCache = new HashMap<>();

    public LuckyDrawWinnerAdapter(List<LuckyDrawModel> drawList) {
        setDrawList(drawList);
    }

    public void setDrawList(List<LuckyDrawModel> drawList) {
        items.clear();
        if (drawList == null || drawList.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        String currentGroup = null;
        for (LuckyDrawModel m : drawList) {
            Timestamp ts = m.getCompletedAt() != null ? m.getCompletedAt() : m.getCreatedAt();
            String groupLabel = getGroupDateLabel(ts);

            if (currentGroup == null || !currentGroup.equals(groupLabel)) {
                currentGroup = groupLabel;
                items.add(new WinnerListItem(groupLabel));
            }
            items.add(new WinnerListItem(m));
        }
        notifyDataSetChanged();
    }

    public static String getGroupDateLabel(Timestamp timestamp) {
        if (timestamp == null) return "OLDER";
        Date date = timestamp.toDate();

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        Calendar nowDay = (Calendar) now.clone();
        nowDay.set(Calendar.HOUR_OF_DAY, 0);
        nowDay.set(Calendar.MINUTE, 0);
        nowDay.set(Calendar.SECOND, 0);
        nowDay.set(Calendar.MILLISECOND, 0);

        Calendar targetDay = (Calendar) target.clone();
        targetDay.set(Calendar.HOUR_OF_DAY, 0);
        targetDay.set(Calendar.MINUTE, 0);
        targetDay.set(Calendar.SECOND, 0);
        targetDay.set(Calendar.MILLISECOND, 0);

        long diffDays = (nowDay.getTimeInMillis() - targetDay.getTimeInMillis()) / (24 * 60 * 60 * 1000L);

        if (diffDays == 0) {
            return "TODAY";
        } else if (diffDays == 1) {
            return "YESTERDAY";
        } else {
            return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date).toUpperCase();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == WinnerListItem.TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_lucky_draw_winner, parent, false);
            return new WinnerVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        WinnerListItem item = items.get(position);

        if (holder instanceof HeaderVH) {
            HeaderVH h = (HeaderVH) holder;
            if (h.headerText != null) {
                h.headerText.setText(item.headerTitle);
            }
        } else if (holder instanceof WinnerVH) {
            WinnerVH h = (WinnerVH) holder;
            LuckyDrawModel model = item.model;
            if (model == null) return;

            // 1. Reward Coins
            if (h.txtReward != null) {
                h.txtReward.setText(model.getRewardCoins() + " Coins");
            }

            // 2. Winning Token
            String token = model.getWinningToken();
            if (token != null && !token.isEmpty()) {
                if (h.layoutTokenContainer != null) h.layoutTokenContainer.setVisibility(View.VISIBLE);
                if (h.txtWinningToken != null) h.txtWinningToken.setText("Token: " + token);
            } else {
                if (h.layoutTokenContainer != null) h.layoutTokenContainer.setVisibility(View.GONE);
            }

            // 3. Winner Name
            String uid = model.getWinnerUid();
            String predefinedName = model.getWinnerName();

            if (predefinedName != null && !predefinedName.isEmpty()) {
                if (h.txtWinner != null) h.txtWinner.setText("👤 Winner: " + predefinedName);
            } else if (uid == null || uid.isEmpty()) {
                if (h.txtWinner != null) h.txtWinner.setText("👤 Winner: Unknown");
            } else if (userCache.containsKey(uid)) {
                if (h.txtWinner != null) h.txtWinner.setText("👤 Winner: " + userCache.get(uid));
            } else {
                if (h.txtWinner != null) h.txtWinner.setText("👤 Winner: Loading...");
                db.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            String name = "User_" + uid.substring(0, Math.min(6, uid.length()));
                            if (snapshot.exists()) {
                                String n = snapshot.getString("name");
                                if (n == null || n.isEmpty()) {
                                    n = snapshot.getString("username");
                                }
                                if (n == null || n.isEmpty()) {
                                    n = snapshot.getString("email");
                                }
                                if (n != null && !n.isEmpty()) {
                                    name = n;
                                }
                            }

                            userCache.put(uid, name);

                            int adapterPos = h.getAdapterPosition();
                            if (adapterPos != RecyclerView.NO_POSITION && adapterPos == position) {
                                if (h.txtWinner != null) h.txtWinner.setText("👤 Winner: " + name);
                            }
                        })
                        .addOnFailureListener(e -> {
                            String fallback = "User_" + uid.substring(0, Math.min(6, uid.length()));
                            userCache.put(uid, fallback);
                            if (h.txtWinner != null) h.txtWinner.setText("👤 Winner: " + fallback);
                        });
            }

            // 4. Date & Time
            if (h.txtCompletedAt != null) {
                h.txtCompletedAt.setText("📅 Completed at: " + model.getCompletedAtFormatted());
            }

            // 5. Status
            if (h.txtStatus != null) {
                String status = model.getStatus();
                h.txtStatus.setText("COMPLETED");
                if ("CLOSED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                    h.txtStatus.setBackgroundResource(R.drawable.bg_status_success);
                } else {
                    h.txtStatus.setBackgroundColor(Color.GRAY);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView headerText;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }

    static class WinnerVH extends RecyclerView.ViewHolder {
        TextView txtReward, txtWinner, txtWinningToken, txtCompletedAt, txtStatus;
        View layoutTokenContainer;

        WinnerVH(@NonNull View itemView) {
            super(itemView);
            txtReward = itemView.findViewById(R.id.txtReward);
            txtWinner = itemView.findViewById(R.id.txtWinner);
            txtWinningToken = itemView.findViewById(R.id.txtWinningToken);
            layoutTokenContainer = itemView.findViewById(R.id.layoutTokenContainer);
            txtCompletedAt = itemView.findViewById(R.id.txtCompletedAt);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}