package com.zhengzs.rotate3dview;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.print.PrinterId;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

public class Rotate3dView extends ViewGroup {
    private int mWidth;
    private int mHeight;
    private float mDown;
    private Camera mCamera;
    private Matrix mMatrix;
    private Scroller mScroller;
    private Context mContext;
    private VelocityTracker mVelocityTracker;

    public Rotate3dView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mMatrix = new Matrix();
        mCamera = new Camera();
        mScroller = new Scroller(mContext);
        mVelocityTracker = VelocityTracker.obtain();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mWidth = width;
        mHeight = height;
        setMeasuredDimension(width, height);

        int childWSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        measureChildren(childWSpec, childHSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            int top = i * mHeight;
            int left = 0;
            childView.layout(0, top, left + mWidth, top + mHeight);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        for (int i = 0; i < getChildCount(); i++) {
            drawPage(canvas, i, getDrawingTime());
        }
    }

    private void drawPage(Canvas canvas, int index, long drawTime) {
        int top = index * mHeight;
        int bottom = (index + 1) * mHeight;
        int scrollY = getScrollY();
        if (scrollY > bottom || scrollY + mHeight < top) {
            return;
        }
        mCamera.save();
        canvas.save();
        mMatrix.reset();

        int degree = 90 * (getScrollY() - index * mHeight) / mHeight;
        int centerX = mWidth / 2;
        int centerY = getScrollY() > index * mHeight ? (index + 1) * mHeight : index * mHeight;
        mCamera.rotateX(degree);
        mCamera.getMatrix(mMatrix);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        drawChild(canvas, getChildAt(index), drawTime);
        canvas.restore();
        mCamera.restore();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        mVelocityTracker.computeCurrentVelocity(1000);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDown = event.getY();
                if (!mScroller.isFinished()) {
                    mScroller.setFinalY(mScroller.getCurrY());
                    mScroller.abortAnimation();
                    scrollTo(0, getScrollY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float delta = mDown - event.getY();
                mDown = event.getY();
                scrollBy(0, (int) delta);
                if(mScroller.isFinished()) {
                    if (getScrollY() < 0) {
                        addPrePage();
                        break;
                    }
                    if (getScrollY() > (getChildCount() - 1) * mHeight) {
                        addNextPage();
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                int scrollDelta = Math.abs(getScrollY()) % mHeight;
                if (mVelocityTracker.getYVelocity() > 300) {
                    movePage(-scrollDelta);
                } else if (mVelocityTracker.getYVelocity() < -300) {
                    movePage(mHeight - scrollDelta);
                } else {
                    int scrollValue = scrollDelta > mHeight / 2 ? mHeight - scrollDelta : -scrollDelta;
                    movePage(scrollValue);
                }
                break;
        }
        return true;
    }

    private void addPrePage() {
        View childView = getChildAt(getChildCount() - 1);
        removeViewAt(getChildCount() - 1);
        addView(childView, 0);
        scrollBy(0, mHeight);
    }

    private void addNextPage() {
        View childView = getChildAt(0);
        removeViewAt(0);
        addView(childView, getChildCount());
        scrollBy(0, -mHeight);
    }

    private void movePage(int scrollValue) {
        mScroller.startScroll(0, getScrollY(), 0, scrollValue, 1000);
        invalidate();
    }
}
