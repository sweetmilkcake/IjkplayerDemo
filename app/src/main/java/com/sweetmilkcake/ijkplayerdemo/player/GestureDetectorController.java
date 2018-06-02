package com.sweetmilkcake.ijkplayerdemo.player;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureDetectorController implements GestureDetector.OnGestureListener {

    private static final String TAG = GestureDetectorController.class.getSimpleName();
    private GestureDetector mGestureDetector;
    private IGestureListener mGestureListener;
    private int mWidth;
    private ScrollType mCurrentType;

    public GestureDetectorController(Context context, IGestureListener listener) {
        mWidth = context.getResources().getDisplayMetrics().widthPixels;
        mGestureListener = listener;
        mGestureDetector = new GestureDetector(context, this);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mCurrentType = ScrollType.NOTHING;
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mGestureListener != null) {
            if (mCurrentType != ScrollType.NOTHING) {
                switch (mCurrentType) {
                    case VERTICAL_LEFT:
                        mGestureListener.onScrollVerticalLeft(distanceY, e1.getY() - e2.getY());
                        break;
                    case VERTICAL_RIGHT:
                        mGestureListener.onScrollVerticalRight(distanceY, e1.getY() - e2.getY());
                        break;
                    case HORIZONTAL:
                        mGestureListener.onScrollHorizontal(distanceX, e2.getX() - e1.getX());
                        break;
                }
                return false;
            }
        }
        if (Math.abs(distanceY) <= Math.abs(distanceX)) {
            mCurrentType = ScrollType.HORIZONTAL;
            mGestureListener.onScrollStart(mCurrentType);
            return false;
        }
        int i = mWidth / 3;
        if (e1.getX() <= i) {
            mCurrentType = ScrollType.VERTICAL_LEFT;
            mGestureListener.onScrollStart(mCurrentType);
        } else if (e1.getX() > i * 2) {
            mCurrentType = ScrollType.VERTICAL_RIGHT;
            mGestureListener.onScrollStart(mCurrentType);
        } else {
            mCurrentType = ScrollType.NOTHING;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public enum ScrollType {
        NOTHING,
        VERTICAL_LEFT,
        VERTICAL_RIGHT,
        HORIZONTAL
    }

    public interface IGestureListener {
        void onScrollStart(ScrollType type);

        void onScrollHorizontal(float x1, float x2);

        void onScrollVerticalLeft(float y1, float y2);

        void onScrollVerticalRight(float y1, float y2);
    }
}
