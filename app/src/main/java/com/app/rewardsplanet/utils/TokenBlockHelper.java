package com.app.rewardsplanet.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.app.rewardsplanet.R;

public class TokenBlockHelper {

    /**
     * Creates a horizontal row of digit blocks for a single token (e.g. "A7K2P9X4M8").
     * Each character is placed in an individual rounded box.
     */
    public static LinearLayout createTokenBlockRow(Context context, String token) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        if (token == null) token = "";
        token = token.trim().toUpperCase();

        int boxWidth = (token.length() <= 5) ? dpToPx(context, 30) : dpToPx(context, 24);
        int boxHeight = (token.length() <= 5) ? dpToPx(context, 36) : dpToPx(context, 32);
        int margin = dpToPx(context, 3);

        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);

            TextView box = new TextView(context);
            box.setText(String.valueOf(c));
            box.setTextSize((token.length() <= 5) ? 14f : 13f);
            box.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            box.setTextColor(Color.parseColor("#4F46E5"));
            box.setGravity(Gravity.CENTER);
            box.setBackgroundResource(R.drawable.bg_token_block);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(boxWidth, boxHeight);
            lp.setMargins(margin, 0, margin, 0);
            box.setLayoutParams(lp);
            row.addView(box);
        }

        return row;
    }

    /**
     * Populates a parent vertical LinearLayout with token items, where each token has
     * an optional label and a digit-by-digit block row.
     */
    public static void renderTokenBlocks(LinearLayout parentLayout, java.util.List<String> tokens) {
        if (parentLayout == null) return;
        parentLayout.removeAllViews();

        Context context = parentLayout.getContext();

        if (tokens == null || tokens.isEmpty()) {
            TextView emptyText = new TextView(context);
            emptyText.setText("No tokens available.");
            emptyText.setTextColor(Color.parseColor("#64748B"));
            emptyText.setTextSize(14f);
            emptyText.setGravity(Gravity.CENTER);
            parentLayout.addView(emptyText);
            return;
        }

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);

            if (tokens.size() > 1) {
                TextView label = new TextView(context);
                label.setText("🎟️ Ticket #" + (i + 1));
                label.setTextColor(Color.parseColor("#64748B"));
                label.setTextSize(11f);
                label.setTypeface(null, Typeface.BOLD);
                label.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                labelLp.setMargins(0, i > 0 ? dpToPx(context, 10) : 0, 0, dpToPx(context, 4));
                label.setLayoutParams(labelLp);
                container.addView(label);
            }

            LinearLayout blockRow = createTokenBlockRow(context, token);
            container.addView(blockRow);

            parentLayout.addView(container);
        }
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
