package com.skyfishjy.library;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by fyu on 11/3/14.
 */

public class RippleBackground extends RelativeLayout implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {

    private static final int DEFAULT_RIPPLE_COUNT = 1;
    private static final int DEFAULT_DURATION_TIME = 3000;
    private static final float DEFAULT_SCALE = 600.0f;
    private static final int DEFAULT_FILL_TYPE = 0;

    private int rippleColor;
    private float rippleStrokeWidth;
    private float rippleRadius;
    private int rippleDurationTime;
    private int rippleAmount;
    private int rippleDelay;
    private float rippleScale;
    private float currentRippleScale;
    private int rippleType;
    private Paint paint;
    private boolean animationRunning = false;
    private AnimatorSet animatorSet;
    private ArrayList<Animator> animatorList;
    private LayoutParams rippleParams;
    private ArrayList<RippleView> rippleViewList = new ArrayList<RippleView>();

    Animator.AnimatorListener mAnimatorListener;

    private float hotspotX, hotspotY;
    private float rippleViewSize;

    public RippleBackground(Context context) {
        super(context);
    }

    public RippleBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RippleBackground(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(final Context context, final AttributeSet attrs) {
        if (isInEditMode())
            return;

        if (null == attrs) {
            throw new IllegalArgumentException("Attributes should be provided to this view,");
        }

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RippleBackground);
        rippleColor = typedArray.getColor(R.styleable.RippleBackground_rb_color, getResources().getColor(R.color.rippelColor));
        rippleStrokeWidth = typedArray.getDimension(R.styleable.RippleBackground_rb_strokeWidth, getResources().getDimension(R.dimen.rippleStrokeWidth));
        rippleRadius = typedArray.getDimension(R.styleable.RippleBackground_rb_radius, getResources().getDimension(R.dimen.rippleRadius));
        rippleDurationTime = typedArray.getInt(R.styleable.RippleBackground_rb_duration, DEFAULT_DURATION_TIME);
        rippleAmount = typedArray.getInt(R.styleable.RippleBackground_rb_rippleAmount, DEFAULT_RIPPLE_COUNT);
        rippleScale = typedArray.getFloat(R.styleable.RippleBackground_rb_scale, DEFAULT_SCALE);
        rippleType = typedArray.getInt(R.styleable.RippleBackground_rb_type, DEFAULT_FILL_TYPE);
        typedArray.recycle();

        rippleDelay = rippleDurationTime / rippleAmount;

        paint = new Paint();
        paint.setAntiAlias(true);
        if (rippleType == DEFAULT_FILL_TYPE) {
            rippleStrokeWidth = 0;
            paint.setStyle(Paint.Style.FILL);
        } else
            paint.setStyle(Paint.Style.STROKE);
        paint.setColor(rippleColor);


        rippleViewSize = 2 * (rippleRadius + rippleStrokeWidth);
        rippleParams = new LayoutParams((int) (rippleViewSize), (int) (rippleViewSize));
        //rippleParams.addRule(CENTER_IN_PARENT, TRUE);

        for (int i = 0; i < rippleAmount; i++) {
            RippleView rippleView = new RippleView(getContext());
            rippleView.setVisibility(GONE);
            addView(rippleView, rippleParams);
            rippleViewList.add(rippleView);
        }
    }

    public void setHotSpot(View view) {
        int[] viewXY = new int[2];
        int[] rippleXY = new int[2];
        view.getLocationInWindow(viewXY);
        this.getLocationInWindow(rippleXY);
        hotspotX = viewXY[0] + (view.getWidth() / 2) - rippleXY[0];
        hotspotY = viewXY[1] + (view.getHeight() / 2) - rippleXY[1];
        for (RippleView rippleView : rippleViewList) {
            rippleView.setHotspot(hotspotX, hotspotY);
        }
        updateScale();
    }

    public void setAnimatorListener(Animator.AnimatorListener animatorListener) {
        mAnimatorListener = animatorListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateScale();
    }

    /**
     * This calculates the correct scale
     */
    private void updateScale() {
        float layoutWidth = getWidth();
        float layoutHeight = getHeight();

        double hypotUpLeft = Math.hypot(hotspotX, hotspotY);
        double hypotUpRight = Math.hypot(layoutWidth - hotspotX, hotspotY);
        double hypotBottomLeft = Math.hypot(hotspotX, layoutHeight - hotspotY);
        double hypotBottomRight = Math.hypot(layoutWidth - hotspotX, layoutHeight - hotspotY);

        double max = Math.max(hypotBottomRight, Math.max(hypotBottomLeft, Math.max(hypotUpLeft, hypotUpRight)));

        rippleScale = (float) (max / rippleViewSize) * 2;
    }

    private void initAnimation(boolean reverse) {
        float startValue;
        float endValue = reverse ? 1.0f : rippleScale;

        //We should start from current value
        if (isRippleAnimationRunning()) {
            startValue = currentRippleScale;
            cancelRippleAnimation();
        } else {
            startValue = reverse ? rippleScale : 1.0f;
        }

        long duration = (long) (rippleDurationTime * Math.abs(startValue - endValue) / (rippleScale - 1.0f));

        animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorList = new ArrayList<Animator>();

        for (int i = 0; i < rippleViewList.size(); i++) {
            RippleView rippleView = rippleViewList.get(i);

            final ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(rippleView, "ScaleX", startValue, endValue);
            scaleXAnimator.setStartDelay(i * rippleDelay);
            scaleXAnimator.setDuration(duration);
            scaleXAnimator.addUpdateListener(this);
            animatorList.add(scaleXAnimator);

            final ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(rippleView, "ScaleY", startValue, endValue);
            scaleYAnimator.setStartDelay(i * rippleDelay);
            scaleYAnimator.setDuration(duration);
            animatorList.add(scaleYAnimator);
        }

        animatorSet.addListener(this);

        animatorSet.playTogether(animatorList);
    }

    @Override
    public void onAnimationStart(Animator animation) {
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationStart(animation);
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        animationRunning = false;
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationEnd(animation);
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        animationRunning = false;
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationCancel(animation);
        }
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationRepeat(animation);
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        currentRippleScale = (float) animation.getAnimatedValue();
        Log.d("Ciao", currentRippleScale + "");
    }

    private class RippleView extends View {

        private float hotspotX, hotspotY;

        public RippleView(Context context) {
            super(context);
            this.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            setX(hotspotX);
            setY(hotspotY);

            int radius = (Math.min(getWidth(), getHeight())) / 2;
            canvas.drawCircle(radius, radius, radius - rippleStrokeWidth, paint);
        }

        public void setHotspot(float x, float y) {
            hotspotY = y;
            hotspotX = x;
            invalidate();
        }
    }

    public void showRipple(boolean animate) {
        if (animate) {
            initAnimation(false);

            for (RippleView rippleView : rippleViewList) {
                rippleView.setVisibility(VISIBLE);
            }
            animatorSet.start();
            animationRunning = true;
        } else {
            for (RippleView rippleView : rippleViewList) {
                rippleView.setVisibility(VISIBLE);
                rippleView.setScaleX(rippleScale);
                rippleView.setScaleY(rippleScale);
            }
        }

    }

    public void hideRipple(boolean animate) {
        if (animate) {
            initAnimation(true);

            for (RippleView rippleView : rippleViewList) {
                rippleView.setVisibility(VISIBLE);
            }
            animatorSet.start();
            animationRunning = true;
        } else {
            for (RippleView rippleView : rippleViewList) {
                rippleView.setVisibility(GONE);
                rippleView.setScaleX(0f);
                rippleView.setScaleY(0f);
            }
        }

    }

    public void stopRippleAnimation() {
        if (isRippleAnimationRunning()) {
            animatorSet.end();
        }
    }

    public void cancelRippleAnimation() {
        if(isRippleAnimationRunning()) {
            animatorSet.cancel();
        }
    }

    public boolean isRippleAnimationRunning() {
        return animationRunning;
    }
}
