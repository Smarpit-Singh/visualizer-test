package com.example.android.visualizer_test.AudioVisuals;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.example.android.visualizer_test.R;


public class VisualizerView extends View {

    // These constants determine how much of a percentage of the audio frequencies each shape
    // represents. For example, the bass circle represents the bottom 10% of the frequencies.
    private static final float SEGMENT_SIZE = 100.f;
    private static final float BASS_SEGMENT_SIZE = 10.f / SEGMENT_SIZE;
    private static final float MID_SEGMENT_SIZE = 30.f / SEGMENT_SIZE;
    private static final float TREBLE_SEGMENT_SIZE = 60.f / SEGMENT_SIZE;

    // The minimum size of the shape, by default, before scaling
    private static final float MIN_SIZE_DEFAULT = 50;

    // This multiplier is used to make the frequency jumps a little more visually pronounced
    private static final float BASS_MULTIPLIER = 1.5f;
    private static final float MID_MULTIPLIER = 3;
    private static final float TREBLE_MULTIPLIER = 5;

    private static final float REVOLUTIONS_PER_SECOND = .3f;

    // Controls the Size of the circle each shape makes
    private static final float RADIUS_BASS = 20 / 100.f;
    private static final float RADIUS_MID = 60 / 100.f;
    private static final float RADIUS_TREBLE = 90 / 100.f;

    // The shapes
    private final TrailedShape mBassCircle;
    private final TrailedShape mMidSquare;
    private final TrailedShape mTrebleTriangle;

    // The array which keeps the current fft bytes
    private byte[] mBytes;

    // The time when the animation started
    private long mStartTime;

    // Numbers representing the current average of all the values in the bass, mid and treble range
    // in the fft
    private float bass;
    private float mid;
    private float treble;

    // Determines whether each of these should be shown
    private boolean showBass;
    private boolean showMid;
    private boolean showTreble;

    @ColorInt
    private int backgroundColor;

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBytes = null;
        TrailedShape.setMinSize(MIN_SIZE_DEFAULT);

        // Create each of the shapes and define how they are drawn on screen
        // Make bass circle
        mBassCircle = new TrailedShape(BASS_MULTIPLIER) {
            @Override
            protected void drawThisShape(float shapeCenterX, float shapeCenterY, float currentSize, Canvas canvas, Paint paint) {
                canvas.drawCircle(shapeCenterX, shapeCenterY, currentSize, paint);
            }
        };

        // Make midrange square
        mMidSquare = new TrailedShape(MID_MULTIPLIER) {
            @Override
            protected void drawThisShape(float shapeCenterX, float shapeCenterY, float currentSize, Canvas canvas, Paint paint) {
                canvas.drawRect(shapeCenterX - currentSize,
                        shapeCenterY - currentSize,
                        shapeCenterX + currentSize,
                        shapeCenterY + currentSize,
                        paint);
            }
        };

        // Make treble triangle
        mTrebleTriangle = new TrailedShape(TREBLE_MULTIPLIER) {
            @Override
            protected void drawThisShape(float shapeCenterX, float shapeCenterY, float currentSize, Canvas canvas, Paint paint) {
                Path trianglePath = new Path();
                trianglePath.moveTo(shapeCenterX, shapeCenterY - currentSize);
                trianglePath.lineTo(shapeCenterX + currentSize, shapeCenterY + currentSize / 2);
                trianglePath.lineTo(shapeCenterX - currentSize, shapeCenterY + currentSize / 2);
                trianglePath.lineTo(shapeCenterX, shapeCenterY - currentSize);
                canvas.drawPath(trianglePath, paint);
            }
        };
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Setup all the view measurement code after the view is laid out. If this is done any
        // earlier the height and width are not yet determined
        mStartTime = SystemClock.uptimeMillis();

        float viewCenterX = getWidth() / 2.f;
        float viewCenterY = getHeight() / 2.f;
        float shortSide = viewCenterX < viewCenterY ? viewCenterX : viewCenterY;
        TrailedShape.setViewCenterX(viewCenterX);
        TrailedShape.setViewCenterY(viewCenterY);

        mBassCircle.setShapeRadiusFromCenter(shortSide * RADIUS_BASS);
        mMidSquare.setShapeRadiusFromCenter(shortSide * RADIUS_MID);
        mTrebleTriangle.setShapeRadiusFromCenter(shortSide * RADIUS_TREBLE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBytes == null) {
            return;
        }

        // Get the current angle all of the shapes are at
        double currentAngleRadians = calcCurrentAngle();

        // Draw the background
        canvas.drawColor(backgroundColor);

        // Draw each shape
        if (showBass) {
            mBassCircle.draw(canvas, bass, currentAngleRadians);
        }
        if (showMid) {
            mMidSquare.draw(canvas, mid, currentAngleRadians);
        }
        if (showTreble) {
            mTrebleTriangle.draw(canvas, treble, currentAngleRadians);
        }

        // Invalidate the view to immediately redraw
        invalidate();
    }


    private double calcCurrentAngle() {
        long elapsedTime = SystemClock.uptimeMillis() - mStartTime;
        float revolutions = elapsedTime * REVOLUTIONS_PER_SECOND / 1000;
        return revolutions * 2 * Math.PI;
    }


    public void updateFFT(byte[] bytes) {
        mBytes = bytes;

        // Calculate average for bass segment
        float bassTotal = 0;
        for (int i = 0; i < bytes.length * BASS_SEGMENT_SIZE; i++) {
            bassTotal += Math.abs(bytes[i]);
        }
        bass = bassTotal / (bytes.length * BASS_SEGMENT_SIZE);

        // Calculate average for mid segment
        float midTotal = 0;
        for (int i = (int) (bytes.length * BASS_SEGMENT_SIZE); i < bytes.length * MID_SEGMENT_SIZE; i++) {
            midTotal += Math.abs(bytes[i]);
        }
        mid = midTotal / (bytes.length * MID_SEGMENT_SIZE);

        // Calculate average for terble segment
        float trebleTotal = 0;
        for (int i = (int) (bytes.length * MID_SEGMENT_SIZE); i < bytes.length; i++) {
            trebleTotal += Math.abs(bytes[i]);
        }
        treble = trebleTotal / (bytes.length * TREBLE_SEGMENT_SIZE);

        invalidate();
    }


    public void restart() {
        mBassCircle.restartTrail();
        mMidSquare.restartTrail();
        mTrebleTriangle.restartTrail();
    }


    public void setShowBass(boolean showBass) {
        this.showBass = showBass;
    }


    public void setShowMid(boolean showMid) {
        this.showMid = showMid;
    }


    public void setShowTreble(boolean showTreble) {
        this.showTreble = showTreble;
    }


    public void setMinSizeScale(float scale) {
        TrailedShape.setMinSize(MIN_SIZE_DEFAULT * scale);
    }


    public void setColor(String newColorKey) {

        @ColorInt
        int shapeColor;

        @ColorInt
        int trailColor;

        if (newColorKey.equals(getContext().getString(R.string.pref_color_blue_value))) {
            shapeColor = ContextCompat.getColor(getContext(), R.color.shapeBlue);
            trailColor = ContextCompat.getColor(getContext(), R.color.trailBlue);
            backgroundColor = ContextCompat.getColor(getContext(), R.color.backgroundBlue);
        } else if (newColorKey.equals(getContext().getString(R.string.pref_color_green_value))) {
            shapeColor = ContextCompat.getColor(getContext(), R.color.shapeGreen);
            trailColor = ContextCompat.getColor(getContext(), R.color.trailGreen);
            backgroundColor = ContextCompat.getColor(getContext(), R.color.backgroundGreen);
        } else {
            shapeColor = ContextCompat.getColor(getContext(), R.color.shapeRed);
            trailColor = ContextCompat.getColor(getContext(), R.color.trailRed);
            backgroundColor = ContextCompat.getColor(getContext(), R.color.backgroundRed);
        }

        mBassCircle.setShapeColor(shapeColor);
        mMidSquare.setShapeColor(shapeColor);
        mTrebleTriangle.setShapeColor(shapeColor);

        mBassCircle.setTrailColor(trailColor);
        mMidSquare.setTrailColor(trailColor);
        mTrebleTriangle.setTrailColor(trailColor);
    }
}
