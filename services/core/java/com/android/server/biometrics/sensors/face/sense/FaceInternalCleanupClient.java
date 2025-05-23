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
import android.hardware.biometrics.BiometricsProtoEnums;
import android.hardware.face.Face;
import android.os.IBinder;

import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.InternalCleanupClient;
import com.android.server.biometrics.sensors.InternalEnumerateClient;
import com.android.server.biometrics.sensors.RemovalClient;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import vendor.aospa.biometrics.face.ISenseService;

class FaceInternalCleanupClient extends InternalCleanupClient<Face, ISenseService> {

    FaceInternalCleanupClient(@NonNull Context context,
            @NonNull Supplier<ISenseService> lazyDaemon, int userId, @NonNull String owner,
            int sensorId, @NonNull BiometricLogger logger,
            @NonNull BiometricContext biometricContext,
            @NonNull BiometricUtils<Face> utils, @NonNull Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, userId, owner, sensorId, logger, biometricContext,
                utils, authenticatorIds);
    }

    @Override
    protected InternalEnumerateClient<ISenseService> getEnumerateClient(Context context,
            Supplier<ISenseService> lazyDaemon, IBinder token, int userId, String owner,
            List<Face> enrolledList, BiometricUtils<Face> utils, int sensorId,
            @NonNull BiometricLogger logger, @NonNull BiometricContext biometricContext) {
        return new FaceInternalEnumerateClient(context, lazyDaemon, token, userId, owner,
                enrolledList, utils, sensorId, logger, biometricContext);
    }

    @Override
    protected RemovalClient<Face, ISenseService> getRemovalClient(Context context,
            Supplier<ISenseService> lazyDaemon, IBinder token,
            int biometricId, int userId, String owner, BiometricUtils<Face> utils, int sensorId,
            @NonNull BiometricLogger logger, @NonNull BiometricContext biometricContext,
            Map<Integer, Long> authenticatorIds, int reason) {
        // Internal remove does not need to send results to anyone. Cleanup (enumerate + remove)
        // is all done internally.
        return new FaceRemovalClient(context, lazyDaemon, token,
                null /* ClientMonitorCallbackConverter */, biometricId, userId, owner, utils,
                sensorId, logger, biometricContext, authenticatorIds, reason);
    }

    @Override
    protected int getModality() {
        return BiometricsProtoEnums.MODALITY_FACE;
    }
}
