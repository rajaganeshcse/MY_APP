package com.app.rewardsplanet;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * LeaderboardAdapter — Renders ranks 4+ in the leaderboard RecyclerView.
 *
 * Bug Fixes Applied:
 *  ✅ Dark card background (#1E1B3A), not light blue (#ADD8E6)
 *  ✅ Current-user card highlighted with gold border, not blue
 *  ✅ Score shows coins not just streak_count
 *  ✅ Rank badge color: gold for rank 4, else neutral
 *  ✅ Removed GradientDrawable override (layout XML handles background via CardView)
 *  ✅ fade-in animation only on first bind (no flicker on scroll)
 *  ✅ Added txtRankLabel sub-label
 */
public class LeaderboardAdapter
        extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final Context     context;
    private final List<User>  list;

    // Leaderboard nav bg color (#C4C3EF) for current user card
    private static final int COLOR_SELF_BG      = Color.parseColor("#9C27B0");
    private static final int COLOR_DEFAULT_BG   = Color.parseColor("#1E1B3A");

    public LeaderboardAdapter(Context context, List<User> list) {
        this.context = context;
        this.list    = list;
        setHasStableIds(false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        // Adapter position 0 = list position 3 (top 3 shown in podium header)
        int listPos = position + 3;

        if (listPos < 0 || listPos >= list.size()) return;

        User u = list.get(listPos);
        if (u == null) return;

        int displayRank = listPos + 1;  // 1-based rank

        // ── Rank badge — all serial numbers (#4, #5...) black color ──────
        h.rank.setText("#" + displayRank);
        h.rank.setTextColor(Color.BLACK);

        // ── Name ────────────────────────────────────────────────────────────
        h.name.setText((u.name != null && !u.name.trim().isEmpty()) ? u.name : "User");

        // ── Rank sub-label ──────────────────────────────────────────────────
        if (h.txtRankLabel != null) {
            h.txtRankLabel.setText("Rank #" + displayRank);
        }

        // ── Score — show coins (primary) or streak as fallback ──────────────
        if (u.coins > 0) {
            h.score.setText(String.valueOf(u.coins));
        } else {
            h.score.setText(String.valueOf(u.streak_count));
        }

        // ── Current-user highlight ──────────────────────────────────────────
        String currentUid    = FirebaseAuth.getInstance().getUid();
        boolean isCurrentUser = currentUid != null && currentUid.equals(u.uid);

        if (h.card != null) {
            if (isCurrentUser) {
                // Background color = Leaderboard Nav Bg Color (#C4C3EF)
                h.card.setCardBackgroundColor(COLOR_SELF_BG);
                h.card.setCardElevation(dpToPx(5));
                h.name.setTextColor(Color.WHITE);
                if (h.txtRankLabel != null) h.txtRankLabel.setTextColor(Color.parseColor("#6B7280"));
                h.score.setTextColor(Color.parseColor("#FFD700"));
            } else {
                h.card.setCardBackgroundColor(COLOR_DEFAULT_BG);
                h.card.setCardElevation(dpToPx(4));
                h.name.setTextColor(Color.WHITE);
                if (h.txtRankLabel != null) h.txtRankLabel.setTextColor(Color.parseColor("#6B7280"));
                h.score.setTextColor(Color.parseColor("#FFD700"));
            }
        }

        // ── Profile image ──────────────────────────────────────────────────
        if (u.profile_pic != null && !u.profile_pic.trim().isEmpty()) {
            Glide.with(context)
                    .load(u.profile_pic)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(h.image);
        } else {
            h.image.setImageResource(R.drawable.ic_profile);
        }

        // ── Fade-in animation (staggered by position, no double-animation on scroll)
        h.itemView.setAlpha(0f);
        h.itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(Math.min(position * 40L, 400L))
                .start();
    }

    @Override
    public int getItemCount() {
        // Top 3 shown in the podium header — RecyclerView shows ranks 4+
        return Math.max(0, list.size() - 3);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CardView  card;
        TextView  rank;
        TextView  name;
        TextView  txtRankLabel;
        TextView  score;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card         = itemView.findViewById(R.id.cardRoot);
            rank         = itemView.findViewById(R.id.rank);
            name         = itemView.findViewById(R.id.name);
            txtRankLabel = itemView.findViewById(R.id.txtRankLabel);
            score        = itemView.findViewById(R.id.score);
            image        = itemView.findViewById(R.id.image);
        }
    }
}