package ragone.io.quietmind;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal ruler-style picker. Drag or fling to scroll; it always settles on an item.
 *
 * Replaces com.lantouzi.wheelview, which is no longer published anywhere. Only user gestures
 * notify the listener; {@link #selectIndex} and {@link #smoothSelectIndex} are silent so the
 * countdown can drive the wheel without being mistaken for a new selection.
 */
public class WheelView extends View {

    public interface OnWheelItemSelectedListener {
        /** The item under the cursor changed while the user is scrolling. */
        void onWheelItemChanged(WheelView wheelView, int position);

        /** The user finished scrolling and the wheel settled on {@code position}. */
        void onWheelItemSelected(WheelView wheelView, int position);
    }

    private static final int MINOR_MARKS_PER_ITEM = 4;
    private static final int SNAP_DURATION_MS = 250;

    private final Paint markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cursorPath = new Path();
    private final OverScroller scroller;
    private final GestureDetector gestureDetector;
    private final int touchSlop;
    private final int minFlingVelocity;
    private final int maxFlingVelocity;

    private final int highlightColor;
    private final int markColor;
    private final int textColor;
    private final float itemSpacing;
    private final float markHeight;
    private final float markRatio;
    private final float markWidth;
    private final float textSize;
    private final float selectedTextSize;
    private final float cursorSize;

    private List<String> items = new ArrayList<>();
    private OnWheelItemSelectedListener listener;
    private VelocityTracker velocityTracker;
    /** Content x-coordinate under the centre of the view; item i sits at i * itemSpacing. */
    private float offset;
    private int selectedIndex;
    private float lastTouchX;
    private boolean dragging;
    /** True while a scroll started by the user is still settling. */
    private boolean userScroll;

    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WheelView, defStyleAttr, 0);
        try {
            highlightColor = a.getColor(R.styleable.WheelView_wvHighlightColor, 0xFF28A69A);
            markColor = a.getColor(R.styleable.WheelView_wvMarkColor, 0xFFBDBDBD);
            textColor = a.getColor(R.styleable.WheelView_wvTextColor, 0xFF616161);
            itemSpacing = a.getDimension(R.styleable.WheelView_wvItemSpacing, dp(72));
            markRatio = a.getFloat(R.styleable.WheelView_wvMarkRatio, 0.5f);
        } finally {
            a.recycle();
        }
        markHeight = dp(14);
        markWidth = dp(1.5f);
        textSize = sp(18);
        selectedTextSize = sp(24);
        cursorSize = dp(9);

        markPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setTextAlign(Paint.Align.CENTER);
        cursorPaint.setColor(highlightColor);

        scroller = new OverScroller(context);
        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();
        minFlingVelocity = config.getScaledMinimumFlingVelocity();
        maxFlingVelocity = config.getScaledMaximumFlingVelocity();
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                int index = indexAt(offset + e.getX() - getWidth() / 2f);
                userScroll = true;
                scrollToIndex(index);
                return true;
            }
        });

        setFocusable(true);
        setClickable(true);
        if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    public void setItems(List<String> items) {
        this.items = new ArrayList<>(items);
        selectIndex(Math.min(selectedIndex, Math.max(0, this.items.size() - 1)));
        requestLayout();
    }

    public List<String> getItems() {
        return new ArrayList<>(items);
    }

    public void setOnWheelItemSelectedListener(OnWheelItemSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedPosition() {
        return selectedIndex;
    }

    /** Jumps to {@code index} without animating or notifying the listener. */
    public void selectIndex(int index) {
        scroller.forceFinished(true);
        userScroll = false;
        selectedIndex = clampIndex(index);
        offset = selectedIndex * itemSpacing;
        invalidate();
    }

    /** Animates to {@code index} without notifying the listener. */
    public void smoothSelectIndex(int index) {
        if (dragging) {
            return;
        }
        userScroll = false;
        scrollToIndex(index);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            stopDragging();
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) Math.ceil(cursorSize + dp(6) + markHeight + dp(6)
                + selectedTextSize + dp(8)) + getPaddingTop() + getPaddingBottom();
        // The ruler is meant to span the available width even when the layout says wrap_content.
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
                ? (int) (itemSpacing * 5) : MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty()) {
            return;
        }
        float centerX = getWidth() / 2f;
        float top = getPaddingTop();
        float markTop = top + cursorSize + dp(6);
        float markBottom = markTop + markHeight;
        float minorTop = markBottom - markHeight * markRatio;
        float textBaseline = markBottom + dp(6) + selectedTextSize;
        float minorSpacing = itemSpacing / (MINOR_MARKS_PER_ITEM + 1);
        int centerIndex = indexAt(offset);
        float alpha = isEnabled() ? 1f : 0.6f;

        int first = Math.max(0, (int) Math.floor((offset - centerX) / itemSpacing) - 1);
        int last = Math.min(items.size() - 1, (int) Math.ceil((offset + centerX) / itemSpacing) + 1);
        markPaint.setStrokeWidth(markWidth);
        for (int i = first; i <= last; i++) {
            float x = centerX + i * itemSpacing - offset;
            boolean selected = i == centerIndex;

            markPaint.setColor(withAlpha(selected ? highlightColor : markColor, alpha));
            canvas.drawLine(x, markTop, x, markBottom, markPaint);
            if (i < items.size() - 1) {
                markPaint.setColor(withAlpha(markColor, alpha));
                for (int m = 1; m <= MINOR_MARKS_PER_ITEM; m++) {
                    float mx = x + m * minorSpacing;
                    canvas.drawLine(mx, minorTop, mx, markBottom, markPaint);
                }
            }

            textPaint.setTextSize(selected ? selectedTextSize : textSize);
            textPaint.setColor(withAlpha(selected ? highlightColor : textColor, alpha));
            canvas.drawText(items.get(i), x, textBaseline, textPaint);
        }

        // Downward-pointing cursor above the centre mark.
        cursorPath.reset();
        cursorPath.moveTo(centerX - cursorSize, top);
        cursorPath.lineTo(centerX + cursorSize, top);
        cursorPath.lineTo(centerX, top + cursorSize);
        cursorPath.close();
        cursorPaint.setColor(withAlpha(highlightColor, alpha));
        canvas.drawPath(cursorPath, cursorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || items.isEmpty()) {
            return false;
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
        boolean tapped = gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                scroller.forceFinished(true);
                lastTouchX = event.getX();
                dragging = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                if (!dragging && Math.abs(dx) > touchSlop) {
                    dragging = true;
                    userScroll = true;
                    dx = 0;
                    lastTouchX = event.getX();
                }
                if (dragging) {
                    lastTouchX = event.getX();
                    setOffset(offset - dx);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                    float velocity = velocityTracker.getXVelocity();
                    if (Math.abs(velocity) > minFlingVelocity) {
                        scroller.fling((int) offset, 0, (int) -velocity, 0,
                                0, (int) maxOffset(), 0, 0);
                        postInvalidateOnAnimation();
                    } else {
                        scrollToIndex(indexAt(offset));
                    }
                } else if (tapped) {
                    performClick();
                } else {
                    scrollToIndex(indexAt(offset));
                }
                stopDragging();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    scrollToIndex(indexAt(offset));
                }
                stopDragging();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            setOffset(scroller.getCurrX());
            if (scroller.isFinished()) {
                settle();
            } else {
                postInvalidateOnAnimation();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled() && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            userScroll = true;
            scrollToIndex(selectedIndex + (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return android.widget.SeekBar.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (!items.isEmpty()) {
            info.setText(items.get(selectedIndex));
        }
        if (isEnabled() && selectedIndex > 0) {
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        if (isEnabled() && selectedIndex < items.size() - 1) {
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (isEnabled() && (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
            int step = action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1;
            int target = clampIndex(selectedIndex + step);
            if (target == selectedIndex) {
                return false;
            }
            selectIndex(target);
            notifyChanged();
            notifySelected();
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopDragging();
        super.onDetachedFromWindow();
    }

    private void scrollToIndex(int index) {
        int target = clampIndex(index);
        int dx = (int) (target * itemSpacing - offset);
        scroller.forceFinished(true);
        if (dx == 0) {
            setOffset(target * itemSpacing);
            settle();
            return;
        }
        scroller.startScroll((int) offset, 0, dx, 0, SNAP_DURATION_MS);
        postInvalidateOnAnimation();
    }

    /** Called when a scroll animation ends: snap exactly onto an item and report it if the user moved it. */
    private void settle() {
        int index = indexAt(offset);
        float snapped = index * itemSpacing;
        if (Math.abs(snapped - offset) >= 1f) {
            scrollToIndex(index);
            return;
        }
        offset = snapped;
        invalidate();
        if (userScroll) {
            userScroll = false;
            notifySelected();
        }
    }

    private void setOffset(float newOffset) {
        offset = Math.max(0, Math.min(maxOffset(), newOffset));
        int index = indexAt(offset);
        if (index != selectedIndex) {
            selectedIndex = index;
            if (userScroll) {
                notifyChanged();
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            }
        }
        invalidate();
    }

    private void stopDragging() {
        dragging = false;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onWheelItemChanged(this, selectedIndex);
        }
    }

    private void notifySelected() {
        if (listener != null) {
            listener.onWheelItemSelected(this, selectedIndex);
        }
    }

    private int indexAt(float contentX) {
        return clampIndex(Math.round(contentX / itemSpacing));
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(items.size() - 1, index));
    }

    private float maxOffset() {
        return Math.max(0, items.size() - 1) * itemSpacing;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
