/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.hardware.fingerprint;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.common.CommonProps;
import android.hardware.biometrics.common.SensorStrength;
import android.hardware.biometrics.fingerprint.FingerprintSensorType;
import android.hardware.biometrics.fingerprint.SensorLocation;
import android.hardware.biometrics.fingerprint.SensorProps;
import android.util.Slog;

import com.android.internal.R;
import com.android.internal.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse HIDL fingerprint sensor config and map it to SensorProps.aidl to match AIDL.
 * See core/res/res/values/config.xml config_biometric_sensors
 * @hide
 */
public final class HidlFingerprintSensorConfig extends SensorProps {
    private static final String TAG = "HidlFingerprintSensorConfig";

    private int mSensorId;
    private int mModality;
    private int mStrength;

    /**
     * Parse through the config string and map it to SensorProps.aidl.
     * @throws IllegalArgumentException when config string has unexpected format
     */
    public void parse(@NonNull String config, @NonNull Context context)
            throws IllegalArgumentException {
        final String[] elems = config.split(":");
        if (elems.length < 3) {
            throw new IllegalArgumentException();
        }
        mSensorId = Integer.parseInt(elems[0]);
        mModality = Integer.parseInt(elems[1]);
        mStrength = Integer.parseInt(elems[2]);
        mapHidlToAidlSensorConfiguration(context);
    }

    @BiometricAuthenticator.Modality
    public int getModality() {
        return mModality;
    }

    private void mapHidlToAidlSensorConfiguration(@NonNull Context context) {
        commonProps = new CommonProps();
        commonProps.componentInfo = null;
        commonProps.sensorId = mSensorId;
        commonProps.sensorStrength = authenticatorStrengthToPropertyStrength(mStrength);
        commonProps.maxEnrollmentsPerUser = context.getResources().getInteger(
                R.integer.config_fingerprintMaxTemplatesPerUser);
        halControlsIllumination = false;
        halHandlesDisplayTouches = false;
        sensorLocations = new SensorLocation[1];

        // Non-empty workaroundLocations indicates that the sensor is SFPS.
        final List<SensorLocation> workaroundLocations =
                getWorkaroundSensorProps(context);

        final int[] udfpsProps = context.getResources().getIntArray(
                com.android.internal.R.array.config_udfps_sensor_props);
        final boolean isUdfps = !ArrayUtils.isEmpty(udfpsProps);
        // config_is_powerbutton_fps indicates whether device has a power button fingerprint sensor.
        final boolean isPowerbuttonFps = context.getResources().getBoolean(
                R.bool.config_is_powerbutton_fps);

        if (isUdfps) {
            sensorType = FingerprintSensorType.UNDER_DISPLAY_OPTICAL;
        } else if (isPowerbuttonFps) {
            sensorType = FingerprintSensorType.POWER_BUTTON;
        } else {
            sensorType = FingerprintSensorType.REAR;
        }

        if (isUdfps && udfpsProps.length == 3) {
            setSensorLocation(udfpsProps[0], udfpsProps[1], udfpsProps[2]);
        } else if (!workaroundLocations.isEmpty()) {
            sensorLocations = new SensorLocation[workaroundLocations.size()];
            workaroundLocations.toArray(sensorLocations);
        } else {
            setSensorLocation(540 /* sensorLocationX */, 1636 /* sensorLocationY */,
                    130 /* sensorRadius */);
        }

    }

    private void setSensorLocation(int sensorLocationX,
            int sensorLocationY, int sensorRadius) {
        sensorLocations[0] = new SensorLocation();
        sensorLocations[0].display = "";
        sensorLocations[0].sensorLocationX = sensorLocationX;
        sensorLocations[0].sensorLocationY = sensorLocationY;
        sensorLocations[0].sensorRadius = sensorRadius;
    }

    // TODO(b/174868353): workaround for gaps in HAL interface (remove and get directly from HAL)
    // reads values via an overlay instead of querying the HAL
    @NonNull
    public static List<SensorLocation> getWorkaroundSensorProps(@NonNull Context context) {
        final List<SensorLocation> sensorLocations = new ArrayList<>();

        final TypedArray sfpsProps = context.getResources().obtainTypedArray(
                com.android.internal.R.array.config_sfps_sensor_props);
        for (int i = 0; i < sfpsProps.length(); i++) {
            final int id = sfpsProps.getResourceId(i, -1);
            if (id > 0) {
                final SensorLocation location = parseSensorLocation(
                        context.getResources().obtainTypedArray(id));
                if (location != null) {
                    sensorLocations.add(location);
                }
            }
        }
        sfpsProps.recycle();

        return sensorLocations;
    }

    @Nullable
    private static SensorLocation parseSensorLocation(@Nullable TypedArray array) {
        if (array == null) {
            return null;
        }

        try {
            SensorLocation sensorLocation = new SensorLocation();
            sensorLocation.display = array.getString(0);
            sensorLocation.sensorLocationX = array.getInt(1, 0);
            sensorLocation.sensorLocationY = array.getInt(2, 0);
            sensorLocation.sensorRadius = array.getInt(3, 0);
            return sensorLocation;
        } catch (Exception e) {
            Slog.w(TAG, "malformed sensor location", e);
        }
        return null;
    }

    private byte authenticatorStrengthToPropertyStrength(
            @BiometricManager.Authenticators.Types int strength) {
        switch (strength) {
            case BiometricManager.Authenticators.BIOMETRIC_CONVENIENCE:
                return SensorStrength.CONVENIENCE;
            case BiometricManager.Authenticators.BIOMETRIC_WEAK:
                return SensorStrength.WEAK;
            case BiometricManager.Authenticators.BIOMETRIC_STRONG:
                return SensorStrength.STRONG;
            default:
                throw new IllegalArgumentException("Unknown strength: " + strength);
        }
    }
}
