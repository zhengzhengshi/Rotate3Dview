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
    private final static int NONE_SCROLL = 0;
    private final static int HORIZONTAL_SCROLL = 1;
    private final static int VERTICAL_SCROLL = 2;
    private int mWidth;
    private int mHeight;
    private float mDownX;
    private float mDownY;
    private Camera mCamera;
    private Matrix mMatrix;
    private Scroller mScroller;
    private Context mContext;
    private VelocityTracker mVelocityTracker;
    private int mCurrentScrollDir = NONE_SCROLL;

    private final static int[][] HORIZONTAL_ARRAY = {
            {1, 6, 3, 5}, {2, 6, 4, 5}, {3, 6, 1, 5}, {4, 6, 2, 5}, {5, 2, 6, 4}, {6, 4, 5, 2}
    };
    private final static int[][] VERTICAL_ARRAY = {
            {1, 2, 3, 4}, {2, 3, 4, 1}, {3, 4, 1, 2}, {4, 1, 2, 3}, {5, 3, 6, 1}, {6, 3, 5, 1}
    };

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
        if (mCurrentScrollDir == NONE_SCROLL) {
            View childView = getChildAt(0);
            childView.layout(0, 0, mWidth, mHeight);
        } else if (mCurrentScrollDir == VERTICAL_SCROLL) {
            onLayoutVertical();
        } else if (mCurrentScrollDir == HORIZONTAL_SCROLL) {
            onLayoutHorizontal();
        }
    }

    private void onLayoutHorizontal() {
        for (int i = 0; i < 4; i++) {
            //TODO:
            View childView = getChildAt(i);
            int top = 0;
            int left = i * mWidth;
            childView.layout(left, top, left + mWidth, top + mHeight);
        }
    }

    private void onLayoutVertical() {
        for (int i = 0; i < 4; i++) {
            //TODO:
            View childView = getChildAt(i);
            int top = i * mHeight;
            int left = 0;
            childView.layout(left, top, left + mWidth, top + mHeight);
        }
    }


/*    @Override
    protected void dispatchDraw(Canvas canvas) {
        for (int i = 0; i < getChildCount(); i++) {
            drawPage(canvas, i, getDrawingTime());
        }
    }*/

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
                mDownX = event.getX();
                mDownY = event.getY();
                if (!mScroller.isFinished()) {
                    mScroller.setFinalX(mScroller.getCurrX());
                    mScroller.setFinalY(mScroller.getCurrY());
                    mScroller.abortAnimation();
                    scrollTo(getScrollX(), getScrollY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = mDownX - event.getX();
                float deltaY = mDownY - event.getY();
                if (deltaX == 0 && deltaY == 0) {
                    break;
                }
                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    //horizontal scroll.
                    if (mCurrentScrollDir == NONE_SCROLL || mCurrentScrollDir == HORIZONTAL_SCROLL) {
                        recycleViewHorizontal();
                        mCurrentScrollDir = HORIZONTAL_SCROLL;
                        requestLayout();
                        setScrollY(0);
                        scrollBy((int) deltaX, 0);
                    }
                } else {
                    //vertical scroll.
                    if (mCurrentScrollDir == NONE_SCROLL || mCurrentScrollDir == VERTICAL_SCROLL) {
                       recycleViewVertical();
                        mCurrentScrollDir = VERTICAL_SCROLL;
                        requestLayout();
                        setScrollX(0);
                        scrollBy(0, (int) deltaY);
                    }
                }
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                int scrollDelta;
                int scrollValue;
                if(mCurrentScrollDir==VERTICAL_SCROLL){
                    scrollDelta = Math.abs(getScrollY()) % mHeight;
                    scrollValue = scrollDelta > mHeight / 2 ? mHeight - scrollDelta : -scrollDelta;
                    movePageVertical(scrollValue);
                }else if(mCurrentScrollDir ==HORIZONTAL_SCROLL){
                    scrollDelta = Math.abs(getScrollX()) % mWidth;
                    scrollValue = scrollDelta > mWidth / 2 ? mWidth - scrollDelta : -scrollDelta;
                    movePageHorizontal(scrollValue);
                }

               /* int scrollDelta = Math.abs(getScrollY()) % mHeight;
                if (mVelocityTracker.getYVelocity() > 300) {
                    movePage(-scrollDelta);
                } else if (mVelocityTracker.getYVelocity() < -300) {
                    movePage(mHeight - scrollDelta);
                } else {
                    int scrollValue = scrollDelta > mHeight / 2 ? mHeight - scrollDelta : -scrollDelta;
                    movePage(scrollValue);
                }*/

                mCurrentScrollDir = NONE_SCROLL;
                break;
        }
        return true;
    }

    private void recycleViewHorizontal() {
        if (getScrollX() < 0) {
            addPrePage();
        } else if (getScrollX() > 3 * mWidth) {
            addNextPage();
        }
    }

    private void recycleViewVertical() {
        if (getScrollY() < 0) {
            addPrePage();
        } else if (getScrollY() > 3 * mHeight) {
            addNextPage();
        }
    }

    private void addPrePage() {
        View childView = getChildAt(3);
        removeViewAt(3);
        addView(childView, 0);
        if (mCurrentScrollDir == VERTICAL_SCROLL) {
            scrollBy(0, mHeight);
        } else if (mCurrentScrollDir == HORIZONTAL_SCROLL) {
            scrollBy(mWidth, 0);
        }
    }

    private void addNextPage() {
        View childView = getChildAt(0);
        removeViewAt(0);
        addView(childView, 3);
        if (mCurrentScrollDir == VERTICAL_SCROLL) {
            scrollBy(0, -mHeight);
        } else if (mCurrentScrollDir == HORIZONTAL_SCROLL) {
            scrollBy(-mWidth, 0);
        }
    }

    private void movePageHorizontal(int scrollValue) {
        mScroller.startScroll(getScrollX(), 0, scrollValue, 0, 1000);
        invalidate();
    }

    private void movePageVertical(int scrollValue) {
        mScroller.startScroll(0, getScrollY(), 0, scrollValue, 1000);
        invalidate();
    }
}
