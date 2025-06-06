/*
 * Copyright (C) 2023-2024 The RisingOS Android Project
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
package org.rising.server;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager; 
import android.os.UserHandle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telecom.TelecomManager;
import android.util.Log;
import android.provider.Settings;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.android.internal.R;

public class PocketModeService {

    private static final float PROXIMITY_THRESHOLD = 1.0f;
    private static final float GRAVITY_THRESHOLD = -0.6f;
    private static final int MIN_INCLINATION = 75;
    private static final int MAX_INCLINATION = 100;

    public static final String ACTION_POCKET_STATE_CHANGED = "org.rising.server.action.POCKET_STATE_CHANGED";
    public static final String EXTRA_IN_POCKET = "in_pocket";
    
    private static final long DISPLAY_OFF_DELAY = 3000;
    private Handler mDisplayOffHandler = new Handler();
    private Runnable mDisplayOffRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isCallInProgress() && isDeviceOnKeyguard()) {
                hideOverlay();
                if (mPowerManager != null) {
                    mPowerManager.goToSleep(SystemClock.uptimeMillis());
                }
            }
        }
    };

    private float[] gravityValues;
    private float proximitySensorValue;
    private int inclinationAngle;

    private Context mContext;
    private View mOverlayView;
    private WindowManager mWindowManager;
    private GestureDetector mGestureDetector;
    private PowerManager mPowerManager;
    private WindowManager.LayoutParams mLayoutParams;
    private TelecomManager mTelecomManager;

    private BroadcastReceiver mScreenStateReceiver;
    private SettingsObserver mSettingsObserver;

    private static final String ALWAYS_ON_POCKET_MODE_ENABLED = "always_on_pocket_mode_enabled";
    private static final String POCKET_MODE_ENABLED = "pocket_mode_enabled";
    private final Handler mHandler = new Handler();
    
    private static PocketModeService instance = null;

    private KeyguardManager mKeyguardManager;
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mProximitySensor;

    boolean mIsInPocket;
    boolean mIsOverlayUserUnlocked;
    private boolean mShowing = false;
    private boolean mSystemReady = false;

    private static final int POCKET_MODE_SENSOR_DELAY = 400000;
    
    private VibrationEffect mDoubleClickEffect;
    private Vibrator mVibrator;
    
    private boolean mDozing = false;
    
    private BroadcastReceiver mAodStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mDozing = intent.getBooleanExtra("is_doze", false);
            Log.d("PocketModeService", mDozing ? "Dozing" : "not dozing");
            onDozingChanged();
        }
    };

    private PocketModeService(Context context) {
        mContext = context;
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        if (mSensorManager != null) {
            mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mProximitySensor = !isExcludedDevice() ? mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) : null;
        }
        mDoubleClickEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);
        createOverlayView(mContext);
        mSettingsObserver = new SettingsObserver(null);
        mSettingsObserver.observe();
        IntentFilter filter = new IntentFilter("org.rising.server.action.AOD_STATE_CHANGED");
        mContext.registerReceiver(mAodStateReceiver, filter, Context.RECEIVER_EXPORTED);
    }
    
    public static synchronized PocketModeService getInstance(Context context) {
        if (instance == null) {
            instance = new PocketModeService(context);
        }
        return instance;
    }

    private void sendPocketStateChangedBroadcast(boolean inPocket) {
        Intent intent = new Intent(ACTION_POCKET_STATE_CHANGED);
        intent.setPackage("com.android.systemui");
        intent.putExtra(EXTRA_IN_POCKET, inPocket);
        mContext.sendBroadcastAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
    }
    
    private void onDozingChanged() {
        if (mDozing) {
            hideOverlay();
        } else {
            onInteractiveChanged(true);
        }
    }
    
    private boolean isAlwaysOnPocketMode() {
        return Settings.Secure.getIntForUser(
                mContext.getContentResolver(),
                ALWAYS_ON_POCKET_MODE_ENABLED,
                0, ActivityManager.getCurrentUser()) == 1;
    }

    private boolean isPocketModeEnabled() {
        return Settings.Secure.getIntForUser(
                mContext.getContentResolver(),
                POCKET_MODE_ENABLED,
                0, ActivityManager.getCurrentUser()) == 1;
    }

    private void registerListeners() {
        registerSensorListeners();
        initializeGestureDetector(mContext);
        mOverlayView.setOnTouchListener(mOverlayTouchListener);
    }

    private void unregisterListeners() {
        unregisterSensorListeners();
        mGestureDetector = null;
        mOverlayView.setOnTouchListener(null);
    }
    
    public void onInteractiveChanged(boolean show) {
        if (!isPocketModeEnabled() && mShowing) {
            hideOverlay();
            return;
        }
        mIsOverlayUserUnlocked = false;
        if ((show && isAlwaysOnPocketMode() || mIsInPocket) 
            && !mIsOverlayUserUnlocked && isPocketModeEnabled()) {
            showOverlay();
        } else {
            hideOverlay();
        }
    }
    
    public void disablePocketLock() {
        vibrate(mDoubleClickEffect);
        hideOverlay();
        mIsOverlayUserUnlocked = true;
    }

    private void showOverlay() {
        if (!mSystemReady || mDozing) return;
        final Runnable show = new Runnable() {
            @Override
            public void run() {
                if (!isCallInProgress() && mWindowManager != null && !mShowing && isDeviceOnKeyguard()) {
                    mWindowManager.addView(mOverlayView, mLayoutParams);
                    mOverlayView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    mShowing = true;
                    startDisplayOffTimer();
                    sendPocketStateChangedBroadcast(true);
                }
            }
        };
        mHandler.post(show);
    }

    private void hideOverlay() {
        if (!mSystemReady) return;
        final Runnable hide = new Runnable() {
            @Override
            public void run() {
                if (mWindowManager != null && mShowing) {
                    mOverlayView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                    mWindowManager.removeView(mOverlayView);
                    mShowing = false;
                    cancelDisplayOffTimer();
                    sendPocketStateChangedBroadcast(false);
                }
            }
        };
        mHandler.post(hide);
    }

    private View.OnTouchListener mOverlayTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            return true;
        }
    };

    private void registerSensorListeners() {
        if (mSensorManager != null) {
            if (mAccelerometerSensor != null) {
                mSensorManager.registerListener(mPocketModeListener, mAccelerometerSensor, POCKET_MODE_SENSOR_DELAY, mHandler);
            }
            if (mProximitySensor != null) {
                mSensorManager.registerListener(mPocketModeListener, mProximitySensor, POCKET_MODE_SENSOR_DELAY, mHandler);
            }
        }
    }

    private void unregisterSensorListeners() {
        if (mSensorManager != null && mPocketModeListener != null) {
            mSensorManager.unregisterListener(mPocketModeListener);
        }
    }
    
    private final SensorEventListener mPocketModeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravityValues = sensorEvent.values.clone();
                double gravityMagnitude = Math.sqrt(
                        gravityValues[0] * gravityValues[0] +
                                gravityValues[1] * gravityValues[1] +
                                gravityValues[2] * gravityValues[2]);

                gravityValues[0] = (float) (gravityValues[0] / gravityMagnitude);
                gravityValues[1] = (float) (gravityValues[1] / gravityMagnitude);
                gravityValues[2] = (float) (gravityValues[2] / gravityMagnitude);

                inclinationAngle = (int) Math.round(Math.toDegrees(Math.acos(gravityValues[2])));
            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                proximitySensorValue = sensorEvent.values[0];
            }

            if (proximitySensorValue != -1 && inclinationAngle != -1 && gravityValues != null) {
                detect(proximitySensorValue, gravityValues, inclinationAngle);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private float round(float value) {
        return Math.round(value * 100.0) / 100.0f;
    }

    public void detect(Float prox, float[] g, Integer inc) {
        if (isAlwaysOnPocketMode() || !mSystemReady) return;
        boolean isProxInPocket = mProximitySensor != null && prox != -1f && prox < PROXIMITY_THRESHOLD;
        boolean isGravityInPocket = mAccelerometerSensor != null && g != null && g.length == 3 && g[1] < GRAVITY_THRESHOLD;
        boolean isInclinationInPocket= mAccelerometerSensor != null && inc != -1 && (inc > MIN_INCLINATION || inc < MAX_INCLINATION);

        mIsInPocket = isProxInPocket;

        if (!mIsInPocket) {
            mIsInPocket = isGravityInPocket && isInclinationInPocket;
        }

        if (mIsInPocket && !mIsOverlayUserUnlocked) {
            showOverlay();
        } else {
            hideOverlay();
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }
        void observe() {
            mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(ALWAYS_ON_POCKET_MODE_ENABLED), false, this,
                    UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(POCKET_MODE_ENABLED), false, this,
                    UserHandle.USER_ALL);
            updatePocketModeSettings();
        }
        void updatePocketModeSettings() {
             if (isPocketModeEnabled()) {
                registerListeners();
             } else {
                unregisterListeners();
             }
        }
        @Override
        public void onChange(boolean selfChange) {
            updatePocketModeSettings();
        }
    }

    private void createOverlayView(Context context) {
        mOverlayView = View.inflate(context, R.layout.pocket_mode_layout, null);
        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        mLayoutParams.gravity = Gravity.CENTER;
        mLayoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        mOverlayView.setLayoutParams(mLayoutParams);
        mOverlayView.setBackgroundColor(Color.argb(224, 0, 0, 0));
    }

    private void vibrate(VibrationEffect effect) {
        if (mVibrator != null) {
            mVibrator.vibrate(effect);
        }
    }

    private void initializeGestureDetector(Context context) {
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                disablePocketLock();
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mPowerManager != null) {
                    hideOverlay();
                    mPowerManager.goToSleep(SystemClock.uptimeMillis());
                }
                return true;
            }
        });
    }

    private boolean isDeviceOnKeyguard() {
        return mKeyguardManager != null && mKeyguardManager.isKeyguardLocked();
    }

    private boolean isCallInProgress() {
        if (mTelecomManager != null) {
            return mTelecomManager.isRinging() || mTelecomManager.isInCall();
        }
        return false;
    }
    
    private void startDisplayOffTimer() {
        mDisplayOffHandler.postDelayed(mDisplayOffRunnable, DISPLAY_OFF_DELAY);
    }

    private void cancelDisplayOffTimer() {
        mDisplayOffHandler.removeCallbacks(mDisplayOffRunnable);
    }
    
    public boolean isOverlayShowing() {
        return isPocketModeEnabled() && mShowing;
    }
    
    public boolean isDeviceInPocket() {
        return mIsInPocket && !mIsOverlayUserUnlocked;
    }
    
    public void setSystemReady() {
        this.mSystemReady = true;
    }
    

    private boolean isExcludedDevice() {
        return Build.MODEL.startsWith("Pixel 6");
    }
}
