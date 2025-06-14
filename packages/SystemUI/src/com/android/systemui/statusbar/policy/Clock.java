/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.annotation.NonNull;
import android.app.ActivityManager;
import android.app.StatusBarManager;
import android.app.WindowConfiguration;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.icu.lang.UCharacter;
import android.icu.text.DateTimePatternGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.demomode.DemoModeCommandReceiver;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.res.R;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.shared.system.TaskStackChangeListeners;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import lineageos.providers.LineageSettings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Digital clock for the status bar.
 */
public class Clock extends TextView implements
        DemoModeCommandReceiver,
        Tunable,
        CommandQueue.Callbacks,
        DarkReceiver, ConfigurationListener {

    private static final String CLOCK_SUPER_PARCELABLE = "clock_super_parcelable";
    private static final String CURRENT_USER_ID = "current_user_id";
    private static final String VISIBLE_BY_POLICY = "visible_by_policy";
    private static final String VISIBLE_BY_USER = "visible_by_user";
    private static final String SHOW_SECONDS = "show_seconds";
    private static final String VISIBILITY = "visibility";
    private static final String QSHEADER = "qsheader";

    public static final String STATUS_BAR_CLOCK_SECONDS =
            "system:" + Settings.System.STATUS_BAR_CLOCK_SECONDS;
    private static final String STATUS_BAR_AM_PM =
            "lineagesystem:" + LineageSettings.System.STATUS_BAR_AM_PM;
    private static final String STATUS_BAR_CLOCK_AUTO_HIDE_LAUNCHER =
            "lineagesystem:" + LineageSettings.System.STATUS_BAR_CLOCK_AUTO_HIDE;
    public static final String STATUS_BAR_CLOCK_DATE_DISPLAY =
            "system:" + Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY;
    public static final String STATUS_BAR_CLOCK_DATE_STYLE =
            "system:" + Settings.System.STATUS_BAR_CLOCK_DATE_STYLE;
    public static final String STATUS_BAR_CLOCK_DATE_POSITION =
            "system:" + Settings.System.STATUS_BAR_CLOCK_DATE_POSITION;
    public static final String STATUS_BAR_CLOCK_DATE_FORMAT =
            "system:" + Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT;
    public static final String STATUS_BAR_CLOCK_AUTO_HIDE =
            "system:" + Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE;
    public static final String STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION =
            "system:" + Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION;
    public static final String STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION =
            "system:" + Settings.System.STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION;
    private static final String STATUSBAR_CLOCK_CHIP =
            "system:" + Settings.System.STATUSBAR_CLOCK_CHIP;
    public static final String STATUS_BAR_CLOCK_SIZE =
            "system:" + Settings.System.STATUS_BAR_CLOCK_SIZE;
    public static final String QS_HEADER_CLOCK_SIZE =
            "system:" + Settings.System.QS_HEADER_CLOCK_SIZE;

    private int mClockSize;
    private int mClockSizeQsHeader;

    private final UserTracker mUserTracker;
    private final CommandQueue mCommandQueue;
    private int mCurrentUserId;

    private boolean mClockAutoHideLauncher = false;
    private boolean mClockVisibleByPolicy = true;
    private boolean mClockVisibleByUser = getVisibility() == View.VISIBLE;
    private boolean mClockBgOn;
    private boolean mClockBgStyleIsTwo;

    private boolean mAttached;
    private boolean mScreenReceiverRegistered;
    private boolean mTaskStackListenerRegistered;
    private Calendar mCalendar;
    private String mContentDescriptionFormatString;
    private SimpleDateFormat mClockFormat;
    private SimpleDateFormat mContentDescriptionFormat;
    private Locale mLocale;
    private DateTimePatternGenerator mDateTimePatternGenerator;
    private boolean mScreenOn = true;
    private Handler autoHideHandler = new Handler();

    private static final int AM_PM_STYLE_NORMAL  = 0;
    private static final int AM_PM_STYLE_SMALL   = 1;
    private static final int AM_PM_STYLE_GONE    = 2;

    private static final int CLOCK_DATE_DISPLAY_GONE = 0;
    private static final int CLOCK_DATE_DISPLAY_SMALL = 1;
    private static final int CLOCK_DATE_DISPLAY_NORMAL = 2;

    private static final int CLOCK_DATE_STYLE_REGULAR = 0;
    private static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    private static final int CLOCK_DATE_STYLE_UPPERCASE = 2;

    private static final int STYLE_DATE_LEFT = 0;
    private static final int STYLE_DATE_RIGHT = 1;
    private static final int HIDE_DURATION = 60; // 1 minute
    private static final int SHOW_DURATION = 5; // 5 seconds

    private int mAmPmStyle = AM_PM_STYLE_GONE;
    private final boolean mShowDark;
    private boolean mShowSeconds;
    private Handler mSecondsHandler;
    private int mClockDateDisplay = CLOCK_DATE_DISPLAY_GONE;
    private int mClockDateStyle = CLOCK_DATE_STYLE_REGULAR;
    private int mClockDatePosition;
    private String mClockDateFormat = null;
    private boolean mClockAutoHide;
    private int mHideDuration = HIDE_DURATION, mShowDuration = SHOW_DURATION;
    private boolean mQsHeader;

    private boolean mIsStatusBar;

    // Fields to cache the width so the clock remains at an approximately constant width
    private int mCharsAtCurrentWidth = -1;
    private int mCachedWidth = -1;

    /**
     * Color to be set on this {@link TextView}, when wallpaperTextColor is <b>not</b> utilized.
     */
    private int mNonAdaptedColor;

    private final BroadcastDispatcher mBroadcastDispatcher;

    private final UserTracker.Callback mUserChangedCallback =
            new UserTracker.Callback() {
                @Override
                public void onUserChanged(int newUser, @NonNull Context userContext) {
                    mCurrentUserId = newUser;
                    updateClock();
                }
            };

    public Clock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Clock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mCommandQueue = Dependency.get(CommandQueue.class);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.Clock,
                0, 0);
        try {
            mAmPmStyle = a.getInt(R.styleable.Clock_amPmStyle, mAmPmStyle);
            mShowDark = a.getBoolean(R.styleable.Clock_showDark, true);
            mIsStatusBar = a.getBoolean(R.styleable.Clock_isStatusBar, mIsStatusBar);
            mNonAdaptedColor = getCurrentTextColor();
        } finally {
            a.recycle();
        }
        mBroadcastDispatcher = Dependency.get(BroadcastDispatcher.class);
        mUserTracker = Dependency.get(UserTracker.class);

        setIncludeFontPadding(false);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CLOCK_SUPER_PARCELABLE, super.onSaveInstanceState());
        bundle.putInt(CURRENT_USER_ID, mCurrentUserId);
        bundle.putBoolean(VISIBLE_BY_POLICY, mClockVisibleByPolicy);
        bundle.putBoolean(VISIBLE_BY_USER, mClockVisibleByUser);
        bundle.putBoolean(SHOW_SECONDS, mShowSeconds);
        bundle.putInt(VISIBILITY, getVisibility());
        bundle.putBoolean(QSHEADER, mQsHeader);

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof Bundle)) {
            super.onRestoreInstanceState(state);
            return;
        }

        Bundle bundle = (Bundle) state;
        Parcelable superState = bundle.getParcelable(CLOCK_SUPER_PARCELABLE);
        super.onRestoreInstanceState(superState);
        if (bundle.containsKey(CURRENT_USER_ID)) {
            mCurrentUserId = bundle.getInt(CURRENT_USER_ID);
        }
        mClockVisibleByPolicy = bundle.getBoolean(VISIBLE_BY_POLICY, true);
        mClockVisibleByUser = bundle.getBoolean(VISIBLE_BY_USER, true);
        mShowSeconds = bundle.getBoolean(SHOW_SECONDS, false);
        if (bundle.containsKey(VISIBILITY)) {
            super.setVisibility(bundle.getInt(VISIBILITY));
        }
        mQsHeader = bundle.getBoolean(QSHEADER, false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);

            // NOTE: This receiver could run before this method returns, as it's not dispatching
            // on the main thread and BroadcastDispatcher may not need to register with Context.
            // The receiver will return immediately if the view does not have a Handler yet.
            mBroadcastDispatcher.registerReceiverWithHandler(mIntentReceiver, filter,
                    Dependency.get(Dependency.TIME_TICK_HANDLER), UserHandle.ALL);
            Dependency.get(TunerService.class).addTunable(this,
                    STATUS_BAR_CLOCK_SECONDS,
                    STATUS_BAR_AM_PM,
                    STATUS_BAR_CLOCK_AUTO_HIDE_LAUNCHER,
                    STATUS_BAR_CLOCK_DATE_DISPLAY,
                    STATUS_BAR_CLOCK_DATE_STYLE,
                    STATUS_BAR_CLOCK_DATE_POSITION,
                    STATUS_BAR_CLOCK_DATE_FORMAT,
                    STATUS_BAR_CLOCK_AUTO_HIDE,
                    STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION,
                    STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION,
                    STATUSBAR_CLOCK_CHIP,
                    STATUS_BAR_CLOCK_SIZE,
                    QS_HEADER_CLOCK_SIZE);
            mCommandQueue.addCallback(this);
            if (mShowDark) {
                Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
            }
            mUserTracker.addCallback(mUserChangedCallback, mContext.getMainExecutor());
            mCurrentUserId = mUserTracker.getUserId();
        }

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = Calendar.getInstance(TimeZone.getDefault());
        mContentDescriptionFormatString = "";
        mDateTimePatternGenerator = null;

        // Make sure we update to the current time
        updateShowSeconds();
        mContext.getMainExecutor().execute(() -> {
            updateClock();
            updateClockSize();
            updateClockVisibility();
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mScreenReceiverRegistered) {
            mScreenReceiverRegistered = false;
            mBroadcastDispatcher.unregisterReceiver(mScreenReceiver);
            if (mSecondsHandler != null) {
                mSecondsHandler.removeCallbacks(mSecondTick);
                mSecondsHandler = null;
            }
        }
        if (mAttached) {
            mBroadcastDispatcher.unregisterReceiver(mIntentReceiver);
            mAttached = false;
            Dependency.get(TunerService.class).removeTunable(this);
            mCommandQueue.removeCallback(this);
            if (mShowDark) {
                Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
            }
            mUserTracker.removeCallback(mUserChangedCallback);
            handleTaskStackListener(false);
        }
    }

    private void handleTaskStackListener(boolean register) {
        if (!mIsStatusBar) {
            // We don't support clock auto hide for quick settings.
            return;
        }
        if (register && !mTaskStackListenerRegistered) {
            TaskStackChangeListeners.getInstance().registerTaskStackListener(mTaskStackListener);
            mTaskStackListenerRegistered = true;
        } else if (!register && mTaskStackListenerRegistered) {
            TaskStackChangeListeners.getInstance().unregisterTaskStackListener(mTaskStackListener);
            mTaskStackListenerRegistered = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // If the handler is null, it means we received a broadcast while the view has not
            // finished being attached or in the process of being detached.
            // In that case, do not post anything.
            Handler handler = getHandler();
            if (handler == null) return;

            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra(Intent.EXTRA_TIMEZONE);
                handler.post(() -> {
                    mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
                    if (mClockFormat != null) {
                        mClockFormat.setTimeZone(mCalendar.getTimeZone());
                    }
                });
            } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                final Locale newLocale = getResources().getConfiguration().locale;
                handler.post(() -> {
                    if (!newLocale.equals(mLocale)) {
                        mLocale = newLocale;
                    }
                    // Force refresh of dependent variables.
                    mContentDescriptionFormatString = "";
                    mDateTimePatternGenerator = null;
                    updateClockVisibility();
                    updateShowSeconds();
                    updateClock(true);
                    return;
                });
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mScreenOn = true;
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenOn = false;
            }
            if (mScreenOn) {
                handler.post(() -> updateClock());
                if (mClockAutoHide) autoHideHandler.post(() -> updateClockVisibility());
            }
        }
    };

    @Override
    public void setVisibility(int visibility) {
        if (visibility == View.VISIBLE && !shouldBeVisible()) {
            return;
        }

        super.setVisibility(visibility);
    }

    public void setQsHeader() {
        mQsHeader = true;
    }

    public void setClockVisibleByUser(boolean visible) {
        mClockVisibleByUser = visible;
        updateClockVisibility();
    }

    public void setClockVisibilityByPolicy(boolean visible) {
        mClockVisibleByPolicy = visible;
        updateClockVisibility();
    }

    public boolean shouldBeVisible() {
        return !mClockAutoHideLauncher && mClockVisibleByPolicy && mClockVisibleByUser;
    }

    private void updateClockVisibility() {
        boolean visible = shouldBeVisible();
        int visibility = visible ? View.VISIBLE : View.GONE;
        try {
            autoHideHandler.removeCallbacksAndMessages(null);
        } catch (NullPointerException e) {
            // Do nothing
        }
        setVisibility(visibility);
        if (!mQsHeader && mClockAutoHide && visible && mScreenOn) {
            autoHideHandler.postDelayed(()->autoHideClock(), mShowDuration * 1000);
        }
    }

    private void autoHideClock() {
        setVisibility(View.GONE);
        autoHideHandler.postDelayed(()->updateClockVisibility(), mHideDuration * 1000);
    }

    final void updateClock(boolean forceTextUpdate) {
        if (mDemoMode || mCalendar == null) return;
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        CharSequence smallTime = getSmallTime();
        // Setting text actually triggers a layout pass (because the text view is set to
        // wrap_content width and TextView always relayouts for this). Avoid needless
        // relayout if the text didn't actually change.
        if (forceTextUpdate || !TextUtils.equals(smallTime, getText())) {
            setText(smallTime);
        }
        setContentDescription(mContentDescriptionFormat.format(mCalendar.getTime()));
    }

    final void updateClock() {
        updateClock(false);
    }

    /**
     * In order to avoid the clock growing and shrinking due to proportional fonts, we want to
     * cache the drawn width at a given number of characters (removing the cache when it changes),
     * and only use the biggest value. This means that the clock width with grow to the maximum
     * size over time, but reset whenever the number of characters changes (or the configuration
     * changes)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int chars = getText().length();
        if (chars != mCharsAtCurrentWidth) {
            mCharsAtCurrentWidth = chars;
            mCachedWidth = getMeasuredWidth();
            return;
        }

        int measuredWidth = getMeasuredWidth();
        if (mCachedWidth > measuredWidth) {
            setMeasuredDimension(mCachedWidth, getMeasuredHeight());
        } else {
            mCachedWidth = measuredWidth;
        }
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case STATUS_BAR_CLOCK_SECONDS:
                mShowSeconds =
                        TunerService.parseIntegerSwitch(newValue, false);
                updateShowSeconds();
                break;
            case STATUS_BAR_AM_PM:
                mAmPmStyle =
                        TunerService.parseInteger(newValue, AM_PM_STYLE_GONE);
                break;
            case STATUS_BAR_CLOCK_AUTO_HIDE_LAUNCHER:
                handleTaskStackListener(TunerService.parseIntegerSwitch(newValue, false));
                break;
            case STATUS_BAR_CLOCK_DATE_DISPLAY:
                mClockDateDisplay =
                        TunerService.parseInteger(newValue, CLOCK_DATE_DISPLAY_GONE);
                break;
            case STATUS_BAR_CLOCK_DATE_STYLE:
                mClockDateStyle =
                        TunerService.parseInteger(newValue, CLOCK_DATE_STYLE_REGULAR);
                break;
            case STATUS_BAR_CLOCK_DATE_POSITION:
                mClockDatePosition =
                        TunerService.parseInteger(newValue, STYLE_DATE_LEFT);
                break;
            case STATUS_BAR_CLOCK_DATE_FORMAT:
                mClockDateFormat = newValue;
                break;
            case STATUS_BAR_CLOCK_AUTO_HIDE:
                mClockAutoHide =
                        TunerService.parseIntegerSwitch(newValue, false);
                break;
            case STATUS_BAR_CLOCK_AUTO_HIDE_HDURATION:
                mHideDuration =
                        TunerService.parseInteger(newValue, HIDE_DURATION);
                break;
            case STATUS_BAR_CLOCK_AUTO_HIDE_SDURATION:
                mShowDuration =
                        TunerService.parseInteger(newValue, SHOW_DURATION);
                break;
            case STATUSBAR_CLOCK_CHIP:
                int sbClockBgStyle = TunerService.parseInteger(newValue, 0);
                mClockBgOn = sbClockBgStyle != 0;
                mClockBgStyleIsTwo = sbClockBgStyle == 2;
                break;
            case STATUS_BAR_CLOCK_SIZE:
                mClockSize =
                        TunerService.parseInteger(newValue, 14);
                updateClockSize();
                break;
            case QS_HEADER_CLOCK_SIZE:
                mClockSizeQsHeader =
                        TunerService.parseInteger(newValue, 14);
                updateClockSize();
                break;
            default:
                break;
        }
        // Force refresh of dependent variables.
        mContentDescriptionFormatString = "";
        mDateTimePatternGenerator = null;
        mContext.getMainExecutor().execute(() -> {
            updateClock(true);
            updateClockVisibility();
        });
    }

    @Override
    public void disable(int displayId, int state1, int state2, boolean animate) {
        if (displayId != getDisplay().getDisplayId()) {
            return;
        }
        boolean clockVisibleByPolicy = (state1 & StatusBarManager.DISABLE_CLOCK) == 0;
        if (clockVisibleByPolicy != mClockVisibleByPolicy) {
            setClockVisibilityByPolicy(clockVisibleByPolicy);
        }
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        mNonAdaptedColor = DarkIconDispatcher.getTint(areas, this, tint);
        setTextColor(mClockBgOn && !mClockBgStyleIsTwo ? Color.WHITE : mNonAdaptedColor);
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        reloadDimens();
    }

    private void reloadDimens() {
        // reset mCachedWidth so the new width would be updated properly when next onMeasure
        mCachedWidth = -1;

        FontSizeUtils.updateFontSize(this, R.dimen.status_bar_clock_size);

        float fontHeight = getPaint().getFontMetricsInt(null);
        setLineHeight(TypedValue.COMPLEX_UNIT_PX, fontHeight);

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) {
            lp.height = (int) Math.ceil(fontHeight);
            setLayoutParams(lp);
        }
    }

    private void updateShowSeconds() {
        if (mShowSeconds) {
            // Wait until we have a display to start trying to show seconds.
            if (mSecondsHandler == null && getDisplay() != null) {
                mSecondsHandler = new Handler();
                if (getDisplay().getState() == Display.STATE_ON) {
                    mSecondsHandler.postAtTime(mSecondTick,
                            SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
                }
                mScreenReceiverRegistered = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_SCREEN_ON);
                mBroadcastDispatcher.registerReceiver(mScreenReceiver, filter);
            }
        } else {
            if (mSecondsHandler != null) {
                mScreenReceiverRegistered = false;
                mBroadcastDispatcher.unregisterReceiver(mScreenReceiver);
                mSecondsHandler.removeCallbacks(mSecondTick);
                mSecondsHandler = null;
            }
        }
    }

    private void updateShowClock() {
        ActivityManager.RunningTaskInfo runningTask =
                ActivityManagerWrapper.getInstance().getRunningTask();
        final int activityType = runningTask != null
                ? runningTask.configuration.windowConfiguration.getActivityType()
                : WindowConfiguration.ACTIVITY_TYPE_UNDEFINED;
        final boolean clockAutoHide = activityType == WindowConfiguration.ACTIVITY_TYPE_HOME;
        if (mClockAutoHideLauncher != clockAutoHide) {
            mClockAutoHideLauncher = clockAutoHide;
            updateClockVisibility();
        }
    }

    private final CharSequence getSmallTime() {
        Context context = getContext();
        boolean is24 = DateFormat.is24HourFormat(context, mCurrentUserId);
        if (mDateTimePatternGenerator == null) {
            // Despite its name, getInstance creates a cloned instance, so reuse the generator to
            // avoid unnecessary churn.
            mDateTimePatternGenerator = DateTimePatternGenerator.getInstance(
                context.getResources().getConfiguration().locale);
        }

        final char MAGIC1 = '\uEF00';
        final char MAGIC2 = '\uEF01';

        final String formatSkeleton = mShowSeconds
                ? is24 ? "Hms" : "hms"
                : is24 ? "Hm" : "hm";
        String format = mDateTimePatternGenerator.getBestPattern(formatSkeleton);
        if (!format.equals(mContentDescriptionFormatString)) {
            mContentDescriptionFormatString = format;
            mContentDescriptionFormat = new SimpleDateFormat(format);
            /*
             * Search for an unquoted "a" in the format string, so we can
             * add marker characters around it to let us find it again after
             * formatting and change its size.
             */
            if (mAmPmStyle != AM_PM_STYLE_NORMAL) {
                int a = -1;
                boolean quoted = false;
                for (int i = 0; i < format.length(); i++) {
                    char c = format.charAt(i);

                    if (c == '\'') {
                        quoted = !quoted;
                    }
                    if (!quoted && c == 'a') {
                        a = i;
                        break;
                    }
                }

                if (a >= 0) {
                    // Move a back so any whitespace before AM/PM is also in the alternate size.
                    final int b = a;
                    while (a > 0 && UCharacter.isUWhiteSpace(format.charAt(a - 1))) {
                        a--;
                    }
                    format = format.substring(0, a) + MAGIC1 + format.substring(a, b)
                        + "a" + MAGIC2 + format.substring(b + 1);
                }
            }
            mClockFormat = new SimpleDateFormat(format);
        }

        CharSequence dateString = null;

        String result = "";
        String timeResult = mClockFormat.format(mCalendar.getTime());
        String dateResult = "";

        if (!mQsHeader && mClockDateDisplay != CLOCK_DATE_DISPLAY_GONE) {
            Date now = new Date();

            if (mClockDateFormat == null || mClockDateFormat.isEmpty()) {
                // Set dateString to short uppercase Weekday if empty
                dateString = DateFormat.format("EEE", now);
            } else {
                dateString = DateFormat.format(mClockDateFormat, now);
            }
            if (mClockDateStyle == CLOCK_DATE_STYLE_LOWERCASE) {
                // When Date style is small, convert date to uppercase
                dateResult = dateString.toString().toLowerCase();
            } else if (mClockDateStyle == CLOCK_DATE_STYLE_UPPERCASE) {
                dateResult = dateString.toString().toUpperCase();
            } else {
                dateResult = dateString.toString();
            }
            result = (mClockDatePosition == STYLE_DATE_LEFT) ? dateResult + " " + timeResult
                    : timeResult + " " + dateResult;
        } else {
            // No date, just show time
            result = timeResult;
        }

        SpannableStringBuilder formatted = new SpannableStringBuilder(result);

        if (mClockDateDisplay != CLOCK_DATE_DISPLAY_NORMAL) {
            if (dateString != null) {
                int dateStringLen = dateString.length();
                int timeStringOffset = (mClockDatePosition == STYLE_DATE_RIGHT)
                        ? timeResult.length() + 1 : 0;
                if (mClockDateDisplay == CLOCK_DATE_DISPLAY_GONE) {
                   formatted.delete(0, dateStringLen);
                } else {
                    if (mClockDateDisplay == CLOCK_DATE_DISPLAY_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, timeStringOffset,
                                timeStringOffset + dateStringLen,
                                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                }
            }
        }

        if (mAmPmStyle != AM_PM_STYLE_NORMAL) {
            int magic1 = result.indexOf(MAGIC1);
            int magic2 = result.indexOf(MAGIC2);
            if (magic1 >= 0 && magic2 > magic1) {
                if (mAmPmStyle == AM_PM_STYLE_GONE) {
                    formatted.delete(magic1, magic2+1);
                } else {
                    if (mAmPmStyle == AM_PM_STYLE_SMALL) {
                        CharacterStyle style = new RelativeSizeSpan(0.7f);
                        formatted.setSpan(style, magic1, magic2,
                                          Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    }
                    formatted.delete(magic2, magic2 + 1);
                    formatted.delete(magic1, magic1 + 1);
                }
            }
        }

        return formatted;
    }

    private boolean mDemoMode;

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        // Only registered for COMMAND_CLOCK
        String millis = args.getString("millis");
        String hhmm = args.getString("hhmm");
        if (millis != null) {
            mCalendar.setTimeInMillis(Long.parseLong(millis));
        } else if (hhmm != null && hhmm.length() == 4) {
            int hh = Integer.parseInt(hhmm.substring(0, 2));
            int mm = Integer.parseInt(hhmm.substring(2));
            boolean is24 = DateFormat.is24HourFormat(getContext(), mCurrentUserId);
            if (is24) {
                mCalendar.set(Calendar.HOUR_OF_DAY, hh);
            } else {
                mCalendar.set(Calendar.HOUR, hh);
            }
            mCalendar.set(Calendar.MINUTE, mm);
        }
        setText(getSmallTime());
        setContentDescription(mContentDescriptionFormat.format(mCalendar.getTime()));
    }

    @Override
    public void onDemoModeStarted() {
        mDemoMode = true;
    }

    @Override
    public void onDemoModeFinished() {
        mDemoMode = false;
        updateClock();
    }

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (mSecondsHandler != null) {
                    mSecondsHandler.removeCallbacks(mSecondTick);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (mSecondsHandler != null) {
                    mSecondsHandler.postAtTime(mSecondTick,
                            SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
                }
            }
        }
    };

    private final Runnable mSecondTick = new Runnable() {
        @Override
        public void run() {
            if (mCalendar != null) {
                updateClock();
            }
            mSecondsHandler.postAtTime(this, SystemClock.uptimeMillis() / 1000 * 1000 + 1000);
        }
    };

    private final TaskStackChangeListener mTaskStackListener = new TaskStackChangeListener() {
        @Override
        public void onTaskStackChanged() {
            updateShowClock();
        }

        @Override
        public void onTaskRemoved(int taskId) {
            updateShowClock();
        }

        @Override
        public void onTaskMovedToFront(int taskId) {
            updateShowClock();
        }
    };

    public void updateClockSize() {
        if (mQsHeader) {
            setTextSize(mClockSizeQsHeader);
        } else {
            setTextSize(mClockSize);
        }
    }
}
