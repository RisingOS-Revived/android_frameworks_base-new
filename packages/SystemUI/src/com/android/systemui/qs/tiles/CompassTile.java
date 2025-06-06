/*
 * Copyright (C) 2019-2024 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;

import javax.inject.Inject;

public class CompassTile extends QSTileImpl<BooleanState> implements SensorEventListener {

    public static final String TILE_SPEC = "compass";

    private final static float ALPHA = 0.97f;

    private boolean mActive = false;

    private SensorManager mSensorManager;
    private Sensor mAccelerationSensor;
    private Sensor mGeomagneticFieldSensor;

    private float[] mAcceleration;
    private float[] mGeomagnetic;

    private boolean mListeningSensors;

    @Inject
    public CompassTile(QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger
    ) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGeomagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public BooleanState newTileState() {
        BooleanState state = new BooleanState();
        state.handlesLongClick = false;
        return state;
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        setListeningSensors(false);
        mSensorManager = null;
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        mActive = !mActive;
        refreshState();
        setListeningSensors(mActive);
    }

    @Override
    public void handleLongClick(@Nullable Expandable expandable) {
        handleClick(expandable);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    private void setListeningSensors(boolean listening) {
        if (listening == mListeningSensors) return;
        mListeningSensors = listening;
        if (mListeningSensors) {
            mSensorManager.registerListener(
                    this, mAccelerationSensor, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(
                    this, mGeomagneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_compass_label);
    }

    private Drawable rotateDrawable(Drawable drawable, float degrees) {
        // Convert drawable to bitmap
        Bitmap bitmap = drawableToBitmap(drawable);

        // Create matrix for rotation
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        // Create rotated bitmap
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Convert rotated bitmap back to drawable
        return new BitmapDrawable(mContext.getResources(), rotatedBitmap);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // If the drawable is not a BitmapDrawable, create a new bitmap and draw the drawable on a canvas
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final Float degrees = arg == null ? 0 : (Float) arg;

        state.value = mActive;

        if (state.value) {
            state.state = Tile.STATE_ACTIVE;
            if (arg != null) {
                state.label = formatValueWithCardinalDirection(degrees);
            } else {
                state.label = mContext.getString(R.string.quick_settings_compass_init);
            }
        } else {
            state.label = mContext.getString(R.string.quick_settings_compass_label);
            state.state = Tile.STATE_INACTIVE;
        }
        state.icon = new DrawableIcon(rotateDrawable(
                mContext.getResources().getDrawable(R.drawable.ic_qs_compass), degrees));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public boolean isAvailable() {
        return mSensorManager != null && mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
                && mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (!listening) {
            setListeningSensors(false);
            mActive = false;
        }
    }

    private String formatValueWithCardinalDirection(float degree) {
        int cardinalDirectionIndex = (int) (Math.floor(((degree - 22.5) % 360) / 45) + 1) % 8;
        String[] cardinalDirections = mContext.getResources().getStringArray(
                R.array.cardinal_directions);

        return mContext.getString(R.string.quick_settings_compass_value, degree,
                cardinalDirections[cardinalDirectionIndex]);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (mAcceleration == null) {
                mAcceleration = event.values.clone();
            }

            values = mAcceleration;
        } else {
            // Magnetic field sensor
            if (mGeomagnetic == null) {
                mGeomagnetic = event.values.clone();
            }

            values = mGeomagnetic;
        }

        for (int i = 0; i < 3; i++) {
            values[i] = ALPHA * values[i] + (1 - ALPHA) * event.values[i];
        }

        if (!mActive || !mListeningSensors || mAcceleration == null || mGeomagnetic == null) {
            // Nothing to do at this moment
            return;
        }

        float R[] = new float[9];
        float I[] = new float[9];
        if (!SensorManager.getRotationMatrix(R, I, mAcceleration, mGeomagnetic)) {
            // Rotation matrix couldn't be calculated
            return;
        }

        // Get the current orientation
        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);

        // Convert azimuth to degrees
        Float newDegree = Float.valueOf((float) Math.toDegrees(orientation[0]));
        newDegree = (newDegree + 360) % 360;

        // Convert the angle to one that points north relative to the device
        newDegree = -newDegree + 360;

        refreshState(newDegree);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // noop
    }
}
