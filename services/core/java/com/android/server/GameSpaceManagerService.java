/*
 * Copyright (C) 2025 the AxionAOSP Project
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
package com.android.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.server.SystemService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GameSpaceManagerService extends SystemService {
    private static final String TAG = "GameSpaceManagerService";
    private static final String GAME_LIST_SETTING = "gamespace_game_list";
    private static final String NOTIFICATION_CHANNEL_ID = "gamespace_notif_channel";

    private final Context mContext;
    private final Handler mHandler = new Handler();
    private final PackageManager mPackageManager;

    public GameSpaceManagerService(Context context) {
        super(context);
        mContext = context;
        mPackageManager = context.getPackageManager();
    }

    @Override
    public void onStart() {
        createNotificationChannel();
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
            filter.addDataScheme("package");
            mContext.registerReceiver(new PackageChangeReceiver(), filter);
        }
    }

    private class PackageChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (packageName == null) return;

            if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
                handlePackageAdded(packageName);
            } else if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
                handlePackageRemoved(packageName);
            }
        }
    }

    private void handlePackageAdded(String packageName) {
        if (isGame(packageName)) {
            addToGameSpace(packageName);
        }
    }

    private void handlePackageRemoved(String packageName) {
        removeFromGameSpace(packageName);
    }

    private boolean isGame(String packageName) {
        try {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return (appInfo.category == ApplicationInfo.CATEGORY_GAME);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void addToGameSpace(String packageName) {
        ContentResolver cr = mContext.getContentResolver();
        String currentList = Settings.System.getString(cr, GAME_LIST_SETTING);
        Set<String> gameSet = new HashSet<>();

        if (currentList != null && !currentList.isEmpty()) {
            gameSet.addAll(Arrays.asList(currentList.split(",")));
        }

        String entry = packageName + "=2";
        if (!gameSet.contains(entry)) {
            gameSet.add(entry);
            Settings.System.putString(cr, GAME_LIST_SETTING, String.join(",", gameSet));

            sendGameAddedNotification(packageName);
        }
    }

    private void removeFromGameSpace(String packageName) {
        ContentResolver cr = mContext.getContentResolver();
        String currentList = Settings.System.getString(cr, GAME_LIST_SETTING);

        if (currentList == null || currentList.isEmpty()) return;

        Set<String> gameSet = new HashSet<>(Arrays.asList(currentList.split(",")));
        if (gameSet.removeIf(entry -> entry.startsWith(packageName + "="))) {
            Settings.System.putString(cr, GAME_LIST_SETTING, String.join(",", gameSet));
        }
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager = 
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "GameSpace",
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("GameSpace Notifications");
        channel.enableVibration(true);
        channel.enableLights(true);
        notificationManager.createNotificationChannel(channel);
    }

    private void sendGameAddedNotification(String packageName) {
        String appName;
        try {
            appName = mPackageManager.getApplicationLabel(
                    mPackageManager.getApplicationInfo(packageName, 0)
            ).toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = packageName;
        }

        NotificationManager notificationManager = 
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new Notification.Builder(mContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.mipmap.sym_def_app_icon)
            .setContentTitle("GameSpace")
            .setContentText(appName + " added to GameSpace")
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build();

        notificationManager.notify(packageName.hashCode(), notification);
    }

}
