/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2023 Paranoid Android
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

package com.android.server.biometrics.sensors.face.sense;

import android.annotation.NonNull;
import android.content.Context;
import android.hardware.face.Face;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.RemovalClient;

import java.util.Map;
import java.util.function.Supplier;

import vendor.aospa.biometrics.face.ISenseService;

class FaceRemovalClient extends RemovalClient<Face, ISenseService> {
    private static final String TAG = "FaceRemovalClient";

    private final int mBiometricId;

    FaceRemovalClient(@NonNull Context context, @NonNull Supplier<ISenseService> lazyDaemon,
            @NonNull IBinder token, @NonNull ClientMonitorCallbackConverter listener,
            int biometricId, int userId, @NonNull String owner, @NonNull BiometricUtils<Face> utils,
            int sensorId, @NonNull BiometricLogger logger,
            @NonNull BiometricContext biometricContext,
            @NonNull Map<Integer, Long> authenticatorIds, int reason) {
        super(context, lazyDaemon, token, listener, userId, owner, utils, sensorId, logger,
                biometricContext, authenticatorIds, reason);
        mBiometricId = biometricId;
    }

    @Override
    protected void startHalOperation() {
        try {
            getFreshDaemon().remove(mBiometricId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting remove", e);
            mCallback.onClientFinished(this, false /* success */);
        }
    }
}
