package com.jandergy.myjandergymusic;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import java.util.Random;

public class MovingBlurView extends View {

    private static final int BLOB_COUNT = 5;
    private final Blob[] blobs = new Blob[BLOB_COUNT];
    private final Paint paint = new Paint();
    private final Random random = new Random();
    private int[] currentPalette = getDefaultPalette();

    public MovingBlurView(Context context) {
        super(context);
        init();
    }

    public MovingBlurView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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
        // Randomly assign new colors to blobs
        for (Blob blob : blobs) {
            blob.color = currentPalette[random.nextInt(currentPalette.length)];
        }
    }

    private void init() {
        for (int i = 0; i < BLOB_COUNT; i++) {
            int color = (i == 0) ? Color.parseColor("#2E7D32") : currentPalette[random.nextInt(currentPalette.length)];
            blobs[i] = new Blob(color);
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(10000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            for (Blob blob : blobs) {
                blob.update();
            }
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK); // Dark mode base

        for (Blob blob : blobs) {
            RadialGradient gradient = new RadialGradient(
                    blob.x * getWidth(),
                    blob.y * getHeight(),
                    blob.radius * Math.max(getWidth(), getHeight()),
                    new int[]{blob.color, Color.TRANSPARENT},
                    null,
                    Shader.TileMode.CLAMP
            );
            paint.setShader(gradient);
            paint.setAlpha(100); // Semi-transparent for overlapping
            canvas.drawCircle(blob.x * getWidth(), blob.y * getHeight(), blob.radius * Math.max(getWidth(), getHeight()), paint);
        }
    }

    private class Blob {
        float x, y;
        float vx, vy;
        float radius;
        int color;

        Blob(int color) {
            this.color = color;
            this.x = random.nextFloat();
            this.y = random.nextFloat();
            this.vx = (random.nextFloat() - 0.5f) * 0.005f;
            this.vy = (random.nextFloat() - 0.5f) * 0.005f;
            this.radius = 0.3f + random.nextFloat() * 0.4f;
        }

        void update() {
            x += vx;
            y += vy;

            if (x < 0 || x > 1) vx *= -1;
            if (y < 0 || y > 1) vy *= -1;
        }
    }
}
