package com.app.rewardsplanet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class LeaderboardAdapter
        extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final Context context;
    private final List<User> list;

    public LeaderboardAdapter(
            Context context,
            List<User> list
    ) {
        this.context = context;
        this.list = list;
    }


    // =========================================================
    // CREATE VIEW HOLDER
    // =========================================================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View v = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_row,
                        parent,
                        false
                );

        return new ViewHolder(v);
    }


    // =========================================================
    // BIND VIEW HOLDER
    // =========================================================

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder h,
            int position
    ) {

        // -----------------------------------------------------
        // Recycler position starts from 0.
        // First 3 users are already shown in Top 3.
        // Therefore actual list position = position + 3.
        // -----------------------------------------------------

        int actualPosition = position + 3;


        // -----------------------------------------------------
        // SAFETY CHECK
        // -----------------------------------------------------

        if (actualPosition < 0 ||
                actualPosition >= list.size()) {

            return;
        }


        User u = list.get(actualPosition);


        if (u == null) {
            return;
        }


        // =====================================================
        // RANK
        // =====================================================

        h.rank.setText(
                "#" + (actualPosition + 1)
        );


        // =====================================================
        // NAME
        // =====================================================

        if (u.name != null &&
                !u.name.trim().isEmpty()) {

            h.name.setText(u.name);

        } else {

            h.name.setText("User");
        }


        // =====================================================
        // SCORE
        // =====================================================

        h.score.setText(
                "🔥 " + u.streak_count
        );


        // =====================================================
        // ITEM BORDER + BACKGROUND
        // =====================================================

        String currentUid =
                FirebaseAuth
                        .getInstance()
                        .getUid();


        boolean isCurrentUser =
                currentUid != null &&
                        currentUid.equals(u.uid);


        GradientDrawable background =
                new GradientDrawable();


        // Keep rounded corners
        background.setCornerRadius(
                12 * context.getResources()
                        .getDisplayMetrics().density
        );


        // Keep border
        background.setStroke(
                dpToPx(1),
                Color.parseColor("#5DA9D6")
        );


        // Different background for current user
        if (isCurrentUser) {

            background.setColor(
                    Color.parseColor("#87CEFA")
            );

        } else {

            background.setColor(
                    Color.parseColor("#ADD8E6")
            );
        }


        h.itemView.setBackground(
                background
        );


        // =====================================================
        // PROFILE IMAGE
        // =====================================================

        if (u.profile_pic != null &&
                !u.profile_pic.trim().isEmpty()) {

            Glide.with(context)
                    .load(u.profile_pic)
                    .placeholder(
                            R.drawable.ic_profile
                    )
                    .error(
                            R.drawable.ic_profile
                    )
                    .circleCrop()
                    .into(h.image);

        } else {

            h.image.setImageResource(
                    R.drawable.ic_profile
            );
        }


        // =====================================================
        // FADE-IN ANIMATION
        // =====================================================

        h.itemView.setAlpha(0f);

        h.itemView.animate()
                .alpha(1f)
                .setDuration(400)
                .start();
    }


    // =========================================================
    // DP TO PX
    // =========================================================

    private int dpToPx(int dp) {

        float density =
                context.getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(dp * density);
    }


    // =========================================================
    // ITEM COUNT
    // =========================================================

    @Override
    public int getItemCount() {

        // -----------------------------------------------------
        // Top 3 are displayed separately.
        // RecyclerView displays remaining users.
        // -----------------------------------------------------

        if (list.size() <= 3) {
            return 0;
        }

        return list.size() - 3;
    }


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView rank;
        TextView name;
        TextView score;

        ImageView image;


        public ViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            rank = itemView.findViewById(
                    R.id.rank
            );


            name = itemView.findViewById(
                    R.id.name
            );


            score = itemView.findViewById(
                    R.id.score
            );


            image = itemView.findViewById(
                    R.id.image
            );
        }
    }
}