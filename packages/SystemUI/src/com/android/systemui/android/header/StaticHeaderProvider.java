/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.systemui.android.header;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;

import com.android.systemui.res.R;
import com.android.internal.util.android.Utils;

public class StaticHeaderProvider implements
        StatusBarHeaderMachine.IStatusBarHeaderProvider {

    public static final String TAG = "StaticHeaderProvider";
    private static final boolean DEBUG = false;

    private Context mContext;
    private Resources mRes;
    private String mImage;
    private String mPackageName;

    public StaticHeaderProvider(Context context) {
        mContext = context;
    }

    @Override
    public String getName() {
        return "static";
    }

    @Override
    public void settingsChanged(Uri uri) {
        final boolean customHeader = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0,
                UserHandle.USER_CURRENT) == 1;
        String imageUrl = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_IMAGE,
                UserHandle.USER_CURRENT);

        if (imageUrl != null && customHeader) {
            int idx = imageUrl.indexOf("/");
            if (idx != -1) {
                String[] parts = imageUrl.split("/");
                mPackageName = parts[0];
                mImage = parts[1];
                loadHeaderImage();
            }
        }
    }

    @Override
    public void enableProvider() {
        settingsChanged(null);
    }

    @Override
    public void disableProvider() {
    }

    private void loadHeaderImage() {
        if (DEBUG) Log.i(TAG, "Load header image " + " " + mPackageName + " " + mImage);

        try {
            PackageManager packageManager = mContext.getPackageManager();
            mRes = packageManager.getResourcesForApplication(mPackageName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load icon pack " + mPackageName, e);
            mRes = null;
        }
    }

    @Override
    public Drawable getCurrent(final Calendar now) {
        if (mRes == null) {
            return null;
        }
        if (!Utils.isPackageInstalled(mContext, mPackageName)) {
            Log.w(TAG, "Header pack image " + mImage + " no longer available");
            return null;
        }
        try {
            return mRes.getDrawable(mRes.getIdentifier(mImage, "drawable", mPackageName), null);
        } catch(Resources.NotFoundException e) {
            Log.w(TAG, "No drawable found for " + now +" in " + mPackageName);
        }
        return null;
    }
}
