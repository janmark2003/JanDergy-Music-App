package com.jandergy.myjandergymusic;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

public class MovingBlurView extends View {

    private static final int BLOB_COUNT = 5;
    private static final long FRAME_INTERVAL_MS = 33L; // ~30 fps for smooth, low-CPU ambient blur

    private final Blob[] blobs = new Blob[BLOB_COUNT];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix shaderMatrix = new Matrix();
    private final Random random = new Random();
    private int[] currentPalette = getDefaultPalette();
    private ValueAnimator animator;
    private long lastFrameTimeMs = 0L;

    public MovingBlurView(Context context) {
        super(context);
        init();
    }

    public MovingBlurView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MovingBlurView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private int[] getDefaultPalette() {
        return new int[]{
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#81C784"), // Light Green
                Color.parseColor("#FFEB3B"), // Yellow
                Color.parseColor("#FF5722"), // Deep Orange
                Color.parseColor("#E91E63"), // Pink
                Color.parseColor("#9C27B0"), // Purple
                Color.parseColor("#2196F3")  // Blue
        };
    }

    public void setPalette(int[] colors) {
        if (colors == null || colors.length == 0) {
            currentPalette = getDefaultPalette();
        } else {
            currentPalette = colors;
        }
        for (Blob blob : blobs) {
            if (blob != null) {
                blob.setColor(currentPalette[random.nextInt(currentPalette.length)]);
            }
        }
        invalidate();
    }

    private void init() {
        paint.setAlpha(100);
        for (int i = 0; i < BLOB_COUNT; i++) {
            int color = (i == 0) ? Color.parseColor("#2E7D32") : currentPalette[random.nextInt(currentPalette.length)];
            blobs[i] = new Blob(color);
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(10000L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            long now = SystemClock.uptimeMillis();
            if (now - lastFrameTimeMs < FRAME_INTERVAL_MS) {
                return;
            }
            lastFrameTimeMs = now;
            for (Blob blob : blobs) {
                blob.update();
            }
            invalidate();
        });
    }

    private void updateAnimationState() {
        if (animator == null) return;
        boolean shouldRun = isAttachedToWindow() && getWindowVisibility() == VISIBLE && isShown();
        if (shouldRun) {
            if (!animator.isStarted()) {
                animator.start();
            } else if (animator.isPaused()) {
                animator.resume();
            }
        } else {
            if (animator.isRunning()) {
                animator.pause();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimationState();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateAnimationState();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateAnimationState();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK); // Dark mode base

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float maxDim = Math.max(width, height);
        for (Blob blob : blobs) {
            float radius = blob.radius * maxDim;
            if (radius <= 0f) {
                continue;
            }
            float cx = blob.x * width;
            float cy = blob.y * height;

            shaderMatrix.reset();
            shaderMatrix.setScale(radius, radius);
            shaderMatrix.postTranslate(cx, cy);
            blob.gradient.setLocalMatrix(shaderMatrix);

            paint.setShader(blob.gradient);
            paint.setAlpha(100);
            canvas.drawCircle(cx, cy, radius, paint);
        }
    }

    private class Blob {
        float baseX, baseY;
        float angle;
        float angleSpeed;
        float driftRange;
        float radius;
        float baseRadius;
        int color;
        float x, y;
        RadialGradient gradient;

        Blob(int color) {
            this.baseX = random.nextFloat();
            this.baseY = random.nextFloat();
            this.angle = random.nextFloat() * (float) (2 * Math.PI);
            this.angleSpeed = 0.03f + random.nextFloat() * 0.04f;
            this.driftRange = 0.12f + random.nextFloat() * 0.15f;
            this.baseRadius = 0.35f + random.nextFloat() * 0.35f;
            this.radius = baseRadius;
            this.x = baseX;
            this.y = baseY;
            setColor(color);
        }

        void setColor(int newColor) {
            this.color = newColor;
            this.gradient = new RadialGradient(
                    0f,
                    0f,
                    1f,
                    new int[]{newColor, Color.TRANSPARENT},
                    null,
                    Shader.TileMode.CLAMP
            );
        }

        void update() {
            angle += angleSpeed;
            x = baseX + (float) Math.sin(angle) * driftRange;
            y = baseY + (float) Math.cos(angle * 0.8f) * driftRange;
            radius = baseRadius + (float) Math.sin(angle * 0.5f) * 0.08f;
        }
    }
}
