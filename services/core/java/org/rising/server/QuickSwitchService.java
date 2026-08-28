/*
 * Copyright (C) 2023 The RisingOS Android Project
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoList;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;

import com.android.server.ServiceThread;
import com.android.server.SystemService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuickSwitchService extends SystemService {

    private static final String TAG = "QuickSwitchService";
    private static final String PROP_DEFAULT_LAUNCHER = "persist.sys.default_launcher";
    private static final String LAUNCHER3_PACKAGE = "com.android.launcher3";

    private static final String[] FALLBACK_LAUNCHER_PACKAGES = {
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "app.lawnchair"
    };

    private final Context mContext;
    private final IPackageManager mPM;
    private final IUserManager mUM;
    private final ContentResolver mResolver;
    private final String mOpPackageName;

    private ServiceThread mWorker;
    private Handler mHandler;

    private static final Object sLock = new Object();
    private static List<String> sLauncherPackages = Collections.emptyList();
    private static List<String> sDisabledLaunchersCache = null;
    private static int sLastDefaultLauncher = -1;

    public QuickSwitchService(Context context) {
        super(context);
        mContext = context;
        mResolver = context.getContentResolver();
        mPM = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mUM = IUserManager.Stub.asInterface(ServiceManager.getService(Context.USER_SERVICE));
        mOpPackageName = context.getOpPackageName();
        ensureLauncherPackages();
    }

    private static void ensureLauncherPackages() {
        synchronized (sLock) {
            if (sLauncherPackages != null && !sLauncherPackages.isEmpty()) {
                return;
            }
            List<String> packages = new ArrayList<>();
            try {
                String[] fromRes = Resources.getSystem().getStringArray(
                        com.android.internal.R.array.config_launcherPackages);
                if (fromRes != null) {
                    for (String packageName : fromRes) {
                        if (packageName != null && !packageName.isEmpty()) {
                            packages.add(packageName);
                        }
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to load config_launcherPackages", e);
            }
            if (packages.isEmpty()) {
                Collections.addAll(packages, FALLBACK_LAUNCHER_PACKAGES);
            }
            sLauncherPackages = packages;
            sDisabledLaunchersCache = null;
            Slog.i(TAG, "Launcher packages: " + sLauncherPackages);
        }
    }

    public static String getSelectedLauncherPackage() {
        ensureLauncherPackages();
        int defaultLauncher = getDefaultLauncherIndex();
        synchronized (sLock) {
            if (defaultLauncher >= 0 && defaultLauncher < sLauncherPackages.size()) {
                return sLauncherPackages.get(defaultLauncher);
            }
            return sLauncherPackages.isEmpty() ? LAUNCHER3_PACKAGE : sLauncherPackages.get(0);
        }
    }

    public static boolean shouldHide(int userId, String packageName) {
        return packageName != null && getDisabledDefaultLaunchers().contains(packageName);
    }

    public static PackageInfoList recreatePackageList(
            int callingUid, Context context, int userId, PackageInfoList list) {
        List<PackageInfo> appList = list.getList();
        List<String> disabledLaunchers = getDisabledDefaultLaunchers();
        appList.removeIf(info -> disabledLaunchers.contains(info.packageName));
        return new PackageInfoList(appList);
    }

    public static List<ApplicationInfo> recreateApplicationList(
            int callingUid, Context context, int userId, List<ApplicationInfo> list) {
        List<ApplicationInfo> appList = new ArrayList<>(list);
        List<String> disabledLaunchers = getDisabledDefaultLaunchers();
        appList.removeIf(info -> disabledLaunchers.contains(info.packageName));
        return appList;
    }

    public static List<String> getDisabledDefaultLaunchers() {
        ensureLauncherPackages();
        int defaultLauncher = getDefaultLauncherIndex();
        synchronized (sLock) {
            if (defaultLauncher != sLastDefaultLauncher || sDisabledLaunchersCache == null) {
                sLastDefaultLauncher = defaultLauncher;
                List<String> disabledDefaultLaunchers = new ArrayList<>();
                for (int i = 0; i < sLauncherPackages.size(); i++) {
                    if (i != defaultLauncher && !(i == 0 && defaultLauncher == 2)) {
                        disabledDefaultLaunchers.add(sLauncherPackages.get(i));
                    }
                }
                sDisabledLaunchersCache = disabledDefaultLaunchers;
            }
            return sDisabledLaunchersCache;
        }
    }

    private static int getDefaultLauncherIndex() {
        ensureLauncherPackages();
        int defaultLauncher = SystemProperties.getInt(PROP_DEFAULT_LAUNCHER, 0);
        synchronized (sLock) {
            if (defaultLauncher < 0 || defaultLauncher >= sLauncherPackages.size()) {
                return 0;
            }
            return defaultLauncher;
        }
    }

    private boolean shouldEnablePackage(String packageName, int defaultLauncher) {
        synchronized (sLock) {
            String selected = sLauncherPackages.get(defaultLauncher);
            if (packageName.equals(selected)) {
                return true;
            }
            return LAUNCHER3_PACKAGE.equals(packageName) && defaultLauncher == 2;
        }
    }

    private void updateStateForUser(int userId) {
        ensureLauncherPackages();
        int defaultLauncher = getDefaultLauncherIndex();
        String selected;
        List<String> packages;
        synchronized (sLock) {
            selected = sLauncherPackages.get(defaultLauncher);
            packages = new ArrayList<>(sLauncherPackages);
        }
        Slog.i(TAG, "Applying launcher " + selected + " (index=" + defaultLauncher
                + ") for user " + userId);

        boolean selectedEnabled = false;
        for (String packageName : packages) {
            if (!shouldEnablePackage(packageName, defaultLauncher)) {
                continue;
            }
            if (setLauncherEnabled(packageName, true, userId)) {
                if (packageName.equals(selected)) {
                    selectedEnabled = true;
                }
            }
        }

        if (!selectedEnabled) {
            Slog.w(TAG, "Selected launcher " + selected
                    + " is not installed; leaving other launchers enabled");
            return;
        }

        for (String packageName : packages) {
            if (shouldEnablePackage(packageName, defaultLauncher)) {
                continue;
            }
            setLauncherEnabled(packageName, false, userId);
        }
    }

    private boolean setLauncherEnabled(String packageName, boolean enable, int userId) {
        try {
            mPM.setApplicationEnabledSetting(packageName,
                    enable ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    0, userId, mOpPackageName);
            Slog.i(TAG, (enable ? "Enabled " : "Disabled ") + packageName + " for user " + userId);
            return true;
        } catch (IllegalArgumentException e) {
            Slog.w(TAG, "Launcher not installed: " + packageName);
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to update " + packageName, e);
            return false;
        }
    }

    private void initForUser(int userId) {
        if (userId < 0) {
            return;
        }
        updateStateForUser(userId);
    }

    private void init() {
        try {
            for (UserInfo user : mUM.getUsers(false)) {
                initForUser(user.id);
            }
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_REMOVED);
        mContext.registerReceiver(new UserReceiver(), filter,
                android.Manifest.permission.MANAGE_USERS, mHandler);
    }

    @Override
    public void onStart() {
        mWorker = new ServiceThread(TAG, android.os.Process.THREAD_PRIORITY_DEFAULT, false);
        mWorker.start();
        mHandler = new Handler(mWorker.getLooper());
        init();
    }

    private final class UserReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
            if (Intent.ACTION_USER_ADDED.equals(intent.getAction())) {
                initForUser(userId);
            }
        }
    }
}
