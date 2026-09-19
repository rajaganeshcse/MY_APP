package com.app.rewardsplanet.lucky_draw;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * SpinWheelView — Premium programmatic spin wheel.
 *
 * Segment coordinate system:
 *   • Index 0 starts at 12 o'clock (top) and goes CLOCKWISE.
 *   • This must match the backend's segmentIndex convention exactly.
 *
 * Default 8 segments (clockwise from top):
 *   0→5c  1→10c  2→2c  3→20c  4→5c  5→50c  6→2c  7→10c
 *
 * Override at runtime by calling setSegmentValues(List<Integer>).
 */
public class SpinWheelView extends View {

    // ─── Default wheel layout — must match backend configuration ──────────
    private static final int[] DEFAULT_VALUES = {5, 10, 2, 20, 5, 50, 2, 10};

    // ─── Segment color palettes (dark gaming theme) ───────────────────────
    // Even segments
    private static final int[] PAL_EVEN = {
            0xFF1E1B4B,   // deep indigo
            0xFF14213D,   // navy
            0xFF1A1035,   // dark purple-navy
            0xFF0F2041,   // dark blue
    };
    // Odd segments
    private static final int[] PAL_ODD = {
            0xFF312E81,   // indigo
            0xFF1D3461,   // dark royal blue
            0xFF2D2257,   // mid purple
            0xFF1A3458,   // mid navy
    };
    // Highest-value segment gets an amber/gold background to stand out
    private static final int COLOR_JACKPOT = 0xFF6B3A07;   // dark amber

    // ─── State ────────────────────────────────────────────────────────────
    private int[]  segValues;
    private int    segCount;
    private float  segSweep;   // degrees per segment

    // ─── Paints ───────────────────────────────────────────────────────────
    private final Paint fillPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubFillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubRingPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hubDotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF wheelOval = new RectF();

    // ─── Constructors ─────────────────────────────────────────────────────
    public SpinWheelView(Context context) {
        super(context); init();
    }
    public SpinWheelView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public SpinWheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        segValues = DEFAULT_VALUES.clone();
        segCount  = segValues.length;
        segSweep  = 360f / segCount;

        fillPaint.setStyle(Paint.Style.FILL);

        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setColor(0xAAFFD700);

        outerRingPaint.setStyle(Paint.Style.STROKE);
        outerRingPaint.setColor(0xFFFFD700);
        outerRingPaint.setShadowLayer(18f, 0f, 0f, 0xCCFFD700);

        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setColor(0x30FFFFFF);

        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        valuePaint.setShadowLayer(4f, 0f, 1.5f, 0x99000000);

        labelPaint.setColor(0xFFFFD700);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setShadowLayer(2f, 0f, 1f, 0x88000000);

        hubFillPaint.setStyle(Paint.Style.FILL);

        hubRingPaint.setStyle(Paint.Style.STROKE);
        hubRingPaint.setColor(0xFFFFD700);
        hubRingPaint.setShadowLayer(8f, 0f, 0f, 0xAAFFD700);

        hubDotPaint.setColor(0xFFFFE066);
        hubDotPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(0x28000000);

        // Software layer is required for Paint.setShadowLayer() to work
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    // ─── Public API ───────────────────────────────────────────────────────

    /**
     * Update wheel segments with values received from the backend.
     * Must be called on the main thread.
     */
    public void setSegmentValues(List<Integer> newValues) {
        if (newValues == null || newValues.isEmpty()) return;
        segValues = new int[newValues.size()];
        for (int i = 0; i < newValues.size(); i++) {
            segValues[i] = (newValues.get(i) != null) ? newValues.get(i) : 0;
        }
        segCount = segValues.length;
        segSweep = 360f / segCount;
        invalidate();
    }

    /** Returns the current number of segments (used for angle calculation). */
    public int getSegmentCount() { return segCount; }

    /** Returns a copy of the current segment values array. */
    public int[] getSegmentValues() { return segValues.clone(); }

    // ─── Measurement ──────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Force square measurement
        int size = Math.min(
                MeasureSpec.getSize(widthSpec),
                MeasureSpec.getSize(heightSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        float size   = Math.min(w, h);
        float cx     = size / 2f;
        float pad    = 14f;
        float radius = cx - pad;

        wheelOval.set(pad, pad, size - pad, size - pad);

        // Scale all text & stroke widths proportionally to radius
        valuePaint.setTextSize(Math.max(12f, radius * 0.158f));
        labelPaint.setTextSize(Math.max(9f,  radius * 0.092f));
        outerRingPaint.setStrokeWidth(Math.max(6f, radius * 0.040f));
        dividerPaint.setStrokeWidth(Math.max(1.5f, radius * 0.007f));
        innerRingPaint.setStrokeWidth(Math.max(1.5f, radius * 0.006f));
        hubRingPaint.setStrokeWidth(Math.max(3f, radius * 0.023f));
    }

    // ─── Drawing ──────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float size   = Math.min(getWidth(), getHeight());
        float cx     = size / 2f;
        float cy     = size / 2f;
        float pad    = 14f;
        float radius = cx - pad;

        // Find maximum reward value for jackpot highlight
        int maxVal = 0;
        for (int v : segValues) if (v > maxVal) maxVal = v;

        // ── Step 1: Drop shadow beneath wheel ─────────────────────────────
        canvas.drawCircle(cx + 3f, cy + 5f, radius + 2f, shadowPaint);

        // ── Step 2: Draw each segment ─────────────────────────────────────
        for (int i = 0; i < segCount; i++) {
            float startAngle = -90f + i * segSweep;

            // Pick segment background color
            boolean isJackpot = (segValues[i] == maxVal && maxVal > 5);
            if (isJackpot) {
                fillPaint.setColor(COLOR_JACKPOT);
            } else if (i % 2 == 0) {
                fillPaint.setColor(PAL_EVEN[(i / 2) % PAL_EVEN.length]);
            } else {
                fillPaint.setColor(PAL_ODD[(i / 2) % PAL_ODD.length]);
            }
            canvas.drawArc(wheelOval, startAngle, segSweep, true, fillPaint);

            // ── Step 2a: Draw segment text ─────────────────────────────────
            float midAngle = startAngle + segSweep / 2f;
            double midRad  = Math.toRadians(midAngle);
            float textR    = radius * 0.60f;
            float tx = cx + textR * (float) Math.cos(midRad);
            float ty = cy + textR * (float) Math.sin(midRad);

            canvas.save();
            // Rotate so text reads from center outward (standard for wheels)
            canvas.rotate(midAngle + 90f, tx, ty);

            // Value (e.g., "50")
            if (isJackpot) {
                valuePaint.setColor(0xFFFFE44D);   // bright gold for jackpot
            } else {
                valuePaint.setColor(Color.WHITE);
            }
            canvas.drawText(
                    String.valueOf(segValues[i]),
                    tx,
                    ty - valuePaint.getTextSize() * 0.22f,
                    valuePaint);

            // "coins" sub-label
            canvas.drawText(
                    "coins",
                    tx,
                    ty + valuePaint.getTextSize() * 0.72f,
                    labelPaint);

            canvas.restore();
        }

        // ── Step 3: Segment divider lines ─────────────────────────────────
        for (int i = 0; i < segCount; i++) {
            double rad = Math.toRadians(-90.0 + i * segSweep);
            canvas.drawLine(
                    cx, cy,
                    cx + radius * (float) Math.cos(rad),
                    cy + radius * (float) Math.sin(rad),
                    dividerPaint);
        }

        // ── Step 4: Outer gold ring ────────────────────────────────────────
        canvas.drawCircle(cx, cy, radius, outerRingPaint);

        // ── Step 5: Inner subtle ring (decorative) ─────────────────────────
        canvas.drawCircle(cx, cy, radius * 0.38f, innerRingPaint);

        // ── Step 6: Center hub ─────────────────────────────────────────────
        float hubR = radius * 0.13f;

        // Hub radial gradient (slate center → dark outer)
        RadialGradient hubGrad = new RadialGradient(
                cx, cy, hubR,
                new int[]{0xFF64748B, 0xFF0F172A},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP);
        hubFillPaint.setShader(hubGrad);
        canvas.drawCircle(cx, cy, hubR, hubFillPaint);

        // Hub gold ring
        canvas.drawCircle(cx, cy, hubR, hubRingPaint);

        // Center gold dot
        canvas.drawCircle(cx, cy, hubR * 0.30f, hubDotPaint);
    }
}
