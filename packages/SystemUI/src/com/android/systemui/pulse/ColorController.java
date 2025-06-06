/**
 * Copyright (C) 2020-2025 crDroid Android Project
 *
 * @author: Randall Rushing <randall.rushing@gmail.com>
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
 *
 * Generalized renderer color state management and color event dispatch
 */

package com.android.systemui.pulse;

import com.android.systemui.statusbar.policy.ConfigurationController;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.TypedValue;

import com.android.internal.util.ContrastColorUtil;
import com.android.settingslib.Utils;

public class ColorController extends ContentObserver
        implements ColorAnimator.ColorAnimationListener,
        ConfigurationController.ConfigurationListener {
    public static final int COLOR_TYPE_ACCENT = 0;
    public static final int COLOR_TYPE_USER = 1;
    public static final int COLOR_TYPE_LAVALAMP = 2;
    public static final int COLOR_TYPE_AUTO = 3;
    public static final int LAVA_LAMP_SPEED_DEF = 10000;

    private Context mContext;
    private Renderer mRenderer;
    private ColorAnimator mLavaLamp;
    private int mColorType;
    private int mAccentColor;
    private int mColor;
    private int mAlbumColor;

    public ColorController(
            Context context,
            Handler handler,
            ConfigurationController configurationController) {
        super(handler);
        mContext = context;
        mLavaLamp = new ColorAnimator();
        mLavaLamp.setColorAnimatorListener(this);
        mAccentColor = getAccentColor();
        mAlbumColor = mAccentColor;
        updateSettings();
        startListening();
        configurationController.addCallback(this);
    }

    void setRenderer(Renderer renderer) {
        mRenderer = renderer;
        notifyRenderer();
    }

    void startListening() {
        ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.PULSE_COLOR_MODE), false,
                this,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.PULSE_COLOR_USER), false, this,
                UserHandle.USER_ALL);
        resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.PULSE_LAVALAMP_SPEED), false, this,
                UserHandle.USER_ALL);
    }

    void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        if (mColorType == COLOR_TYPE_LAVALAMP) {
            stopLavaLamp();
        }
        mColorType = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_COLOR_MODE, COLOR_TYPE_LAVALAMP, UserHandle.USER_CURRENT);
        mColor = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_COLOR_USER,
                0x92FFFFFF,
                UserHandle.USER_CURRENT);
        int lava_speed = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_LAVALAMP_SPEED, LAVA_LAMP_SPEED_DEF,
                UserHandle.USER_CURRENT);
        mLavaLamp.setAnimationTime(lava_speed);
        notifyRenderer();
    }

    void notifyRenderer() {
        if (mRenderer != null) {
            if (mColorType == COLOR_TYPE_ACCENT) {
                mRenderer.onUpdateColor(mAccentColor);
            } else if (mColorType == COLOR_TYPE_USER) {
                mRenderer.onUpdateColor(mColor);
            } else if (mColorType == COLOR_TYPE_LAVALAMP && mRenderer.isValidStream()) {
                startLavaLamp();
            } else if (mColorType == COLOR_TYPE_AUTO) {
                mRenderer.onUpdateColor(mAlbumColor);
            }
        }
    }

    void startLavaLamp() {
        if (mColorType == COLOR_TYPE_LAVALAMP) {
            mLavaLamp.start();
        }
    }

    void stopLavaLamp() {
        mLavaLamp.stop();
    }

    int getAccentColor() {
        return Utils.getColorAccentDefaultColor(mContext);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        updateSettings();
    }

    @Override
    public void onConfigChanged(Configuration newConfig) {
        final int lastAccent = mAccentColor;
        final int currentAccent = getAccentColor();
        if (lastAccent != currentAccent) {
            mAccentColor = currentAccent;
            if (mRenderer != null && mColorType == COLOR_TYPE_ACCENT) {
                mRenderer.onUpdateColor(mAccentColor);
            }
        }
    }

    @Override
    public void onColorChanged(ColorAnimator colorAnimator, int color) {
        if (mRenderer != null) {
            mRenderer.onUpdateColor(color);
        }
    }

    public void setMediaNotificationColor(int color) {
        if (color != 0) {
            // be sure the color has an acceptable contrast against dark background
            mAlbumColor = ContrastColorUtil.findContrastColorAgainstDark(color, 0x000000, true, 2);
            // now be sure the color also has an acceptable contrast against light background
            mAlbumColor = ContrastColorUtil.findContrastColor(mAlbumColor, 0xffffff, true, 2);
        } else {
            // fallback to accent color if the media notification isn't colorized
            mAlbumColor = mAccentColor;
        }
        if (mRenderer != null && mColorType == COLOR_TYPE_AUTO) {
            mRenderer.onUpdateColor(mAlbumColor);
        }
    }
}
