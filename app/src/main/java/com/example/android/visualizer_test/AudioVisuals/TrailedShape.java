package com.example.android.visualizer_test.AudioVisuals;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.ColorInt;

import java.util.LinkedList;


abstract class TrailedShape {

    // Static variables for the center of the parent view and the minimum size of all of the shapes
    private static float sViewCenterX, sViewCenterY;
    private static float sMinSize;

    // Variables for determining size
    private final float mMultiplier;

    // Variables for determining trail
    private final Path mTrailPath;
    private final LinkedList<TrailPoint> mTrailList;

    // Paint for drawing
    private final Paint mPaint;
    private Paint mTrailPaint;

    // Variable for determining position
    private float mShapeRadiusFromCenter;



    TrailedShape(float multiplier) {
        this.mMultiplier = multiplier;

        // Setup trail variables
        this.mTrailPath = new Path();
        this.mTrailList = new LinkedList<>();

        // Setup paint and attributes
        this.mPaint = new Paint();
        this.mTrailPaint = new Paint();

        mPaint.setStyle(Paint.Style.FILL);
        mTrailPaint.setStyle(Paint.Style.STROKE);
        mTrailPaint.setStrokeWidth(5);
        mTrailPaint.setStrokeJoin(Paint.Join.ROUND);
        mTrailPaint.setStrokeCap(Paint.Cap.ROUND);
    }


    // Static methods
    static void setMinSize(float minSize) {
        TrailedShape.sMinSize = minSize;
    }


    static void setViewCenterY(float viewCenterY) {
        TrailedShape.sViewCenterY = viewCenterY;
    }


    static void setViewCenterX(float viewCenterX) {
        TrailedShape.sViewCenterX = viewCenterX;
    }


    void draw(Canvas canvas, float currentFreqAve, double currentAngle) {

        float currentSize = sMinSize + mMultiplier * currentFreqAve;

        // Calculate where the shape is
        float shapeCenterX = calcLocationInAnimationX(mShapeRadiusFromCenter, currentAngle);
        float shapeCenterY = calcLocationInAnimationY(mShapeRadiusFromCenter, currentAngle);

        // Calculate where the next point in the trail is
        float trailX = calcLocationInAnimationX((mShapeRadiusFromCenter + currentSize - sMinSize), currentAngle);
        float trailY = calcLocationInAnimationY((mShapeRadiusFromCenter + currentSize - sMinSize), currentAngle);

        mTrailPath.rewind(); // clear the trail
        mTrailList.add(new TrailPoint(trailX, trailY, currentAngle)); // add the new line segment

        // Keep the trail size correct
        while (currentAngle - mTrailList.peekFirst().theta > 2 * Math.PI) {
            mTrailList.poll();
        }

        // Draw the trail
        mTrailPath.moveTo(mTrailList.peekFirst().x, mTrailList.peekFirst().y);
        for (TrailPoint trailPoint : mTrailList) {
            mTrailPath.lineTo(trailPoint.x, trailPoint.y);
        }

        canvas.drawPath(mTrailPath, mTrailPaint);

        // Call the abstract drawThisShape method, this must be defined for each shape.
        drawThisShape(shapeCenterX, shapeCenterY, currentSize, canvas, mPaint);
    }


    protected abstract void drawThisShape(float shapeCenterX, float shapeCenterY, float currentSize, Canvas canvas, Paint paint);


    void restartTrail() {
        mTrailList.clear();
    }


    private float calcLocationInAnimationX(float radiusFromCenter, double currentAngleRadians) {
        return (float) (sViewCenterX + Math.cos(currentAngleRadians) * radiusFromCenter);

    }


    private float calcLocationInAnimationY(float radiusFromCenter, double currentAngleRadians) {
        return (float) (sViewCenterY + Math.sin(currentAngleRadians) * radiusFromCenter);
    }


    void setShapeColor(@ColorInt int color) {
        mPaint.setColor(color);
    }


    void setTrailColor(@ColorInt int color) {
        mTrailPaint.setColor(color);
    }


    void setShapeRadiusFromCenter(float mShapeRadiusFromCenter) {
        this.mShapeRadiusFromCenter = mShapeRadiusFromCenter;
    }


    private class TrailPoint {
        final float x;
        final float y;
        final double theta;

        TrailPoint(float x, float y, double theta) {
            this.x = x;
            this.y = y;
            this.theta = theta;
        }
    }
}
