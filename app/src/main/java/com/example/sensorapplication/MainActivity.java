package com.example.sensorapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;
    private TextView mTextSensorAzimuth;
    private TextView mTextSensorPitch;
    private TextView mTextSensorRoll;
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    private ImageView mSpotTop;
    private ImageView mSpotBottom;
    private ImageView mSpotLeft;
    private ImageView mSpotRight;
    private float mCircleX, mCircleY;
    private ImageView mCircle;
    private ImageView mTargetCircle;
    private float mTargetCircleX, mTargetCircleY;
    private int score = 0;
    private TextView mTextSensorScore;
    private static final float VALUE_DRIFT = 0.05f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mTextSensorAzimuth = (TextView) findViewById(R.id.value_azimuth);
        mTextSensorPitch = (TextView) findViewById(R.id.value_pitch);
        mTextSensorRoll = (TextView) findViewById(R.id.value_roll);
        mSpotTop = (ImageView) findViewById(R.id.spot_top);
        mSpotBottom = (ImageView) findViewById(R.id.spot_bottom);
        mSpotLeft = (ImageView) findViewById(R.id.spot_left);
        mSpotRight = (ImageView) findViewById(R.id.spot_right);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mCircle = findViewById(R.id.circle); // initialize circle ImageView
        // set initial position of the circle to the center of the screen
        mCircleX = getWindowManager().getDefaultDisplay().getWidth() / 2f;
        mCircleY = getWindowManager().getDefaultDisplay().getHeight() / 2f;
        mCircle.setX(mCircleX);
        mCircle.setY(mCircleY);

        mTargetCircle = findViewById(R.id.larger_circle); // initialize target circle ImageView
        mTargetCircle.setX(mTargetCircleX);
        mTargetCircle.setY(mTargetCircleY);
        mTextSensorScore = findViewById(R.id.value_score); // initialize score text view

    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = sensorEvent.values.clone();
                break;
            default:
                return;
        }

        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix, null,
                mAccelerometerData, mMagnetometerData);
        float[] orientationValues = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrix, orientationValues);
        }

        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];

        if (Math.abs(pitch) < VALUE_DRIFT) {
            pitch = 0;
        }
        if (Math.abs(roll) < VALUE_DRIFT) {
            roll = 0;
        }

        mTextSensorAzimuth.setText(getResources().getString(R.string.value_format, azimuth));
        mTextSensorPitch.setText(getResources().getString(R.string.value_format, pitch));
        mTextSensorRoll.setText(getResources().getString(R.string.value_format, roll));

        mSpotTop.setAlpha(0f);
        mSpotBottom.setAlpha(0f);
        mSpotLeft.setAlpha(0f);
        mSpotRight.setAlpha(0f);
        //radian values overflow 1.0 max, but it's okay
        //  we don't care about tracking the full device tilt
        if (pitch > 0) {
            mSpotBottom.setAlpha(pitch);
        } else {
            mSpotTop.setAlpha(Math.abs(pitch));
        }
        if (roll > 0) {
            mSpotLeft.setAlpha(roll);
        } else {
            mSpotRight.setAlpha(Math.abs(roll));
        }

        // calculate translation values for the circle
        float translationX = -mAccelerometerData[0] * 10;
        float translationY = mAccelerometerData[1] * 10;

        // update circle position
        mCircleX += translationX;
        mCircleY += translationY;

        // bound circle position to screen bounds
        mCircleX = Math.max(0, Math.min(getWindowManager().getDefaultDisplay().getWidth() - mCircle.getWidth(), mCircleX));
        mCircleY = Math.max(0, Math.min(getWindowManager().getDefaultDisplay().getHeight() - mCircle.getHeight(), mCircleY));

        // update circle position
        mCircle.setX(mCircleX);
        mCircle.setY(mCircleY);

        if (isColliding(mCircle, mTargetCircle)) {
            // move the target circle to a random location on the screen
            Random random = new Random();
            int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
            int screenHeight = getWindowManager().getDefaultDisplay().getHeight();

            int circleWidth = mTargetCircle.getWidth();
            int circleHeight = mTargetCircle.getHeight();

            float randomX = random.nextFloat() * (screenWidth - circleWidth);
            float randomY = random.nextFloat() * (screenHeight - circleHeight);

            // ensure target circle stays within screen bounds
            randomX = Math.max(0, Math.min(screenWidth - circleWidth, randomX));
            randomY = Math.max(0, Math.min(screenHeight - circleHeight, randomY));

            mTargetCircle.setX(randomX);
            mTargetCircle.setY(randomY);
            score++; // update score
            mTextSensorScore.setText(getResources().getString(R.string.score_format, score)); // display updated score

        }
    }
    // function to see if two circles are colliding
    private boolean isColliding(View circle1, View circle2) {
        // Get the center coordinates and radius of each circle
        float circle1X = circle1.getX() + circle1.getWidth() / 2f;
        float circle1Y = circle1.getY() + circle1.getHeight() / 2f;
        float circle1Radius = circle1.getWidth() / 2f;

        float circle2X = circle2.getX() + circle2.getWidth() / 2f;
        float circle2Y = circle2.getY() + circle2.getHeight() / 2f;
        float circle2Radius = circle2.getWidth() / 2f;

        // Calculate the distance between the centers of the circles
        float distance = (float) Math.sqrt(Math.pow(circle2X - circle1X, 2) + Math.pow(circle2Y - circle1Y, 2));

        // Check if the circles are colliding
        return distance < (circle1Radius + circle2Radius);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //intentionally blank
    }
}