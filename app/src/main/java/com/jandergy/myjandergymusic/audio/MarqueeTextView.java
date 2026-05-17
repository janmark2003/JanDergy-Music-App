package com.jandergy.myjandergymusic;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;

public class MarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {

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
        // Force the text view to always setup the marquee rules
        setSingleLine(true);
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1); // -1 means marquee_forever
        setHorizontallyScrolling(true);
        setSelected(true); // Required to start the marquee
    }

    // --- THESE 4 OVERRIDES ARE THE SECRET TO STOPPING THE TEXT FROM CUTTING OFF ---

    @Override
    public boolean isFocused() {
        // Trick Android into thinking this view always has focus
        return true;
    }

    @Override
    public boolean isSelected() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // Even if focus changes, tell the superclass it still has focus
        if (focused) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
        }
    }
}