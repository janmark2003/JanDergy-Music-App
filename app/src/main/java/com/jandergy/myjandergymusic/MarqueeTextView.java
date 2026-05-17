package com.jandergy.myjandergymusic;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A robust TextView for Marquee scrolling that aggressively maintains its focused
 * and selected state. Extends standard TextView for maximum compatibility.
 */
public class MarqueeTextView extends TextView {

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
        return true; // Fake focus to trigger marquee
    }

    @Override
    public boolean isSelected() {
        return true; // Fake selection to trigger marquee
    }
}
