package com.app.rewardsplanet.lucky_draw;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.app.rewardsplanet.R;

public class ScratchView extends View {

    // ============================================================
    // FIELDS
    // ============================================================

    private Bitmap scratchBitmap;
    private Canvas scratchCanvas;

    private Paint scratchPaint;   // eraser brush
    private Paint textPaint;      // text on cover
    private Paint cardPaint;      // solid fills
    private Paint borderPaint;    // borders
    private Paint shinePaint;     // shimmering effect
    private Paint particlePaint;  // sparkle dots

    private Path scratchPath;

    private float lastX;
    private float lastY;

    private boolean isScratchEnabled    = true;
    private boolean isRevealed          = false;
    private boolean isLimitReached      = false;
    private boolean hasStartedScratching = false;

    /** Reward comes ONLY from backend */
    private int reward = 0;

    // Sparkle particles for the revealed state
    private static final int PARTICLE_COUNT = 14;
    private float[] particleX;
    private float[] particleY;
    private float[] particleR;
    private int[]   particleAlpha;

    // Coin pulse animation
    private float coinScale  = 1.0f;
    private float glowAlpha  = 0.0f;
    private ValueAnimator coinAnimator;
    private ValueAnimator glowAnimator;

    private ScratchListener scratchListener;

    public interface ScratchListener {
        void onScratchStart();
        void onScratchComplete();
    }

    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public ScratchView(Context context) {
        super(context);
        init();
    }

    public ScratchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScratchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // ============================================================
    // INIT
    // ============================================================

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // --- Eraser brush ---
        scratchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scratchPaint.setStyle(Paint.Style.STROKE);
        scratchPaint.setStrokeCap(Paint.Cap.ROUND);
        scratchPaint.setStrokeJoin(Paint.Join.ROUND);
        scratchPaint.setStrokeWidth(dp(105));
        scratchPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        scratchPath = new Path();

        // --- Text ---
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // --- Card fill ---
        cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // --- Border ---
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(3));

        // --- Shine / glow ---
        shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setStyle(Paint.Style.FILL);

        // --- Particle dots ---
        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);

        setFocusable(true);
    }

    // ============================================================
    // SIZE CHANGED
    // ============================================================

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        initParticles();
        createScratchCard();
    }

    // ============================================================
    // PARTICLES
    // ============================================================

    private void initParticles() {
        particleX     = new float[PARTICLE_COUNT];
        particleY     = new float[PARTICLE_COUNT];
        particleR     = new float[PARTICLE_COUNT];
        particleAlpha = new int[PARTICLE_COUNT];

        float w = getWidth();
        float h = getHeight();

        // Position sparkles randomly over the card
        float[][] positions = {
            {0.12f, 0.08f}, {0.85f, 0.06f}, {0.05f, 0.30f}, {0.95f, 0.25f},
            {0.20f, 0.65f}, {0.78f, 0.70f}, {0.50f, 0.05f}, {0.50f, 0.93f},
            {0.30f, 0.40f}, {0.70f, 0.38f}, {0.15f, 0.82f}, {0.85f, 0.85f},
            {0.40f, 0.18f}, {0.62f, 0.88f}
        };

        float[] radii = {4, 3, 5, 3, 4, 5, 3, 4, 6, 3, 4, 3, 5, 4};

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] = w * positions[i][0];
            particleY[i] = h * positions[i][1];
            particleR[i] = dp(radii[i % radii.length]);
            particleAlpha[i] = 180;
        }
    }

    // ============================================================
    // CREATE SCRATCH COVER
    // ============================================================

    private void createScratchCard() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        if (scratchBitmap != null && !scratchBitmap.isRecycled()) {
            scratchBitmap.recycle();
        }

        scratchBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        scratchCanvas = new Canvas(scratchBitmap);
        scratchCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        int w = getWidth();
        int h = getHeight();
        RectF rect = new RectF(0, 0, w, h);
        float radius = dp(22);

        // ── COVER GRADIENT (gold-orange) ──
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{0xFFFFD700, 0xFFFF9500, 0xFFFF6B00},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        ));
        scratchCanvas.drawRoundRect(rect, radius, radius, cardPaint);
        cardPaint.setShader(null);

        // ── INNER GLOSS (top highlight) ──
        Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glossPaint.setStyle(Paint.Style.FILL);
        glossPaint.setShader(new LinearGradient(
                0, 0, 0, h * 0.45f,
                new int[]{0x55FFFFFF, 0x00FFFFFF},
                null,
                Shader.TileMode.CLAMP
        ));
        scratchCanvas.drawRoundRect(rect, radius, radius, glossPaint);

        // ── COIN ICON ON COVER ──
        try {
            Drawable coinIcon = getResources().getDrawable(R.drawable.ic_coin, getContext().getTheme());
            int iconSize = dp(52);
            int left = (w - iconSize) / 2;
            int top  = (int)(h * 0.18f);
            coinIcon.setBounds(left, top, left + iconSize, top + iconSize);
            coinIcon.draw(scratchCanvas);
        } catch (Exception ignored) {}

        // ── "SCRATCH HERE" LABEL ──
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(17));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        // Draw below center
        float textY = h * 0.62f - (fm.ascent + fm.descent) / 2f;
        scratchCanvas.drawText("SCRATCH HERE", w / 2f, textY, textPaint);

        // ── SMALL COIN SYMBOLS ON COVER ──
        textPaint.setTextSize(dp(13));
        textPaint.setAlpha(160);
        scratchCanvas.drawText("\uD83E\uDE99  \uD83E\uDE99  \uD83E\uDE99", w / 2f, h * 0.80f, textPaint);
        textPaint.setAlpha(255);

        // ── DASHED BORDER ──
        borderPaint.setColor(0x99FFFFFF);
        borderPaint.setStrokeWidth(dp(2));
        borderPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(8), dp(6)}, 0));
        RectF borderRect = new RectF(dp(6), dp(6), w - dp(6), h - dp(6));
        scratchCanvas.drawRoundRect(borderRect, radius - dp(4), radius - dp(4), borderPaint);
        borderPaint.setPathEffect(null);

        isRevealed           = false;
        hasStartedScratching = false;
    }

    // ============================================================
    // DRAW
    // ============================================================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float radius = dp(22);
        RectF rect = new RectF(0, 0, w, h);

        // If limit reached OR card revealed, show revealed back (with previous reward) if reward > 0
        if (isRevealed || (isLimitReached && reward > 0)) {
            drawRevealedBack(canvas, w, h, rect, radius);
        } else {
            drawMysteryBack(canvas, w, h, rect, radius);
        }

        // Draw scratch cover only if limit is NOT reached
        if (!isLimitReached && scratchBitmap != null && !scratchBitmap.isRecycled()) {
            canvas.drawBitmap(scratchBitmap, 0, 0, null);
        }

        // Draw rubber stamp overlay when daily limit is reached
        if (isLimitReached) {
            drawLimitReachedStamp(canvas, w, h);
        }
    }

    // ============================================================
    // MYSTERY BACK (shown while user is scratching)
    // ============================================================

    private void drawMysteryBack(Canvas canvas, int w, int h, RectF rect, float radius) {
        // Same dark purple gradient as the reveal, but no reward text
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{0xFF1A0533, 0xFF2D1B69, 0xFF1A0533},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, radius, radius, cardPaint);
        cardPaint.setShader(null);

        // Coin icon centered
        try {
            Drawable coinIcon = getResources().getDrawable(R.drawable.ic_coin, getContext().getTheme());
            int iconSize = dp(64);
            int left = (w - iconSize) / 2;
            int top  = (int)(h * 0.20f);
            coinIcon.setBounds(left, top, left + iconSize, top + iconSize);
            coinIcon.draw(canvas);
        } catch (Exception ignored) {}

        // "?" question mark placeholder
        textPaint.setColor(0x55FFFFFF);
        textPaint.setTextSize(dp(32));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("?", w / 2f, h * 0.65f, textPaint);

        // Subtle coin row at bottom
        drawCoinRow(canvas, w, h);

        // Subtle border
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2));
        borderPaint.setColor(0x33FFD700);
        borderPaint.setPathEffect(null);
        RectF borderRect = new RectF(dp(2), dp(2), w - dp(2), h - dp(2));
        canvas.drawRoundRect(borderRect, radius, radius, borderPaint);
    }

    // ============================================================
    // DRAW REVEALED BACK SIDE (PROFESSIONAL)
    // ============================================================

    private void drawRevealedBack(Canvas canvas, int w, int h, RectF rect, float radius) {

        // ── DEEP PURPLE-BLUE GRADIENT BACKGROUND ──
        cardPaint.setStyle(Paint.Style.FILL);
        cardPaint.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{0xFF1A0533, 0xFF2D1B69, 0xFF1A0533},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(rect, radius, radius, cardPaint);
        cardPaint.setShader(null);

        // ── GLOWING AURA behind coin area ──
        Paint auraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        auraPaint.setStyle(Paint.Style.FILL);
        auraPaint.setShader(new android.graphics.RadialGradient(
                w / 2f, h * 0.40f, dp(80),
                new int[]{0x44FFD700, 0x22FFB800, 0x00FFD700},
                null,
                Shader.TileMode.CLAMP
        ));
        auraPaint.setAlpha((int)(glowAlpha * 255));
        canvas.drawCircle(w / 2f, h * 0.40f, dp(80), auraPaint);

        // ── SPARKLE PARTICLES ──
        int[] sparkColors = {0xFFFFD700, 0xFFFFFFFF, 0xFFFF9500, 0xFFFFE066};
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particlePaint.setColor(sparkColors[i % sparkColors.length]);
            particlePaint.setAlpha(particleAlpha[i]);
            canvas.drawCircle(particleX[i], particleY[i], particleR[i], particlePaint);

            // Cross/star sparkle
            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setColor(sparkColors[i % sparkColors.length]);
            linePaint.setAlpha(particleAlpha[i] / 2);
            linePaint.setStrokeWidth(dp(1.5f));
            float r = particleR[i] * 2.5f;
            canvas.drawLine(particleX[i] - r, particleY[i], particleX[i] + r, particleY[i], linePaint);
            canvas.drawLine(particleX[i], particleY[i] - r, particleX[i], particleY[i] + r, linePaint);
        }

        // ── COIN ICON (large, scaled) ──
        try {
            Drawable coinIcon = getResources().getDrawable(R.drawable.ic_coin, getContext().getTheme());
            int iconSize = (int)(dp(72) * coinScale);
            int left = (w - iconSize) / 2;
            int top  = (int)(h * 0.08f);
            coinIcon.setBounds(left, top, left + iconSize, top + iconSize);
            coinIcon.draw(canvas);
        } catch (Exception ignored) {}

        // ── "YOU WON!" LABEL ──
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dp(16));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setLetterSpacing(0.12f);
        canvas.drawText("\u2728  YOU WON!  \u2728", w / 2f, h * 0.41f, textPaint);
        textPaint.setLetterSpacing(0f);

        // ── GOLD REWARD AMOUNT ──
        if (reward > 0) {
            // Gold gradient text  (approximated via shadow + yellow)
            textPaint.setColor(0xFFFFD700);
            textPaint.setTextSize(dp(36));
            textPaint.setShadowLayer(dp(10), 0, 0, 0xAAFFAA00);
            canvas.drawText("+" + reward, w / 2f, h * 0.57f, textPaint);
            textPaint.setShadowLayer(0, 0, 0, 0);

            // "COINS" sub-label
            textPaint.setColor(0xFFFFE57F);
            textPaint.setTextSize(dp(15));
            textPaint.setLetterSpacing(0.14f);
            canvas.drawText("C O I N S", w / 2f, h * 0.66f, textPaint);
            textPaint.setLetterSpacing(0f);
        }

        // ── COIN ROW ICONS (decorative) ──
        drawCoinRow(canvas, w, h);

        // ── GLOWING BORDER ──
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(3));
        borderPaint.setColor(0xAAFFD700);
        borderPaint.setShadowLayer(dp(8), 0, 0, 0xFFFFD700);
        borderPaint.setPathEffect(null);
        RectF borderRect = new RectF(dp(2), dp(2), w - dp(2), h - dp(2));
        canvas.drawRoundRect(borderRect, radius, radius, borderPaint);
        borderPaint.setShadowLayer(0, 0, 0, 0);
    }

    // ============================================================
    // COIN ROW (small coin circles at bottom)
    // ============================================================

    private void drawCoinRow(Canvas canvas, int w, int h) {
        float y     = h * 0.83f;
        float space = dp(28);
        int   count = 5;
        float startX = w / 2f - (count - 1) * space / 2f;
        int[] alphas = {100, 160, 220, 160, 100};
        float[] scales = {0.6f, 0.75f, 1.0f, 0.75f, 0.6f};

        for (int i = 0; i < count; i++) {
            float cx  = startX + i * space;
            float r   = dp(8) * scales[i];

            // Coin circle
            cardPaint.setStyle(Paint.Style.FILL);
            cardPaint.setShader(new android.graphics.RadialGradient(
                    cx, y - r * 0.3f, r,
                    new int[]{0xFFFFF0A0, 0xFFFFD700, 0xFFB8860B},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            cardPaint.setAlpha(alphas[i]);
            canvas.drawCircle(cx, y, r, cardPaint);
            cardPaint.setAlpha(255);
            cardPaint.setShader(null);

            // Coin rim
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dp(1.5f));
            borderPaint.setColor(0xFFB8860B);
            borderPaint.setAlpha(alphas[i]);
            borderPaint.setPathEffect(null);
            canvas.drawCircle(cx, y, r, borderPaint);
            borderPaint.setAlpha(255);

            // "$" symbol
            textPaint.setColor(0xFF8B6914);
            textPaint.setAlpha(alphas[i]);
            textPaint.setTextSize(r * 1.1f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm2 = textPaint.getFontMetrics();
            canvas.drawText("$", cx, y - (fm2.ascent + fm2.descent) / 2f, textPaint);
            textPaint.setAlpha(255);
        }
    }

    // ============================================================
    // TOUCH
    // ============================================================

    private boolean thresholdReached = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isScratchEnabled)            return true;
        if (isLimitReached)               return true;

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                hasStartedScratching = true;
                thresholdReached     = false;
                scratchPath.reset();
                scratchPath.moveTo(lastX, lastY);
                invalidate();
                if (scratchListener != null && !isRevealed) {
                    scratchListener.onScratchStart();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!hasStartedScratching) return true;
                float x = event.getX();
                float y = event.getY();
                scratchPath.quadTo(lastX, lastY, (lastX + x) / 2f, (lastY + y) / 2f);
                if (scratchCanvas != null) {
                    scratchCanvas.drawPath(scratchPath, scratchPaint);
                }
                lastX = x;
                lastY = y;
                invalidate();

                // Reveal background ("YOU WON!" + coin value) underneath cover once 30% scratched
                if (!thresholdReached && !isRevealed) {
                    float scratched = calculateScratchedPercentage();
                    if (scratched >= 30f) {
                        thresholdReached = true;
                        isRevealed       = true;
                        startRevealAnimations();
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (hasStartedScratching) {
                    scratchPath.lineTo(event.getX(), event.getY());
                    if (scratchCanvas != null) {
                        scratchCanvas.drawPath(scratchPath, scratchPaint);
                    }
                    invalidate();

                    float scratched = calculateScratchedPercentage();
                    if (thresholdReached || scratched >= 30f) {
                        thresholdReached = true;
                        isRevealed       = true;
                        isScratchEnabled = false;
                        scratchPath.reset();

                        // Clear remaining cover
                        if (scratchCanvas != null) {
                            scratchCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        }
                        invalidate();

                        // Fire completion ONLY on finger release
                        if (scratchListener != null) {
                            scratchListener.onScratchComplete();
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                scratchPath.reset();
                return true;
        }
        return true;
    }

    // ============================================================
    // CALCULATE SCRATCH %
    // ============================================================

    private float calculateScratchedPercentage() {
        if (scratchBitmap == null || scratchBitmap.isRecycled()) return 0f;

        int width  = scratchBitmap.getWidth();
        int height = scratchBitmap.getHeight();
        if (width <= 0 || height <= 0) return 0f;

        int total = 0;
        int transparent = 0;

        for (int py = 0; py < height; py += 8) {
            for (int px = 0; px < width; px += 8) {
                total++;
                if (Color.alpha(scratchBitmap.getPixel(px, py)) < 80) {
                    transparent++;
                }
            }
        }

        if (total == 0) return 0f;
        return (transparent * 100f) / total;
    }

    // ============================================================
    // SET SERVER REWARD
    // ============================================================

    public void setReward(int serverReward) {
        reward = serverReward;
    }

    public int getReward() {
        return reward;
    }

    // ============================================================
    // REVEAL FROM BACKEND (clears cover + starts animations)
    // ============================================================

    public void revealCardFromBackend() {
        if (scratchCanvas == null) return;
        if (reward <= 0) return;

        isRevealed       = true;
        isScratchEnabled = false;

        // Clear entire scratch cover so onDraw shows the back side
        scratchCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        startRevealAnimations();

        invalidate();
    }

    // ============================================================
    // ANIMATIONS
    // ============================================================

    private void startRevealAnimations() {

        // Coin pulse scale
        coinAnimator = ValueAnimator.ofFloat(0.85f, 1.15f, 1.0f);
        coinAnimator.setDuration(600);
        coinAnimator.setInterpolator(new DecelerateInterpolator());
        coinAnimator.addUpdateListener(anim -> {
            coinScale = (float) anim.getAnimatedValue();
            invalidate();
        });
        coinAnimator.start();

        // Glow fade in
        glowAnimator = ValueAnimator.ofFloat(0f, 1f);
        glowAnimator.setDuration(800);
        glowAnimator.addUpdateListener(anim -> {
            glowAlpha = (float) anim.getAnimatedValue();
            invalidate();
        });
        glowAnimator.start();

        // Particle flicker
        startParticleAnimation();
    }

    private void startParticleAnimation() {
        ValueAnimator flicker = ValueAnimator.ofFloat(0f, 1f);
        flicker.setDuration(1200);
        flicker.setRepeatCount(ValueAnimator.INFINITE);
        flicker.setRepeatMode(ValueAnimator.REVERSE);
        flicker.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                // stagger each particle
                float phase = (t + (float) i / PARTICLE_COUNT) % 1.0f;
                particleAlpha[i] = (int)(60 + 195 * Math.abs((phase * 2f) - 1f));
            }
            invalidate();
        });
        flicker.start();
    }

    // ============================================================
    // RESET
    // ============================================================

    public void resetScratch() {
        reward               = 0;
        isRevealed           = false;
        thresholdReached     = false;
        hasStartedScratching = false;
        coinScale            = 1.0f;
        glowAlpha            = 0f;

        if (coinAnimator != null) coinAnimator.cancel();
        if (glowAnimator != null) glowAnimator.cancel();

        scratchPath.reset();
        createScratchCard();
        invalidate();
    }

    /**
     * Resets the scratch card only if it was already revealed.
     * Call this from onResume() to prevent showing previous reward on return.
     */
    public void resetIfRevealed() {
        if (isRevealed) {
            resetScratch();
        }
    }

    // ============================================================
    // ENABLE / DISABLE
    // ============================================================

    public void setScratchEnabled(boolean enabled) {
        isScratchEnabled = enabled;
        invalidate();
    }

    public void setLimitReached(boolean limitReached) {
        this.isLimitReached = limitReached;
        if (limitReached) {
            this.isScratchEnabled = false;
        }
        invalidate();
    }

    public boolean isLimitReached()   { return isLimitReached; }
    public boolean isScratchEnabled() { return isScratchEnabled; }
    public boolean isRevealed()       { return isRevealed; }

    // ============================================================
    // STAMP — DAILY LIMIT REACHED
    // ============================================================

    private void drawLimitReachedStamp(Canvas canvas, int w, int h) {
        canvas.save();

        // 1. Semi-transparent dark overlay over card to make stamp pop
        Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(0x880B0B18);
        RectF cardRect = new RectF(0, 0, w, h);
        canvas.drawRoundRect(cardRect, dp(22), dp(22), overlayPaint);

        // 2. Rotate canvas for rubber stamp effect (-14 degrees)
        canvas.rotate(-14f, w / 2f, h / 2f);

        // Stamp Dimensions
        float stampW = w * 0.84f;
        float stampH = dp(84);
        float stampLeft = (w - stampW) / 2f;
        float stampTop  = (h - stampH) / 2f;
        RectF stampRect = new RectF(stampLeft, stampTop, stampLeft + stampW, stampTop + stampH);

        // Stamp Fill Background (Rich crimson red)
        Paint stampBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        stampBg.setStyle(Paint.Style.FILL);
        stampBg.setColor(0xFCDC2626);
        canvas.drawRoundRect(stampRect, dp(12), dp(12), stampBg);

        // Stamp Outer Border
        Paint stampBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        stampBorder.setStyle(Paint.Style.STROKE);
        stampBorder.setStrokeWidth(dp(3.5f));
        stampBorder.setColor(0xFFFFFFFF);
        canvas.drawRoundRect(stampRect, dp(12), dp(12), stampBorder);

        // Stamp Inner Border
        Paint innerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerBorder.setStyle(Paint.Style.STROKE);
        innerBorder.setStrokeWidth(dp(1.5f));
        innerBorder.setColor(0xCCFFFFFF);
        RectF innerRect = new RectF(stampRect.left + dp(4), stampRect.top + dp(4), stampRect.right - dp(4), stampRect.bottom - dp(4));
        canvas.drawRoundRect(innerRect, dp(8), dp(8), innerBorder);

        // Text Paint
        Paint stampTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stampTextPaint.setColor(Color.WHITE);
        stampTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        stampTextPaint.setTextAlign(Paint.Align.CENTER);

        // Title Line: "DAILY LIMIT REACHED"
        stampTextPaint.setTextSize(dp(15));
        stampTextPaint.setLetterSpacing(0.06f);
        canvas.drawText("DAILY LIMIT REACHED", w / 2f, stampTop + dp(35), stampTextPaint);

        // Subtitle Line: "🔒 COME BACK TOMORROW"
        stampTextPaint.setTextSize(dp(11));
        stampTextPaint.setLetterSpacing(0.04f);
        stampTextPaint.setColor(0xFFFFE4E6);
        canvas.drawText("🔒 COME BACK TOMORROW", w / 2f, stampTop + dp(59), stampTextPaint);

        canvas.restore();
    }

    // ============================================================
    // LISTENER
    // ============================================================

    public void setScratchListener(ScratchListener listener) {
        scratchListener = listener;
    }

    // ============================================================
    // DP
    // ============================================================

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (coinAnimator != null) coinAnimator.cancel();
        if (glowAnimator != null) glowAnimator.cancel();
        if (scratchBitmap != null && !scratchBitmap.isRecycled()) {
            scratchBitmap.recycle();
            scratchBitmap = null;
        }
    }
}
