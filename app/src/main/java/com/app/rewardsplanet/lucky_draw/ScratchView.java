package com.app.rewardsplanet.lucky_draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.app.rewardsplanet.R;

public class ScratchView extends View {

    private Bitmap scratchBitmap;
    private Canvas scratchCanvas;

    private Paint scratchPaint;
    private Paint textPaint;
    private Paint cardPaint;
    private Paint borderPaint;

    private Path scratchPath;

    private float lastX;
    private float lastY;

    private boolean isScratchEnabled = true;
    private boolean isRevealed = false;
    private boolean hasStartedScratching = false;

    /*
     * IMPORTANT:
     *
     * Reward is NOT generated here.
     * Backend sends the reward.
     */
    private int reward = 0;

    private ScratchListener scratchListener;

    public interface ScratchListener {
        void onScratchComplete();
    }

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public ScratchView(Context context) {
        super(context);
        init();
    }

    public ScratchView(
            Context context,
            @Nullable AttributeSet attrs) {

        super(context, attrs);
        init();
    }

    public ScratchView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init();
    }

    // ============================================================
    // INIT
    // ============================================================

    private void init() {

        setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
        );

        // Scratch brush
        scratchPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        scratchPaint.setStyle(
                Paint.Style.STROKE
        );

        scratchPaint.setStrokeCap(
                Paint.Cap.ROUND
        );

        scratchPaint.setStrokeJoin(
                Paint.Join.ROUND
        );

        scratchPaint.setStrokeWidth(
                dp(100)
        );

        scratchPaint.setXfermode(
                new PorterDuffXfermode(
                        PorterDuff.Mode.CLEAR
                )
        );

        // Path
        scratchPath =
                new Path();

        // Text
        textPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        // Card
        cardPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        // Border
        borderPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        borderPaint.setStyle(
                Paint.Style.STROKE
        );

        borderPaint.setStrokeWidth(
                dp(4)
        );

        setFocusable(true);
    }

    // ============================================================
    // SIZE CHANGED
    // ============================================================

    @Override
    protected void onSizeChanged(
            int w,
            int h,
            int oldw,
            int oldh) {

        super.onSizeChanged(
                w,
                h,
                oldw,
                oldh
        );

        if (w <= 0 || h <= 0) {
            return;
        }

        createScratchCard();
    }

    // ============================================================
    // CREATE SCRATCH CARD
    // ============================================================

    private void createScratchCard() {

        if (getWidth() <= 0 ||
                getHeight() <= 0) {
            return;
        }

        if (scratchBitmap != null &&
                !scratchBitmap.isRecycled()) {

            scratchBitmap.recycle();
        }

        scratchBitmap =
                Bitmap.createBitmap(
                        getWidth(),
                        getHeight(),
                        Bitmap.Config.ARGB_8888
                );

        scratchCanvas =
                new Canvas(scratchBitmap);

        // Clear
        scratchCanvas.drawColor(
                Color.TRANSPARENT,
                PorterDuff.Mode.CLEAR
        );

        // ========================================================
        // SCRATCH COVER
        // ========================================================

        cardPaint.setStyle(
                Paint.Style.FILL
        );

        cardPaint.setColor(
                Color.rgb(255, 215, 0)
        );

        RectF rect =
                new RectF(
                        0,
                        0,
                        getWidth(),
                        getHeight()
                );

        scratchCanvas.drawRoundRect(
                rect,
                dp(20),
                dp(20),
                cardPaint
        );

        // ========================================================
        // SCRATCH HERE TEXT
        // ========================================================

        textPaint.setColor(
                Color.BLACK
        );

        textPaint.setTextSize(
                dp(18)
        );

        textPaint.setTextAlign(
                Paint.Align.CENTER
        );

        textPaint.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        Paint.FontMetrics fm =
                textPaint.getFontMetrics();

        float textY =
                getHeight() / 2f
                        - (fm.ascent + fm.descent) / 2f;

        scratchCanvas.drawText(
                "SCRATCH HERE",
                getWidth() / 2f,
                textY,
                textPaint
        );

        // ========================================================
        // BORDER
        // ========================================================

        borderPaint.setColor(
                Color.rgb(230, 0, 120)
        );

        scratchCanvas.drawRoundRect(
                rect.left + dp(2),
                rect.top + dp(2),
                rect.right - dp(2),
                rect.bottom - dp(2),
                dp(20),
                dp(20),
                borderPaint
        );

        isRevealed = false;
        hasStartedScratching = false;
    }

    // ============================================================
    // DRAW
    // ============================================================

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (scratchBitmap != null &&
                !scratchBitmap.isRecycled()) {

            canvas.drawBitmap(
                    scratchBitmap,
                    0,
                    0,
                    null
            );
        }
    }

    // ============================================================
    // TOUCH
    // ============================================================

    @Override
    public boolean onTouchEvent(
            MotionEvent event) {

        if (!isScratchEnabled) {
            return true;
        }

        if (isRevealed) {
            return true;
        }

        switch (event.getActionMasked()) {

            // ----------------------------------------------------
            // DOWN
            // ----------------------------------------------------

            case MotionEvent.ACTION_DOWN:

                lastX =
                        event.getX();

                lastY =
                        event.getY();

                hasStartedScratching =
                        true;

                scratchPath.reset();

                scratchPath.moveTo(
                        lastX,
                        lastY
                );

                invalidate();

                return true;

            // ----------------------------------------------------
            // MOVE
            // ----------------------------------------------------

            case MotionEvent.ACTION_MOVE:

                if (!hasStartedScratching) {
                    return true;
                }

                float x =
                        event.getX();

                float y =
                        event.getY();

                scratchPath.quadTo(
                        lastX,
                        lastY,
                        (lastX + x) / 2f,
                        (lastY + y) / 2f
                );

                if (scratchCanvas != null) {

                    scratchCanvas.drawPath(
                            scratchPath,
                            scratchPaint
                    );
                }

                lastX = x;
                lastY = y;

                invalidate();

                checkReveal();

                return true;

            // ----------------------------------------------------
            // UP
            // ----------------------------------------------------

            case MotionEvent.ACTION_UP:

                if (hasStartedScratching) {

                    scratchPath.lineTo(
                            event.getX(),
                            event.getY()
                    );

                    if (scratchCanvas != null) {

                        scratchCanvas.drawPath(
                                scratchPath,
                                scratchPaint
                        );
                    }

                    invalidate();

                    checkReveal();
                }

                return true;

            // ----------------------------------------------------
            // CANCEL
            // ----------------------------------------------------

            case MotionEvent.ACTION_CANCEL:

                /*
                 * DO NOT reset card here.
                 *
                 * This prevents the old problem where clicking
                 * outside the ScratchView caused the card to reset
                 * and become scratchable again.
                 */

                scratchPath.reset();

                return true;
        }

        return true;
    }

    // ============================================================
    // CHECK SCRATCH %
    // ============================================================

    private void checkReveal() {

        if (scratchBitmap == null ||
                scratchBitmap.isRecycled()) {
            return;
        }

        if (isRevealed) {
            return;
        }

        int width =
                scratchBitmap.getWidth();

        int height =
                scratchBitmap.getHeight();

        if (width <= 0 ||
                height <= 0) {
            return;
        }

        int total = 0;
        int transparent = 0;

        /*
         * Check every 8 pixels.
         * This reduces CPU usage.
         */

        for (int y = 0;
             y < height;
             y += 8) {

            for (int x = 0;
                 x < width;
                 x += 8) {

                total++;

                int pixel =
                        scratchBitmap.getPixel(
                                x,
                                y
                        );

                int alpha =
                        Color.alpha(pixel);

                if (alpha < 80) {
                    transparent++;
                }
            }
        }

        if (total == 0) {
            return;
        }

        float scratched =
                (transparent * 100f)
                        / total;

        // ========================================================
        // 55% SCRATCHED
        // ========================================================

        if (scratched >= 55f) {

            /*
             * Stop scratching immediately.
             *
             * DO NOT reveal reward here.
             *
             * Activity will call backend.
             */

            isRevealed = true;

            isScratchEnabled = false;

            scratchPath.reset();

            invalidate();

            if (scratchListener != null) {

                scratchListener.onScratchComplete();
            }
        }
    }

    // ============================================================
    // SET SERVER REWARD
    // ============================================================

    public void setReward(
            int serverReward) {

        /*
         * This value comes from backend.
         */
        reward = serverReward;
    }

    // ============================================================
    // GET REWARD
    // ============================================================

    public int getReward() {

        return reward;
    }

    // ============================================================
    // REVEAL FROM BACKEND
    // ============================================================

    public void revealCardFromBackend() {

        if (scratchCanvas == null) {
            return;
        }

        /*
         * Never display a locally generated reward.
         */
        if (reward <= 0) {
            return;
        }

        isRevealed = true;

        isScratchEnabled = false;

        // Clear scratch layer
        scratchCanvas.drawColor(
                Color.TRANSPARENT,
                PorterDuff.Mode.CLEAR
        );

        // Draw server result
        drawFinalCard();

        invalidate();
    }

    // ============================================================
    // FINAL CARD
    // ============================================================

    private void drawFinalCard() {

        if (scratchCanvas == null) {
            return;
        }

        int width =
                getWidth();

        int height =
                getHeight();

        RectF rect =
                new RectF(
                        0,
                        0,
                        width,
                        height
                );

        // ========================================================
        // BACKGROUND
        // ========================================================

        cardPaint.setStyle(
                Paint.Style.FILL
        );

        cardPaint.setColor(
                Color.rgb(230, 0, 120)
        );

        scratchCanvas.drawRoundRect(
                rect,
                dp(20),
                dp(20),
                cardPaint
        );

        // ========================================================
        // GIFT
        // ========================================================

        try {

            Drawable gift =
                    getResources()
                            .getDrawable(
                                    R.drawable.ic_gift1,
                                    getContext()
                                            .getTheme()
                            );

            int iconSize =
                    dp(55);

            int left =
                    (width - iconSize) / 2;

            int top =
                    (int)
                            (height * 0.10f);

            gift.setBounds(
                    left,
                    top,
                    left + iconSize,
                    top + iconSize
            );

            gift.draw(
                    scratchCanvas
            );

        } catch (Exception ignored) {
        }

        // ========================================================
        // YOU WON
        // ========================================================

        textPaint.setColor(
                Color.WHITE
        );

        textPaint.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        textPaint.setTextAlign(
                Paint.Align.CENTER
        );

        textPaint.setTextSize(
                dp(22)
        );

        scratchCanvas.drawText(
                "YOU WON!",
                width / 2f,
                height * 0.40f,
                textPaint
        );

        // ========================================================
        // SERVER REWARD
        // ========================================================

        textPaint.setTextSize(
                dp(30)
        );

        scratchCanvas.drawText(
                reward + " COINS",
                width / 2f,
                height * 0.60f,
                textPaint
        );

        // ========================================================
        // BORDER
        // ========================================================

        borderPaint.setColor(
                Color.WHITE
        );

        scratchCanvas.drawRoundRect(
                rect.left + dp(2),
                rect.top + dp(2),
                rect.right - dp(2),
                rect.bottom - dp(2),
                dp(20),
                dp(20),
                borderPaint
        );
    }

    // ============================================================
    // RESET
    // ============================================================

    public void resetScratch() {

        /*
         * IMPORTANT:
         *
         * No Random here.
         * No reward generation here.
         *
         * Backend chooses the next reward only after the user
         * scratches the next card to 55%.
         */

        reward = 0;

        isRevealed = false;

        hasStartedScratching = false;

        scratchPath.reset();

        createScratchCard();

        invalidate();
    }

    // ============================================================
    // ENABLE / DISABLE
    // ============================================================

    public void setScratchEnabled(
            boolean enabled) {

        isScratchEnabled =
                enabled;

        invalidate();
    }

    public boolean isScratchEnabled() {

        return isScratchEnabled;
    }

    public boolean isRevealed() {

        return isRevealed;
    }

    // ============================================================
    // LISTENER
    // ============================================================

    public void setScratchListener(
            ScratchListener listener) {

        scratchListener =
                listener;
    }

    // ============================================================
    // DP
    // ============================================================

    private int dp(float value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f
        );
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    @Override
    protected void onDetachedFromWindow() {

        super.onDetachedFromWindow();

        if (scratchBitmap != null &&
                !scratchBitmap.isRecycled()) {

            scratchBitmap.recycle();

            scratchBitmap = null;
        }
    }
}

