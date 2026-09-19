package com.app.rewardsplanet.utils;

import android.app.Dialog;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.app.rewardsplanet.R;

public class SuccessAnimationHelper {

    public static void animate(Dialog dialog) {
        if (dialog == null) return;

        ImageView successBadge = dialog.findViewById(R.id.successBadge);
        ImageView checkIcon = dialog.findViewById(R.id.checkIcon);

        animate(successBadge, checkIcon);
    }

    public static void animate(View dialogView) {
        if (dialogView == null) return;

        ImageView successBadge = dialogView.findViewById(R.id.successBadge);
        ImageView checkIcon = dialogView.findViewById(R.id.checkIcon);

        animate(successBadge, checkIcon);
    }

    public static void animate(ImageView successBadge, ImageView checkIcon) {
        if (successBadge == null || checkIcon == null) return;

        // Reset state
        successBadge.setScaleX(0f);
        successBadge.setScaleY(0f);
        checkIcon.setAlpha(0f);

        // 1. Badge Pop Animation (0 to 1.15)
        successBadge.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(180)
                .withEndAction(() -> {

                    // 2. Small Bounce Back (1.15 to 1.0)
                    successBadge.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .withEndAction(() -> {

                                // 3. Fade in check icon & draw vector path
                                checkIcon.setAlpha(1f);

                                Drawable drawable = checkIcon.getDrawable();
                                if (drawable instanceof AnimatedVectorDrawable) {
                                    ((AnimatedVectorDrawable) drawable).start();
                                } else if (drawable instanceof AnimatedVectorDrawableCompat) {
                                    ((AnimatedVectorDrawableCompat) drawable).start();
                                }
                            })
                            .start();
                })
                .start();
    }
}
