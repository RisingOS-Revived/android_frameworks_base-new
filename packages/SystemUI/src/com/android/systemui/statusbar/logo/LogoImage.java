/*
 * Copyright (C) 2018-2024 crDroid Android Project
 * Copyright (C) 2025 Rising Revived
 * Copyright (C) 2018-2019 AICP
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

package com.android.systemui.statusbar.logo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.res.R;

import java.util.ArrayList;

public abstract class LogoImage extends ImageView implements DarkReceiver {

    private Context mContext;

    private boolean mAttached;

    private boolean mShowLogo;
    public int mLogoPosition;
    private int mLogoStyle;
    private int mTintColor = Color.WHITE;
    private boolean mUseCustomLogoColor;
    private int mCustomLogoColor = Color.WHITE;

    class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_LOGO), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_LOGO_POSITION),
                    false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_LOGO_STYLE),
                    false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_LOGO_USE_CUSTOM_COLOR),
                    false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_LOGO_CUSTOM_COLOR),
                    false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public LogoImage(Context context) {
        this(context, null);
    }

    public LogoImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogoImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    protected abstract boolean isLogoVisible();

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAttached)
            return;

        mAttached = true;

        SettingsObserver observer = new SettingsObserver(null);
        observer.observe();
        updateSettings();

        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!mAttached)
            return;

        mAttached = false;
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        if (!mUseCustomLogoColor) {
            mTintColor = DarkIconDispatcher.getTint(areas, this, tint);
            if (mShowLogo && isLogoVisible()) {
                updateLogo();
            }
        }
    }

    public void updateLogo() {
        Drawable drawable = null;
        switch (mLogoStyle) {
            case 0:
            default:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_rising_logo);
                break;
            case 1:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_android_logo);
                break;
            case 2:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_adidas);
                break;
            case 3:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_alien);
                break;
            case 4:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_apple_logo);
                break;
            case 5:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_avengers);
                break;
            case 6:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_batman);
                break;
            case 7:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_batman_tdk);
                break;
            case 8:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_beats);
                break;
            case 9:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_biohazard);
                break;
            case 10:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_blackberry);
                break;
            case 11:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_cannabis);
                break;
            case 12:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_cool);
                break;
            case 13:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_devil);
                break;
            case 14:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_fire);
                break;
            case 15:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_heart);
                break;
            case 16:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_nike);
                break;
            case 17:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_pac_man);
                break;
            case 18:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_puma);
                break;
            case 19:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_rog);
                break;
            case 20:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_spiderman);
                break;
            case 21:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_superman);
                break;
            case 22:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_windows);
                break;
            case 23:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_xbox);
                break;
            case 24:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ghost);
                break;
            case 25:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ninja);
                break;
            case 26:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_robot);
                break;
            case 27:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ironman);
                break;
            case 28:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_captain_america);
                break;
            case 29:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_flash);
                break;
            case 30:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_tux_logo);
                break;
            case 31:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ubuntu_logo);
                break;
            case 32:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_mint_logo);
                break;
            case 33:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_amogus);
                break;
        }

        // Use either the custom color or the system tint color
        int logoColor = mUseCustomLogoColor ? mCustomLogoColor : mTintColor;
        drawable.setTint(logoColor);
        setImageDrawable(drawable);
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        
        // Get existing settings
        mShowLogo = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_LOGO, 0, UserHandle.USER_CURRENT) != 0;
        mLogoPosition = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_LOGO_POSITION, 0, UserHandle.USER_CURRENT);
        mLogoStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_LOGO_STYLE, 0, UserHandle.USER_CURRENT);
        
        // Get color settings
        mUseCustomLogoColor = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_LOGO_USE_CUSTOM_COLOR, 0, UserHandle.USER_CURRENT) != 0;
        
        if (mUseCustomLogoColor) {
            mCustomLogoColor = Settings.System.getIntForUser(resolver,
                    Settings.System.STATUS_BAR_LOGO_CUSTOM_COLOR, Color.WHITE, UserHandle.USER_CURRENT);
        }

        if (!mShowLogo || !isLogoVisible()) {
            setImageDrawable(null);
            setVisibility(View.GONE);
            return;
        }
        updateLogo();
        setVisibility(View.VISIBLE);
    }
}
