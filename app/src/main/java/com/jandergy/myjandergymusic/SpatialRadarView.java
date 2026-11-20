package com.jandergy.myjandergymusic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interactive neon 2D Spatial Radar Pad for 8D Audio.
 * Displays the listener head at the center and a live glowing sound-source orb.
 * Supports direct finger dragging in Touch Mode and real-time orbit/gyro tracking.
 */
public class SpatialRadarView extends View {

    public interface OnPositionChangedListener {
        void onPositionChanged(float x, float y);
    }

    private static final int TRAIL_SIZE = 14;
    private final float[] trailX = new float[TRAIL_SIZE];
    private final float[] trailY = new float[TRAIL_SIZE];
    private int trailCount = 0;

    private float orbX = 0f; // -1 (Left) .. +1 (Right)
    private float orbY = 1f; // -1 (Back) .. +1 (Front)
    private boolean touchInteractive = false;
    private OnPositionChangedListener listener;

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SpatialRadarView(Context context) {
        super(context);
        init();
    }

    public SpatialRadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpatialRadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float dp = getResources().getDisplayMetrics().density;

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.2f * dp);
        ringPaint.setColor(0x3800E5FF);

        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(1f * dp);
        axisPaint.setColor(0x22FFFFFF);

        headPaint.setStyle(Paint.Style.FILL);
        headPaint.setColor(0xFF1F2436);

        labelPaint.setColor(0x9994A3B8);
        labelPaint.setTextSize(10f * dp);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        orbPaint.setStyle(Paint.Style.FILL);
        orbPaint.setColor(0xFF00E5FF);

        trailPaint.setStyle(Paint.Style.FILL);
        trailPaint.setColor(0xFF00E5FF);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f * dp);
        linePaint.setColor(0x5500E5FF);
    }

    public void setTouchInteractive(boolean interactive) {
        this.touchInteractive = interactive;
    }

    public void setOnPositionChangedListener(@Nullable OnPositionChangedListener listener) {
        this.listener = listener;
    }

    public void setOrbPosition(float x, float y) {
        float clampedX = Math.max(-1f, Math.min(1f, x));
        float clampedY = Math.max(-1f, Math.min(1f, y));
        float len = (float) Math.hypot(clampedX, clampedY);
        if (len > 1f) {
            clampedX /= len;
            clampedY /= len;
        }
        if (Math.hypot(clampedX - orbX, clampedY - orbY) > 0.015f) {
            for (int i = TRAIL_SIZE - 1; i > 0; i--) {
                trailX[i] = trailX[i - 1];
                trailY[i] = trailY[i - 1];
            }
            trailX[0] = orbX;
            trailY[0] = orbY;
            if (trailCount < TRAIL_SIZE) trailCount++;
        }
        orbX = clampedX;
        orbY = clampedY;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!touchInteractive) {
            return super.onTouchEvent(event);
        }
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float cx = getWidth() * 0.5f;
            float cy = getHeight() * 0.5f;
            float maxR = Math.min(cx, cy) - 26f * getResources().getDisplayMetrics().density;
            if (maxR > 1f) {
                float nx = (event.getX() - cx) / maxR;
                float ny = -(event.getY() - cy) / maxR; // screen Y down -> spatial Y up = front
                float len = (float) Math.hypot(nx, ny);
                if (len > 1f) {
                    nx /= len;
                    ny /= len;
                }
                setOrbPosition(nx, ny);
                if (listener != null) {
                    listener.onPositionChanged(orbX, orbY);
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float dp = getResources().getDisplayMetrics().density;
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        float maxR = Math.max(20f, Math.min(cx, cy) - 24f * dp);

        // Concentric radar rings
        canvas.drawCircle(cx, cy, maxR, ringPaint);
        canvas.drawCircle(cx, cy, maxR * 0.66f, ringPaint);
        canvas.drawCircle(cx, cy, maxR * 0.33f, ringPaint);

        // Crosshairs
        canvas.drawLine(cx - maxR, cy, cx + maxR, cy, axisPaint);
        canvas.drawLine(cx, cy - maxR, cx, cy + maxR, axisPaint);

        // Cardinal labels
        canvas.drawText("FRONT", cx, cy - maxR - 6f * dp, labelPaint);
        canvas.drawText("BACK", cx, cy + maxR + 14f * dp, labelPaint);
        canvas.drawText("L", cx - maxR - 11f * dp, cy + 4f * dp, labelPaint);
        canvas.drawText("R", cx + maxR + 11f * dp, cy + 4f * dp, labelPaint);

        // Center listener head icon
        float headR = 14f * dp;
        canvas.drawCircle(cx, cy, headR, headPaint);
        canvas.drawCircle(cx, cy, headR, ringPaint);
        // Ear cups
        float earW = 4f * dp;
        float earH = 8f * dp;
        canvas.drawRoundRect(cx - headR - earW, cy - earH, cx - headR + 1f * dp, cy + earH, 3f * dp, 3f * dp, orbPaint);
        canvas.drawRoundRect(cx + headR - 1f * dp, cy - earH, cx + headR + earW, cy + earH, 3f * dp, 3f * dp, orbPaint);

        // Orb coordinates
        float px = cx + orbX * maxR;
        float py = cy - orbY * maxR;

        // Beam from listener to sound source
        canvas.drawLine(cx, cy, px, py, linePaint);

        // Glowing motion trail
        for (int i = trailCount - 1; i >= 0; i--) {
            float tx = cx + trailX[i] * maxR;
            float ty = cy - trailY[i] * maxR;
            float frac = 1f - (i / (float) TRAIL_SIZE);
            trailPaint.setAlpha((int) (frac * 110f));
            canvas.drawCircle(tx, ty, (3f + frac * 5f) * dp, trailPaint);
        }

        // Outer neon halo
        float haloR = 22f * dp;
        glowPaint.setShader(new RadialGradient(px, py, haloR,
                new int[]{0xAA00E5FF, Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(px, py, haloR, glowPaint);

        // Core orb
        canvas.drawCircle(px, py, 9f * dp, orbPaint);
        orbPaint.setColor(Color.WHITE);
        canvas.drawCircle(px, py, 4f * dp, orbPaint);
        orbPaint.setColor(0xFF00E5FF);
    }
}
