package com.app.rewardsplanet.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.app.rewardsplanet.R;

public class SuccessAnimationHelper {

    private static final String TAG = "SuccessAnimationHelper";

    public static void animate(Dialog dialog) {
        if (dialog == null) return;
        View decorView = (dialog.getWindow() != null) ? dialog.getWindow().getDecorView() : null;
        if (decorView != null) {
            animate(decorView);
        }
    }

    public static void animate(View dialogView) {
        if (dialogView == null) return;

        View successContainer = dialogView.findViewById(R.id.successContainer);
        View successBadge = dialogView.findViewById(R.id.successBadge);
        ImageView checkIcon = dialogView.findViewById(R.id.checkIcon);

        // Safe Haptic Vibration
        triggerVibration(dialogView.getContext());

        // Target primary view to animate
        View targetBadge = (successContainer != null) ? successContainer : successBadge;

        if (targetBadge != null) {
            targetBadge.setScaleX(0f);
            targetBadge.setScaleY(0f);
            targetBadge.setAlpha(0f);

            if (checkIcon != null) {
                checkIcon.setAlpha(0f);
                checkIcon.setScaleX(0.5f);
                checkIcon.setScaleY(0.5f);
            }

            // 1. Overshoot Pop Animation (0 to 1.15)
            targetBadge.animate()
                    .alpha(1f)
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(220)
                    .setInterpolator(new OvershootInterpolator(2.0f))
                    .withEndAction(() -> {
                        // 2. Bounce back to 1.0
                        targetBadge.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .withEndAction(() -> {
                                    if (checkIcon != null) {
                                        checkIcon.animate()
                                                .alpha(1.0f)
                                                .scaleX(1.0f)
                                                .scaleY(1.0f)
                                                .setDuration(160)
                                                .withEndAction(() -> {
                                                    Drawable drawable = checkIcon.getDrawable();
                                                    if (drawable instanceof AnimatedVectorDrawable) {
                                                        ((AnimatedVectorDrawable) drawable).start();
                                                    } else if (drawable instanceof AnimatedVectorDrawableCompat) {
                                                        ((AnimatedVectorDrawableCompat) drawable).start();
                                                    }
                                                })
                                                .start();
                                    }
                                })
                                .start();
                    })
                    .start();
        } else if (checkIcon != null) {
            checkIcon.setAlpha(0f);
            checkIcon.animate().alpha(1f).setDuration(200).start();
        }
    }

    public static void animate(ImageView successBadge, ImageView checkIcon) {
        if (successBadge == null && checkIcon == null) return;

        if (successBadge != null) {
            triggerVibration(successBadge.getContext());
            successBadge.setScaleX(0f);
            successBadge.setScaleY(0f);
            successBadge.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(220)
                    .withEndAction(() -> successBadge.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
        }

        if (checkIcon != null) {
            checkIcon.setAlpha(0f);
            checkIcon.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        Drawable drawable = checkIcon.getDrawable();
                        if (drawable instanceof AnimatedVectorDrawable) {
                            ((AnimatedVectorDrawable) drawable).start();
                        } else if (drawable instanceof AnimatedVectorDrawableCompat) {
                            ((AnimatedVectorDrawableCompat) drawable).start();
                        }
                    })
                    .start();
        }
    }

    /** Safe Vibration Helper to prevent crashes across Android versions */
    public static void triggerVibration(Context context) {
        if (context == null) return;
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //noinspection deprecation
                    v.vibrate(180);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Vibration failed safely: " + e.getMessage());
        }
    }
}
