/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.systemui.battery;

import static com.android.settingslib.flags.Flags.newStatusBarIcons;
import static com.android.systemui.Flags.gsfQuickSettings;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;

import com.android.app.animation.Interpolators;
import com.android.settingslib.graph.CircleBatteryDrawable;
import com.android.settingslib.graph.FullCircleBatteryDrawable;
import com.android.settingslib.graph.RLandscapeBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryDrawable;
import com.android.settingslib.graph.RLandscapeBatteryDrawableStyleA;
import com.android.settingslib.graph.LandscapeBatteryDrawableStyleA;
import com.android.settingslib.graph.RLandscapeBatteryDrawableStyleB;
import com.android.settingslib.graph.LandscapeBatteryDrawableStyleB;
import com.android.settingslib.graph.LandscapeBatteryDrawableBuddy;
import com.android.settingslib.graph.LandscapeBatteryDrawableLine;
import com.android.settingslib.graph.LandscapeBatteryDrawableSignal;
import com.android.settingslib.graph.LandscapeBatteryDrawableMusku;
import com.android.settingslib.graph.LandscapeBatteryDrawablePill;
import com.android.settingslib.graph.LandscapeBatteryDrawableiOS15;
import com.android.settingslib.graph.LandscapeBatteryDrawableiOS16;
import com.android.settingslib.graph.LandscapeBatteryDrawableOrigami;
import com.android.settingslib.graph.LandscapeBatteryDrawableOneUI7;
import com.android.systemui.DualToneHandler;
import com.android.systemui.battery.unified.BatteryColors;
import com.android.systemui.battery.unified.BatteryDrawableState;
import com.android.systemui.battery.unified.BatteryLayersDrawable;
import com.android.systemui.battery.unified.ColorProfile;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.res.R;
import com.android.systemui.statusbar.policy.BatteryController;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.text.NumberFormat;
import java.util.ArrayList;

public class BatteryMeterView extends LinearLayout implements DarkReceiver {

    protected static final int BATTERY_STYLE_PORTRAIT = 0;
    protected static final int BATTERY_STYLE_CIRCLE = 1;
    protected static final int BATTERY_STYLE_DOTTED_CIRCLE = 2;
    protected static final int BATTERY_STYLE_FULL_CIRCLE = 3;
    protected static final int BATTERY_STYLE_TEXT = 4;
    protected static final int BATTERY_STYLE_HIDDEN = 5;
    protected static final int BATTERY_STYLE_RLANDSCAPE = 6;
    protected static final int BATTERY_STYLE_LANDSCAPE = 7;
    protected static final int BATTERY_STYLE_BIG_CIRCLE = 8;
    protected static final int BATTERY_STYLE_BIG_DOTTED_CIRCLE = 9;
    protected static final int BATTERY_STYLE_LANDSCAPE_BUDDY = 10;
    protected static final int BATTERY_STYLE_LANDSCAPE_LINE = 11;
    protected static final int BATTERY_STYLE_LANDSCAPE_MUSKU = 12;
    protected static final int BATTERY_STYLE_LANDSCAPE_PILL = 13;
    protected static final int BATTERY_STYLE_LANDSCAPE_SIGNAL = 14;
    protected static final int BATTERY_STYLE_RLANDSCAPE_STYLE_A = 15;
    protected static final int BATTERY_STYLE_LANDSCAPE_STYLE_A = 16;
    protected static final int BATTERY_STYLE_RLANDSCAPE_STYLE_B = 17;
    protected static final int BATTERY_STYLE_LANDSCAPE_STYLE_B = 18;
    protected static final int BATTERY_STYLE_LANDSCAPE_IOS15 = 19;
    protected static final int BATTERY_STYLE_LANDSCAPE_IOS16 = 20;
    protected static final int BATTERY_STYLE_LANDSCAPE_ORIGAMI = 21;
    protected static final int BATTERY_STYLE_LANDSCAPE_ONEUI7 = 22;

    @Retention(SOURCE)
    @IntDef({MODE_DEFAULT, MODE_ON, MODE_OFF, MODE_ESTIMATE})
    public @interface BatteryPercentMode {}
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ON = 1;
    public static final int MODE_OFF = 2;
    public static final int MODE_ESTIMATE = 3;

    private final AccessorizedBatteryDrawable mAccessorizedDrawable;
    private final CircleBatteryDrawable mCircleDrawable;
    private final FullCircleBatteryDrawable mFullCircleDrawable;
    private final RLandscapeBatteryDrawable mRLandscapeDrawable;
    private final LandscapeBatteryDrawable mLandscapeDrawable;
    private final RLandscapeBatteryDrawableStyleA mRLandscapeDrawableStyleA;
    private final LandscapeBatteryDrawableStyleA mLandscapeDrawableStyleA;
    private final RLandscapeBatteryDrawableStyleB mRLandscapeDrawableStyleB;
    private final LandscapeBatteryDrawableStyleB mLandscapeDrawableStyleB;
    private final LandscapeBatteryDrawableBuddy mLandscapeDrawableBuddy;
    private final LandscapeBatteryDrawableLine mLandscapeDrawableLine;
    private final LandscapeBatteryDrawableMusku mLandscapeDrawableMusku;
    private final LandscapeBatteryDrawablePill mLandscapeDrawablePill;
    private final LandscapeBatteryDrawableSignal mLandscapeDrawableSignal;
    private final LandscapeBatteryDrawableiOS15 mLandscapeDrawableiOS15;
    private final LandscapeBatteryDrawableiOS16 mLandscapeDrawableiOS16;
    private final LandscapeBatteryDrawableOrigami mLandscapeDrawableOrigami;
    private final LandscapeBatteryDrawableOneUI7 mLandscapeDrawableOneUI7;
    private final ImageView mBatteryIconView;
    private TextView mBatteryPercentView;

    private final @StyleRes int mPercentageStyleId;
    private int mTextColor;
    private int mLevel;
    private int mShowPercentMode = MODE_DEFAULT;
    private String mEstimateText = null;
    private boolean mPluggedIn;
    private boolean mPowerSaveEnabled;
    private boolean mIsBatteryDefender;
    private boolean mIsIncompatibleCharging;
    // Error state where we know nothing about the current battery state
    private boolean mBatteryStateUnknown;
    // Lazily-loaded since this is expected to be a rare-if-ever state
    private Drawable mUnknownStateDrawable;

    private int mBatteryStyle = BATTERY_STYLE_PORTRAIT;
    private int mShowBatteryPercent;
    private boolean mBatteryPercentCharging;

    private DualToneHandler mDualToneHandler;
    private boolean mIsStaticColor = false;

    private BatteryEstimateFetcher mBatteryEstimateFetcher;

    // for Flags.newStatusBarIcons. The unified battery icon can show percent inside
    @Nullable private BatteryLayersDrawable mUnifiedBattery;
    private BatteryColors mUnifiedBatteryColors = BatteryColors.LIGHT_THEME_COLORS;
    private BatteryDrawableState mUnifiedBatteryState =
            BatteryDrawableState.Companion.getDefaultInitialState();

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(com.android.settingslib.R.color.meter_background_color));
        mPercentageStyleId = atts.getResourceId(R.styleable.BatteryMeterView_textAppearance, 0);

        mAccessorizedDrawable = new AccessorizedBatteryDrawable(context, frameColor);
        mCircleDrawable = new CircleBatteryDrawable(context, frameColor);
        mFullCircleDrawable = new FullCircleBatteryDrawable(context, frameColor);
        mRLandscapeDrawable = new RLandscapeBatteryDrawable(context, frameColor);
        mLandscapeDrawable = new LandscapeBatteryDrawable(context, frameColor);
        mRLandscapeDrawableStyleA = new RLandscapeBatteryDrawableStyleA(context, frameColor);
        mLandscapeDrawableStyleA = new LandscapeBatteryDrawableStyleA(context, frameColor);
        mRLandscapeDrawableStyleB = new RLandscapeBatteryDrawableStyleB(context, frameColor);
        mLandscapeDrawableStyleB = new LandscapeBatteryDrawableStyleB(context, frameColor);
        mLandscapeDrawableBuddy = new LandscapeBatteryDrawableBuddy(context, frameColor);
        mLandscapeDrawableLine = new LandscapeBatteryDrawableLine(context, frameColor);
        mLandscapeDrawableMusku = new LandscapeBatteryDrawableMusku(context, frameColor);
        mLandscapeDrawablePill = new LandscapeBatteryDrawablePill(context, frameColor);
        mLandscapeDrawableSignal = new LandscapeBatteryDrawableSignal(context, frameColor);
        mLandscapeDrawableiOS15 = new LandscapeBatteryDrawableiOS15(context, frameColor);
        mLandscapeDrawableiOS16 = new LandscapeBatteryDrawableiOS16(context, frameColor);
        mLandscapeDrawableOrigami = new LandscapeBatteryDrawableOrigami(context, frameColor);
        mLandscapeDrawableOneUI7 = new LandscapeBatteryDrawableOneUI7(context, frameColor);
        atts.recycle();

        setupLayoutTransition();

        mBatteryIconView = new ImageView(context);
        mBatteryStyle = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        if (newStatusBarIcons()) {
            mUnifiedBattery = BatteryLayersDrawable.Companion
                    .newBatteryDrawable(context, mUnifiedBatteryState);
            mBatteryIconView.setImageDrawable(mUnifiedBattery);

            final MarginLayoutParams mlp = new MarginLayoutParams(
                    getResources().getDimensionPixelSize(
                            R.dimen.status_bar_battery_unified_icon_width),
                    getResources().getDimensionPixelSize(
                            R.dimen.status_bar_battery_unified_icon_height));
            addView(mBatteryIconView, mlp);
        } else {
            updateDrawable();
            int batteryHeight = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                    || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                    getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                    getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
            int batteryWidth = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                    || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                    getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                    getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);

            final MarginLayoutParams mlp = new MarginLayoutParams(batteryWidth, batteryHeight);
            mlp.setMargins(0, 0, 0,
                    getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
            addView(mBatteryIconView, mlp);
        }

        updatePercentView();
        mDualToneHandler = new DualToneHandler(context);
        // Init to not dark at all.
        if (isNightMode()) {
            onDarkChanged(new ArrayList<Rect>(), 0, DarkIconDispatcher.DEFAULT_ICON_TINT);
        }

        setClipChildren(false);
        setClipToPadding(false);
    }

    private boolean isNightMode() {
        return (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setBatteryDrawableState(BatteryDrawableState newState) {
        if (!newStatusBarIcons()) return;

        mUnifiedBatteryState = newState;
        mUnifiedBattery.setBatteryState(mUnifiedBatteryState);
    }

    private void setupLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200);

        // Animates appearing/disappearing of the battery percentage text using fade-in/fade-out
        // and disables all other animation types
        ObjectAnimator appearAnimator = ObjectAnimator.ofFloat(null, "alpha", 0f, 1f);
        transition.setAnimator(LayoutTransition.APPEARING, appearAnimator);
        transition.setInterpolator(LayoutTransition.APPEARING, Interpolators.ALPHA_IN);

        ObjectAnimator disappearAnimator = ObjectAnimator.ofFloat(null, "alpha", 1f, 0f);
        transition.setInterpolator(LayoutTransition.DISAPPEARING, Interpolators.ALPHA_OUT);
        transition.setAnimator(LayoutTransition.DISAPPEARING, disappearAnimator);

        transition.setAnimator(LayoutTransition.CHANGE_APPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGING, null);

        setLayoutTransition(transition);
    }

    public int getBatteryStyle() {
        return mBatteryStyle;
    }

    public void setBatteryStyle(int batteryStyle) {
        if (batteryStyle == mBatteryStyle) return;
        mBatteryStyle = batteryStyle;
        updateBatteryStyle();
    }

    protected void updateBatteryStyle() {
        updateDrawable();
        scaleBatteryMeterViews();
        updatePercentView();
    }

    public void setBatteryPercent(int showBatteryPercent) {
        if (showBatteryPercent == mShowBatteryPercent) return;
        mShowBatteryPercent = showBatteryPercent;
        updatePercentView();
    }

    protected void setBatteryPercentCharging(boolean batteryPercentCharging) {
        if (batteryPercentCharging == mBatteryPercentCharging) return;
        mBatteryPercentCharging = batteryPercentCharging;
        updatePercentView();
    }

    public void setForceShowPercent(boolean show) {
        setPercentShowMode(show ? MODE_ON : MODE_DEFAULT);
    }

    /**
     * Force a particular mode of showing percent
     *
     * 0 - No preference
     * 1 - Force on
     * 2 - Force off
     * 3 - Estimate
     * @param mode desired mode (none, on, off)
     */
    public void setPercentShowMode(@BatteryPercentMode int mode) {
        if (mode == mShowPercentMode) return;
        mShowPercentMode = mode;
        updateShowPercent();
        updatePercentText();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateBatteryStyle();
        mAccessorizedDrawable.notifyDensityChanged();
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }

        mDualToneHandler.setColorsFromContext(context);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Update battery level
     *
     * @param level     int between 0 and 100 (representing percentage value)
     * @param pluggedIn whether the device is plugged in or not
     */
    public void onBatteryLevelChanged(@IntRange(from = 0, to = 100) int level, boolean pluggedIn) {
        boolean wasCharging = isCharging();
        boolean wasPluggedIn = mPluggedIn;
        mPluggedIn = pluggedIn;
        mLevel = level;
        boolean isCharging = isCharging();
        mAccessorizedDrawable.setCharging(isCharging);
        mCircleDrawable.setCharging(isCharging);
        mFullCircleDrawable.setCharging(isCharging);
        mRLandscapeDrawable.setCharging(isCharging());
        mLandscapeDrawable.setCharging(isCharging());
        mRLandscapeDrawableStyleA.setCharging(isCharging());
        mLandscapeDrawableStyleA.setCharging(isCharging());
        mRLandscapeDrawableStyleB.setCharging(isCharging());
        mLandscapeDrawableStyleB.setCharging(isCharging());
        mLandscapeDrawableBuddy.setCharging(isCharging());
        mLandscapeDrawableLine.setCharging(isCharging());
        mLandscapeDrawableMusku.setCharging(isCharging());
        mLandscapeDrawablePill.setCharging(isCharging());
        mLandscapeDrawableSignal.setCharging(isCharging());
        mLandscapeDrawableiOS15.setCharging(isCharging());
        mLandscapeDrawableiOS16.setCharging(isCharging());
        mLandscapeDrawableOrigami.setCharging(isCharging());
        mLandscapeDrawableOneUI7.setCharging(isCharging());
        mAccessorizedDrawable.setBatteryLevel(level);
        mCircleDrawable.setBatteryLevel(level);
        mFullCircleDrawable.setBatteryLevel(level);
        mRLandscapeDrawable.setBatteryLevel(level);
        mLandscapeDrawable.setBatteryLevel(level);
        mRLandscapeDrawableStyleA.setBatteryLevel(level);
        mLandscapeDrawableStyleA.setBatteryLevel(level);
        mRLandscapeDrawableStyleB.setBatteryLevel(level);
        mLandscapeDrawableStyleB.setBatteryLevel(level);
        mLandscapeDrawableBuddy.setBatteryLevel(level);
        mLandscapeDrawableLine.setBatteryLevel(level);
        mLandscapeDrawableMusku.setBatteryLevel(level);
        mLandscapeDrawablePill.setBatteryLevel(level);
        mLandscapeDrawableSignal.setBatteryLevel(level);
        mLandscapeDrawableiOS15.setBatteryLevel(level);
        mLandscapeDrawableiOS16.setBatteryLevel(level);
        mLandscapeDrawableOrigami.setBatteryLevel(level);
        mLandscapeDrawableOneUI7.setBatteryLevel(level);
        updatePercentText();
        if (wasPluggedIn != mPluggedIn) {
            updateShowPercent();
        }

        if (newStatusBarIcons()) {
            Drawable attr = mUnifiedBatteryState.getAttribution();
            if (isCharging != wasCharging) {
                attr = getBatteryAttribution(isCharging);
            }

            BatteryDrawableState newState =
                    new BatteryDrawableState(
                            level,
                            mUnifiedBatteryState.getShowPercent(),
                            getCurrentColorProfile(),
                            attr
                    );

            setBatteryDrawableState(newState);
        }
    }

    // Potentially reloads any attribution. Should not be called if the state hasn't changed
    @SuppressLint("UseCompatLoadingForDrawables")
    private Drawable getBatteryAttribution(boolean isCharging) {
        if (!newStatusBarIcons()) return null;

        int resId = 0;
        if (mPowerSaveEnabled) {
            resId = R.drawable.battery_unified_attr_powersave;
        } else if (mIsBatteryDefender) {
            resId = R.drawable.battery_unified_attr_defend;
        } else if (isCharging) {
            resId = R.drawable.battery_unified_attr_charging;
        }

        Drawable attr = null;
        if (resId > 0) {
            attr = mContext.getDrawable(resId);
        }

        return attr;
    }

    /** Calculate the appropriate color for the current state */
    private ColorProfile getCurrentColorProfile() {
        return getColorProfile(
                mPowerSaveEnabled,
                mIsBatteryDefender,
                mPluggedIn,
                mLevel <= 20);
    }

    /** pure function to compute the correct color profile for our battery icon */
    private ColorProfile getColorProfile(
            boolean isPowerSave,
            boolean isBatteryDefender,
            boolean isCharging,
            boolean isLowBattery
    ) {
        if (isCharging)  return ColorProfile.Active;
        if (isPowerSave) return ColorProfile.Warning;
        if (isBatteryDefender) return ColorProfile.None;
        if (isLowBattery) return ColorProfile.Error;

        return ColorProfile.None;
    }

    void onPowerSaveChanged(boolean isPowerSave) {
        if (isPowerSave == mPowerSaveEnabled) {
            return;
        }
        mPowerSaveEnabled = isPowerSave;
        if (!newStatusBarIcons()) {
            mAccessorizedDrawable.setPowerSaveEnabled(isPowerSave);
            mCircleDrawable.setPowerSaveEnabled(isPowerSave);
            mFullCircleDrawable.setPowerSaveEnabled(isPowerSave);
            mRLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
            mRLandscapeDrawableStyleA.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableStyleA.setPowerSaveEnabled(isPowerSave);
            mRLandscapeDrawableStyleB.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableStyleB.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableBuddy.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableLine.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableMusku.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawablePill.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableSignal.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableiOS15.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableiOS16.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableOrigami.setPowerSaveEnabled(isPowerSave);
            mLandscapeDrawableOneUI7.setPowerSaveEnabled(isPowerSave);
        } else {
            setBatteryDrawableState(
                    new BatteryDrawableState(
                            mUnifiedBatteryState.getLevel(),
                            mUnifiedBatteryState.getShowPercent(),
                            getCurrentColorProfile(),
                            getBatteryAttribution(isCharging())
                    )
            );
        }
    }

    void onIsBatteryDefenderChanged(boolean isBatteryDefender) {
        boolean valueChanged = mIsBatteryDefender != isBatteryDefender;
        mIsBatteryDefender = isBatteryDefender;

        if (!valueChanged) {
            return;
        }

        updateContentDescription();
        if (!newStatusBarIcons()) {
            // The battery drawable is a different size depending on whether it's currently
            // overheated or not, so we need to re-scale the view when overheated changes.
            scaleBatteryMeterViews();
        } else {
            setBatteryDrawableState(
                    new BatteryDrawableState(
                            mUnifiedBatteryState.getLevel(),
                            mUnifiedBatteryState.getShowPercent(),
                            getCurrentColorProfile(),
                            getBatteryAttribution(isCharging())
                    )
            );
        }
    }

    void onIsIncompatibleChargingChanged(boolean isIncompatibleCharging) {
        boolean valueChanged = mIsIncompatibleCharging != isIncompatibleCharging;
        mIsIncompatibleCharging = isIncompatibleCharging;
        if (valueChanged) {
            if (newStatusBarIcons()) {
                setBatteryDrawableState(
                        new BatteryDrawableState(
                                mUnifiedBatteryState.getLevel(),
                                mUnifiedBatteryState.getShowPercent(),
                                getCurrentColorProfile(),
                                getBatteryAttribution(isCharging())
                        )
                );
            } else {
                mAccessorizedDrawable.setCharging(isCharging());
            }
            updateContentDescription();
        }
    }

    private TextView inflatePercentView() {
        return (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.battery_percentage_view, null);
    }

    private void addPercentView(TextView inflatedPercentView) {
        mBatteryPercentView = inflatedPercentView;

        if (mPercentageStyleId != 0) { // Only set if specified as attribute
            mBatteryPercentView.setTextAppearance(mPercentageStyleId);
        }
        float fontHeight = mBatteryPercentView.getPaint().getFontMetricsInt(null);
        mBatteryPercentView.setLineHeight(TypedValue.COMPLEX_UNIT_PX, fontHeight);
        if (gsfQuickSettings()) {
            mBatteryPercentView.setTypeface(Typeface.create("gsf-label-large", Typeface.NORMAL));
        }
        if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
        addView(mBatteryPercentView, new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                (int) Math.ceil(fontHeight)));
    }

    /**
     * Updates percent view by removing old one and reinflating if necessary
     */
    public void updatePercentView() {
        if (mBatteryPercentView != null) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
        updateShowPercent();
    }

    /**
     * Sets the fetcher that should be used to get the estimated time remaining for the user's
     * battery.
     */
    void setBatteryEstimateFetcher(BatteryEstimateFetcher fetcher) {
        mBatteryEstimateFetcher = fetcher;
    }

    void updatePercentText() {
        if (!newStatusBarIcons()) {
            updatePercentTextLegacy();
            return;
        }

        // The unified battery can show the percent inside, so we only need to handle
        // the estimated time remaining case
        if (mShowPercentMode == MODE_ESTIMATE
                && mBatteryEstimateFetcher != null
                && !isCharging()
        ) {
            mBatteryEstimateFetcher.fetchBatteryTimeRemainingEstimate(
                    (String estimate) -> {
                        if (mBatteryPercentView == null) {
                            // Similar to the legacy behavior, inflate and add the view. We will
                            // only use it for the estimate text
                            addPercentView(inflatePercentView());
                        }
                        if (estimate != null && mShowPercentMode == MODE_ESTIMATE) {
                            mEstimateText = estimate;
                            mBatteryPercentView.setText(estimate);
                            updateContentDescription();
                        } else {
                            mEstimateText = null;
                            mBatteryPercentView.setText(null);
                            updateContentDescription();
                        }
                    });
        } else {
            if (mBatteryPercentView != null) {
                mEstimateText = null;
                mBatteryPercentView.setText(null);
            }
            updateContentDescription();
        }
    }

    void updatePercentTextLegacy() {
        if (mBatteryStateUnknown) {
            return;
        }

        if (mBatteryPercentView != null) {
            setPercentTextAtCurrentLevel();
        } else {
            updateContentDescription();
        }
    }

    private void setPercentTextAtCurrentLevel() {
        String text = NumberFormat.getPercentInstance().format(mLevel / 100f);

        mEstimateText = null;

        if (mBatteryEstimateFetcher != null && mShowPercentMode == MODE_ESTIMATE && !isCharging()) {
            mBatteryEstimateFetcher.fetchBatteryTimeRemainingEstimate(
                    (String estimate) -> {
                if (mBatteryPercentView == null) {
                    return;
                }
                if (estimate != null && mShowPercentMode == MODE_ESTIMATE) {
                    mEstimateText = estimate;
                    if (!TextUtils.equals(mBatteryPercentView.getText(), estimate)) {
                        mBatteryPercentView.setText(estimate);
                    }
                } else {
                    if (!TextUtils.equals(mBatteryPercentView.getText(), text)) {
                       mBatteryPercentView.setText(text);
                    }
                }
            });
        } else {
            // Use the high voltage symbol ⚡ (u26A1 unicode) but prevent the system
            // to load its emoji colored variant with the uFE0E flag
            String bolt = "\u26A1";
            CharSequence mChargeIndicator = isCharging() && (mBatteryStyle == BATTERY_STYLE_HIDDEN ||
                    mBatteryStyle == BATTERY_STYLE_TEXT) ? (bolt + " ") : "";
            String percentText = mChargeIndicator + text;
            // Setting text actually triggers a layout pass (because the text view is set to
            // wrap_content width and TextView always relayouts for this). Avoid needless
            // relayout if the text didn't actually change.
            if (!TextUtils.equals(mBatteryPercentView.getText(), percentText)) {
                mBatteryPercentView.setText(percentText);
            }
        }

        updateContentDescription();
    }

    private void updateContentDescription() {
        Context context = getContext();

        String contentDescription;
        if (mBatteryStateUnknown) {
            contentDescription = context.getString(R.string.accessibility_battery_unknown);
        } else if (mShowPercentMode == MODE_ESTIMATE && !TextUtils.isEmpty(mEstimateText)) {
            contentDescription = context.getString(
                    mIsBatteryDefender
                            ? R.string.accessibility_battery_level_charging_paused_with_estimate
                            : R.string.accessibility_battery_level_with_estimate,
                    mLevel,
                    mEstimateText);
        } else if (mIsBatteryDefender) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging_paused, mLevel);
        } else if (isCharging()) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging, mLevel);
        } else {
            contentDescription = context.getString(R.string.accessibility_battery_level, mLevel);
        }

        setContentDescription(contentDescription);
    }

    private void removeBatteryPercentView() {
        if (mBatteryPercentView != null) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
    }

    void updateShowPercent() {
        if (!newStatusBarIcons()) {
            updateShowPercentLegacy();
            return;
        }

        if (mUnifiedBattery == null) return;

        // TODO(b/140051051)
        final int showBatteryPercent = Settings.System.getIntForUser(
                getContext().getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0,
                UserHandle.USER_CURRENT);
        final boolean drawPercentInside = mShowPercentMode == MODE_DEFAULT &&
                showBatteryPercent == 1;
        final boolean drawPercentOnly = mShowPercentMode == MODE_ESTIMATE ||
                showBatteryPercent >= 2;
        boolean shouldShow =
                drawPercentOnly || (mBatteryPercentCharging && isCharging()) ||
                mBatteryStyle == BATTERY_STYLE_TEXT;
        shouldShow = shouldShow && !mBatteryStateUnknown;

        setBatteryDrawableState(
                new BatteryDrawableState(
                        mUnifiedBatteryState.getLevel(),
                        shouldShow,
                        mUnifiedBatteryState.getColor(),
                        mUnifiedBatteryState.getAttribution()
                )
        );

        // The legacy impl used the percent view for the estimate and the percent text. The modern
        // version only uses it for estimate. It can be safely removed here
        if (mShowPercentMode != MODE_ESTIMATE) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
    }

    private void updateShowPercentLegacy() {
        boolean drawPercentInside = mShowBatteryPercent == 1
                                    && !isCharging() && !mBatteryStateUnknown;
        boolean showPercent = mShowBatteryPercent >= 2
                                    || mBatteryStyle == BATTERY_STYLE_TEXT
                                    || mShowPercentMode == MODE_ON;
        showPercent = showPercent && !mBatteryStateUnknown
                                    && mBatteryStyle != BATTERY_STYLE_HIDDEN
                                    && mBatteryStyle != BATTERY_STYLE_LANDSCAPE_IOS16
                                    && mBatteryStyle != BATTERY_STYLE_LANDSCAPE_ONEUI7;

        mAccessorizedDrawable.showPercent(drawPercentInside);
        mCircleDrawable.setShowPercent(drawPercentInside);
        mFullCircleDrawable.setShowPercent(drawPercentInside);
        mRLandscapeDrawable.setShowPercent(drawPercentInside);
        mLandscapeDrawable.setShowPercent(drawPercentInside);
        mRLandscapeDrawableStyleA.setShowPercent(drawPercentInside);
        mLandscapeDrawableStyleA.setShowPercent(drawPercentInside);
        mRLandscapeDrawableStyleB.setShowPercent(drawPercentInside);
        mLandscapeDrawableStyleB.setShowPercent(drawPercentInside);
        mLandscapeDrawableBuddy.setShowPercent(drawPercentInside);
        mLandscapeDrawableLine.setShowPercent(drawPercentInside);
        mLandscapeDrawableMusku.setShowPercent(drawPercentInside);
        mLandscapeDrawablePill.setShowPercent(drawPercentInside);
        mLandscapeDrawableSignal.setShowPercent(drawPercentInside);
        mLandscapeDrawableiOS15.setShowPercent(drawPercentInside);
        mLandscapeDrawableiOS16.setShowPercent(drawPercentInside);
        mLandscapeDrawableOrigami.setShowPercent(drawPercentInside);
        mLandscapeDrawableOneUI7.setShowPercent(drawPercentInside);

        if (showPercent || (mBatteryPercentCharging && isCharging())
                || mShowPercentMode == MODE_ESTIMATE) {
            if (mBatteryPercentView == null) {
                addPercentView(inflatePercentView());
                updatePercentText();
            }
            if (mBatteryStyle == BATTERY_STYLE_HIDDEN || mBatteryStyle == BATTERY_STYLE_TEXT) {
                mBatteryPercentView.setPaddingRelative(0, 0, 0, 0);
            } else {
                Resources res = getContext().getResources();
                mBatteryPercentView.setPaddingRelative(
                        res.getDimensionPixelSize(R.dimen.battery_level_padding_start), 0, 0, 0);
                setLayoutDirection(mShowBatteryPercent > 2 ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            }

        } else {
            removeBatteryPercentView();
        }
    }

    private Drawable getUnknownStateDrawable() {
        if (mUnknownStateDrawable == null) {
            mUnknownStateDrawable = mContext.getDrawable(R.drawable.ic_battery_unknown);
            mUnknownStateDrawable.setTint(mTextColor);
        }

        return mUnknownStateDrawable;
    }

    void onBatteryUnknownStateChanged(boolean isUnknown) {
        if (mBatteryStateUnknown == isUnknown) {
            return;
        }

        mBatteryStateUnknown = isUnknown;
        updateContentDescription();

        if (mBatteryStateUnknown) {
            mBatteryIconView.setImageDrawable(getUnknownStateDrawable());
        } else {
            updateDrawable();
        }

        updateShowPercent();
    }

    void scaleBatteryMeterViews() {
        if (!newStatusBarIcons()) {
            scaleBatteryMeterViewsLegacy();
            return;
        }

        // For simplicity's sake, copy the general pattern in the legacy method and use the new
        // resources, excluding what we don't need
        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();

        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();

        float mainBatteryHeight =
                res.getDimensionPixelSize(
                        R.dimen.status_bar_battery_unified_icon_height) * iconScaleFactor;
        float mainBatteryWidth =
                res.getDimensionPixelSize(
                        R.dimen.status_bar_battery_unified_icon_width) * iconScaleFactor;

        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                Math.round(mainBatteryWidth),
                Math.round(mainBatteryHeight));

        mBatteryIconView.setLayoutParams(scaledLayoutParams);
        mBatteryIconView.invalidateDrawable(mUnifiedBattery);
    }

    /**
     * Looks up the scale factor for status bar icons and scales the battery view by that amount.
     */
    void scaleBatteryMeterViewsLegacy() {
        if (mBatteryIconView == null) {
            return;
        }
        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();

        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();

        int batteryWidth;
        int batteryHeight;
        switch (mBatteryStyle) {
            case BATTERY_STYLE_CIRCLE:
            case BATTERY_STYLE_DOTTED_CIRCLE:
            case BATTERY_STYLE_FULL_CIRCLE:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width);
                batteryWidth = batteryHeight;
                break;
            case BATTERY_STYLE_BIG_CIRCLE:
            case BATTERY_STYLE_BIG_DOTTED_CIRCLE:
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_big_circle_width);
                batteryHeight = batteryWidth;
                break;
            case BATTERY_STYLE_LANDSCAPE:
            case BATTERY_STYLE_RLANDSCAPE:
            case BATTERY_STYLE_RLANDSCAPE_STYLE_A:
            case BATTERY_STYLE_LANDSCAPE_STYLE_A:
            case BATTERY_STYLE_RLANDSCAPE_STYLE_B:
            case BATTERY_STYLE_LANDSCAPE_STYLE_B:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape);
                break;
            case BATTERY_STYLE_LANDSCAPE_SIGNAL:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_signal);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_signal);
                break;
            case BATTERY_STYLE_LANDSCAPE_LINE:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_line);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_line);
                break;
            case BATTERY_STYLE_LANDSCAPE_PILL:
            case BATTERY_STYLE_LANDSCAPE_MUSKU:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_pill_musku);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_pill_musku);
                break;
            case  BATTERY_STYLE_LANDSCAPE_BUDDY:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_buddy);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_buddy);
                break;
            case BATTERY_STYLE_LANDSCAPE_IOS15:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_ios15);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_ios15);
                break;
            case BATTERY_STYLE_LANDSCAPE_IOS16:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_ios16);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_ios16);
                break;
            case BATTERY_STYLE_LANDSCAPE_ORIGAMI:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_origami);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_origami);
                break;
            case BATTERY_STYLE_LANDSCAPE_ONEUI7:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_oneui7);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_oneui7);
                break;
            default:
                batteryHeight = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
                batteryWidth = res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
                break;
        }

        float mainBatteryHeight = batteryHeight * iconScaleFactor;
        float mainBatteryWidth = batteryWidth * iconScaleFactor;

        boolean displayShield = mIsBatteryDefender && getBatteryStyle() == BATTERY_STYLE_PORTRAIT;
        float fullBatteryIconHeight =
                BatterySpecs.getFullBatteryHeight(mainBatteryHeight, displayShield);
        float fullBatteryIconWidth =
                BatterySpecs.getFullBatteryWidth(mainBatteryWidth, displayShield);

        int marginTop;
        if (displayShield) {
            // If the shield is displayed, we need some extra marginTop so that the bottom of the
            // main icon is still aligned with the bottom of all the other system icons.
            int shieldHeightAddition = Math.round(fullBatteryIconHeight - mainBatteryHeight);
            // However, the other system icons have some embedded bottom padding that the battery
            // doesn't have, so we shouldn't move the battery icon down by the full amount.
            // See b/258672854.
            marginTop = shieldHeightAddition
                    - res.getDimensionPixelSize(R.dimen.status_bar_battery_extra_vertical_spacing);
        } else {
            marginTop = 0;
        }

        int marginBottom = res.getDimensionPixelSize(R.dimen.battery_margin_bottom);

        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                Math.round(fullBatteryIconWidth),
                Math.round(fullBatteryIconHeight));
        scaledLayoutParams.setMargins(0, marginTop, 0, marginBottom);

        mAccessorizedDrawable.setDisplayShield(displayShield);
        if (mBatteryIconView != null) {
            mBatteryIconView.setLayoutParams(scaledLayoutParams);
        }
        mBatteryIconView.invalidateDrawable(mAccessorizedDrawable);
    }

    private void updateDrawable() {
        switch (mBatteryStyle) {
            case BATTERY_STYLE_PORTRAIT:
                mBatteryIconView.setImageDrawable(mAccessorizedDrawable);
                break;
            case BATTERY_STYLE_RLANDSCAPE:
                mBatteryIconView.setImageDrawable(mRLandscapeDrawable);
                break;
            case BATTERY_STYLE_LANDSCAPE:
                mBatteryIconView.setImageDrawable(mLandscapeDrawable);
                break;
            case BATTERY_STYLE_RLANDSCAPE_STYLE_A:
                mBatteryIconView.setImageDrawable(mRLandscapeDrawableStyleA);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_STYLE_A:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableStyleA);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_RLANDSCAPE_STYLE_B:
                mBatteryIconView.setImageDrawable(mRLandscapeDrawableStyleB);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_STYLE_B:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableStyleB);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_BUDDY:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableBuddy);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_LINE:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableLine);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_MUSKU:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableMusku);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_PILL:
                mBatteryIconView.setImageDrawable(mLandscapeDrawablePill);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_SIGNAL:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableSignal);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_IOS15:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableiOS15);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_IOS16:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableiOS16);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_ORIGAMI:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableOrigami);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_ONEUI7:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableOneUI7);
                mBatteryIconView.setVisibility(View.VISIBLE);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_FULL_CIRCLE:
                mBatteryIconView.setImageDrawable(mFullCircleDrawable);
                break;
            case BATTERY_STYLE_CIRCLE:
            case BATTERY_STYLE_DOTTED_CIRCLE:
            case BATTERY_STYLE_BIG_CIRCLE:
            case BATTERY_STYLE_BIG_DOTTED_CIRCLE:
                mCircleDrawable.setMeterStyle(mBatteryStyle);
                mBatteryIconView.setImageDrawable(mCircleDrawable);
                break;
            case BATTERY_STYLE_HIDDEN:
            case BATTERY_STYLE_TEXT:
                mBatteryIconView.setImageDrawable(null);
                break;
        }
        boolean shouldHide = mBatteryStyle == BATTERY_STYLE_HIDDEN ||
            mBatteryStyle == BATTERY_STYLE_TEXT;
        mBatteryIconView.setVisibility(shouldHide ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        if (mIsStaticColor) return;

        if (!newStatusBarIcons()) {
            onDarkChangedLegacy(areas, darkIntensity, tint);
            return;
        }

        if (mUnifiedBattery == null) {
            return;
        }

        if (DarkIconDispatcher.isInAreas(areas, this)) {
            if (darkIntensity < 0.5) {
                mUnifiedBatteryColors = BatteryColors.DARK_THEME_COLORS;
            } else {
                mUnifiedBatteryColors = BatteryColors.LIGHT_THEME_COLORS;
            }

            mUnifiedBattery.setColors(mUnifiedBatteryColors);
        } else  {
            // Same behavior as the legacy code when not isInArea
            mUnifiedBatteryColors = BatteryColors.DARK_THEME_COLORS;
            mUnifiedBattery.setColors(mUnifiedBatteryColors);
        }
    }

    private void onDarkChangedLegacy(ArrayList<Rect> areas, float darkIntensity, int tint) {
        float intensity = DarkIconDispatcher.isInAreas(areas, this) ? darkIntensity : 0;
        int nonAdaptedSingleToneColor = mDualToneHandler.getSingleColor(intensity);
        int nonAdaptedForegroundColor = mDualToneHandler.getFillColor(intensity);
        int nonAdaptedBackgroundColor = mDualToneHandler.getBackgroundColor(intensity);

        updateColors(nonAdaptedForegroundColor, nonAdaptedBackgroundColor,
                nonAdaptedSingleToneColor);
    }

    public void setStaticColor(boolean isStaticColor) {
        mIsStaticColor = isStaticColor;
    }

    /**
     * Sets icon and text colors. This will be overridden by {@code onDarkChanged} events,
     * if registered.
     *
     * @param foregroundColor
     * @param backgroundColor
     * @param singleToneColor
     */
    public void updateColors(int foregroundColor, int backgroundColor, int singleToneColor) {
        mAccessorizedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mFullCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawableStyleA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableStyleA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawableStyleB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableStyleB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableBuddy.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableLine.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableMusku.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawablePill.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableSignal.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableiOS15.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableiOS16.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableOrigami.setColors(foregroundColor,backgroundColor, singleToneColor);
        mLandscapeDrawableOneUI7.setColors(foregroundColor,backgroundColor, singleToneColor);
        mTextColor = singleToneColor;
        if (mBatteryPercentView != null) {
            mBatteryPercentView.setTextColor(singleToneColor);
        }

        if (mUnknownStateDrawable != null) {
            mUnknownStateDrawable.setTint(singleToneColor);
        }
    }

    /** For newStatusBarIcons(), we use a BatteryColors object to declare the theme */
    public void setUnifiedBatteryColors(BatteryColors colors) {
        if (!newStatusBarIcons()) return;

        mUnifiedBatteryColors = colors;
        mUnifiedBattery.setColors(mUnifiedBatteryColors);
    }

    @VisibleForTesting
    boolean isCharging() {
        return mPluggedIn && !mIsIncompatibleCharging;
    }

    public void dump(PrintWriter pw, String[] args) {
        String powerSave = mAccessorizedDrawable == null ?
                null : mAccessorizedDrawable.getPowerSaveEnabled() + "";
        String displayShield = mAccessorizedDrawable == null ?
                null : mAccessorizedDrawable.getDisplayShield() + "";
        String charging = mAccessorizedDrawable == null ?
                null : mAccessorizedDrawable.getCharging() + "";
        CharSequence percent = mBatteryPercentView == null ? null : mBatteryPercentView.getText();
        pw.println("  BatteryMeterView:");
        pw.println("    mAccessorizedDrawable.getPowerSave: " + powerSave);
        pw.println("    mDrawable.getDisplayShield: " + displayShield);
        pw.println("    mDrawable.getCharging: " + charging);
        pw.println("    mBatteryPercentView.getText(): " + percent);
        pw.println("    mTextColor: #" + Integer.toHexString(mTextColor));
        pw.println("    mBatteryStateUnknown: " + mBatteryStateUnknown);
        pw.println("    mIsIncompatibleCharging: " + mIsIncompatibleCharging);
        pw.println("    mPluggedIn: " + mPluggedIn);
        pw.println("    mLevel: " + mLevel);
        pw.println("    mMode: " + mShowPercentMode);
        if (newStatusBarIcons()) {
            pw.println("    mUnifiedBatteryState: " + mUnifiedBatteryState);
        }
    }

    @VisibleForTesting
    CharSequence getBatteryPercentViewText() {
        return mBatteryPercentView.getText();
    }

    @VisibleForTesting
    TextView getBatteryPercentView() {
        return mBatteryPercentView;
    }

    @VisibleForTesting
    BatteryDrawableState getUnifiedBatteryState() {
        return mUnifiedBatteryState;
    }

    /** An interface that will fetch the estimated time remaining for the user's battery. */
    public interface BatteryEstimateFetcher {
        void fetchBatteryTimeRemainingEstimate(
                BatteryController.EstimateFetchCompletion completion);
    }
}

