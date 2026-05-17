package com.jandergy.myjandergymusic;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * A robust TextView for Marquee scrolling that aggressively maintains focus and selection.
 */
public class MarqueeTextView extends AppCompatTextView {

    public MarqueeTextView(Context context) {
        super(context);
        init();
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSingleLine(true);
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1); // Forever
        setHorizontallyScrolling(true);
        setSelected(true);
    }

    @Override
    public boolean isFocused() {
        return true; // Always focused for marquee
    }

    @Override
    public boolean isSelected() {
        return true; // Always selected for marquee
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // Ignore focus loss and always report as focused
        super.onFocusChanged(true, direction, previouslyFocusedRect);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // Ignore window focus loss
        super.onWindowFocusChanged(true);
    }
}
