/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.android.internal.policy.SystemBarUtils;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Flags;
import com.android.systemui.Gefingerpoken;
import com.android.systemui.res.R;
import com.android.systemui.shade.ShadeExpandsOnStatusBarLongPress;
import com.android.systemui.shade.StatusBarLongPressGestureDetector;
import com.android.systemui.shared.rotation.FloatingRotationButton;
import com.android.systemui.shared.rotation.RotationButtonController;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.phone.userswitcher.StatusBarUserSwitcherContainer;
import com.android.systemui.statusbar.policy.Offset;
import com.android.systemui.statusbar.window.StatusBarWindowControllerStore;
import com.android.systemui.user.ui.binder.StatusBarUserChipViewBinder;
import com.android.systemui.user.ui.viewmodel.StatusBarUserChipViewModel;
import com.android.systemui.util.leak.RotationUtils;
import com.android.systemui.tuner.TunerService;

import java.util.Objects;

public class PhoneStatusBarView extends FrameLayout implements Callbacks, TunerService.Tunable {

    private static final String STATUSBAR_LEFT_PADDING =
            "system:" + "statusbar_left_padding";
    private static final String STATUSBAR_RIGHT_PADDING =
            "system:" + "statusbar_right_padding";
    private static final String STATUSBAR_TOP_PADDING =
            "system:" + "statusbar_top_padding";
            
    private final TunerService mTunerService;

    private int mStatusBarPaddingLeft = 0;
    private int mStatusBarPaddingRight = 0;
    private int mStatusBarPaddingTop = 0;

    private static final String TAG = "PhoneStatusBarView";
    private final CommandQueue mCommandQueue;
    private final StatusBarWindowControllerStore mStatusBarWindowControllerStore;

    private int mRotationOrientation = -1;
    private RotationButtonController mRotationButtonController;
    @Nullable
    private View mCutoutSpace;
    @Nullable
    private DisplayCutout mDisplayCutout;
    @Nullable
    private Rect mDisplaySize;
    private int mStatusBarHeight;
    @Nullable
    private Gefingerpoken mTouchEventHandler;
    @Nullable
    private HasCornerCutoutFetcher mHasCornerCutoutFetcher;
    @Nullable
    private InsetsFetcher mInsetsFetcher;
    private int mDensity;
    private float mFontScale;
    private StatusBarLongPressGestureDetector mStatusBarLongPressGestureDetector;

    @Nullable
    private ViewGroup mStatusBarContents = null;

    /**
     * Draw this many pixels into the left/right side of the cutout to optimally use the space
     */
    private int mCutoutSideNudge = 0;

    private boolean mBrightnessControlEnabled;

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCommandQueue = Dependency.get(CommandQueue.class);
        mStatusBarWindowControllerStore = Dependency.get(StatusBarWindowControllerStore.class);

        mTunerService = Dependency.get(TunerService.class);

        // Only create FRB here if there is no navbar
        if (!hasNavigationBar()) {
            final Context lightContext = new ContextThemeWrapper(context,
                    Utils.getThemeAttr(context, R.attr.lightIconTheme));
            final Context darkContext = new ContextThemeWrapper(context,
                    Utils.getThemeAttr(context, R.attr.darkIconTheme));
            final int lightIconColor =
                    Utils.getColorAttrDefaultColor(lightContext, R.attr.singleToneColor);
            final int darkIconColor =
                    Utils.getColorAttrDefaultColor(darkContext, R.attr.singleToneColor);
            final FloatingRotationButton floatingRotationButton = new FloatingRotationButton(
                    context,
                    R.string.accessibility_rotate_button, R.layout.rotate_suggestion,
                    R.id.rotate_suggestion, R.dimen.floating_rotation_button_min_margin,
                    R.dimen.rounded_corner_content_padding,
                    R.dimen.floating_rotation_button_taskbar_left_margin,
                    R.dimen.floating_rotation_button_taskbar_bottom_margin,
                    R.dimen.floating_rotation_button_diameter, R.dimen.key_button_ripple_max_width,
                    R.bool.floating_rotation_button_position_left);

            mRotationButtonController = new RotationButtonController(lightContext, lightIconColor,
                    darkIconColor, R.drawable.ic_sysbar_rotate_button_ccw_start_0,
                    R.drawable.ic_sysbar_rotate_button_ccw_start_90,
                    R.drawable.ic_sysbar_rotate_button_cw_start_0,
                    R.drawable.ic_sysbar_rotate_button_cw_start_90,
                    () -> getDisplay().getRotation());
            mRotationButtonController.setRotationButton(floatingRotationButton, null);
        }
    }

    @Override
    public void onRotationProposal(final int rotation, boolean isValid) {
        if (mRotationButtonController != null && !hasNavigationBar()) {
            mRotationButtonController.onRotationProposal(rotation, isValid);
        }
    }

    private boolean hasNavigationBar() {
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            return windowManager.hasNavigationBar(Display.DEFAULT_DISPLAY);
        } catch (RemoteException ex) { }
        return false;
    }

    void setLongPressGestureDetector(
            StatusBarLongPressGestureDetector statusBarLongPressGestureDetector) {
        if (ShadeExpandsOnStatusBarLongPress.isEnabled()) {
            mStatusBarLongPressGestureDetector = statusBarLongPressGestureDetector;
        }
    }

    void setTouchEventHandler(Gefingerpoken handler) {
        mTouchEventHandler = handler;
    }

    void setHasCornerCutoutFetcher(@NonNull HasCornerCutoutFetcher cornerCutoutFetcher) {
        mHasCornerCutoutFetcher = cornerCutoutFetcher;
        updateCutoutLocation();
    }

    void setInsetsFetcher(@NonNull InsetsFetcher insetsFetcher) {
        mInsetsFetcher = insetsFetcher;
        updateSafeInsets();
    }

    void init(StatusBarUserChipViewModel viewModel) {
        StatusBarUserSwitcherContainer container = findViewById(R.id.user_switcher_container);
        StatusBarUserChipViewBinder.bind(container, viewModel);
    }

    public void offsetStatusBar(Offset offset) {
        if (mStatusBarContents == null) {
            return;
        }
        mStatusBarContents.setTranslationX(offset.getX());
        mStatusBarContents.setTranslationY(offset.getY());
        invalidate();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mCutoutSpace = findViewById(R.id.cutout_space_view);
        mStatusBarContents = (ViewGroup) findViewById(R.id.status_bar_contents);

        updateResources();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTunerService.addTunable(this, STATUSBAR_LEFT_PADDING, STATUSBAR_RIGHT_PADDING, STATUSBAR_TOP_PADDING);
        if (updateDisplayParameters()) {
            updateLayoutForCutout();
            updateWindowHeight();
        }

        if (mRotationButtonController != null && !hasNavigationBar()) {
            mCommandQueue.addCallback(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mTunerService.removeTunable(this);
        mDisplayCutout = null;

        if (mRotationButtonController != null) {
            mCommandQueue.removeCallback(this);
        }
    }
    
    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case STATUSBAR_LEFT_PADDING:
                mStatusBarPaddingLeft = convertToDip(TunerService.parseInteger(newValue, 
                    getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_padding_start)));
                updateResources();
                break;
            case STATUSBAR_RIGHT_PADDING:
                mStatusBarPaddingRight = convertToDip(TunerService.parseInteger(newValue, 
                    getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_padding_end)));
                updateResources();
                break;
            case STATUSBAR_TOP_PADDING:
                mStatusBarPaddingTop = convertToDip(TunerService.parseInteger(newValue, 
                    getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_padding_top)));
                updateResources();
                break;
            default:
                break;
         }
    }

    private int convertToDip(int padding) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                padding,
                getResources().getDisplayMetrics()));
    }

    // Per b/300629388, we let the PhoneStatusBarView detect onConfigurationChanged to
    // updateResources, instead of letting the PhoneStatusBarViewController detect onConfigChanged
    // then notify PhoneStatusBarView.
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();

        // May trigger cutout space layout-ing
        if (updateDisplayParameters()) {
            updateLayoutForCutout();
            requestLayout();
        }
        updateWindowHeight();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (updateDisplayParameters()) {
            updateLayoutForCutout();
            requestLayout();
        }
        return super.onApplyWindowInsets(insets);
    }

    /**
     * @return boolean indicating if we need to update the cutout location / margins
     */
    private boolean updateDisplayParameters() {
        boolean changed = false;
        int newRotation = RotationUtils.getExactRotation(mContext);
        if (newRotation != mRotationOrientation) {
            changed = true;
            mRotationOrientation = newRotation;
        }

        if (!Objects.equals(getRootWindowInsets().getDisplayCutout(), mDisplayCutout)) {
            changed = true;
            mDisplayCutout = getRootWindowInsets().getDisplayCutout();
        }

        Configuration newConfiguration = mContext.getResources().getConfiguration();
        final Rect newSize = newConfiguration.windowConfiguration.getMaxBounds();
        if (!Objects.equals(newSize, mDisplaySize)) {
            changed = true;
            mDisplaySize = newSize;
        }

        int density = newConfiguration.densityDpi;
        if (density != mDensity) {
            changed = true;
            mDensity = density;
        }
        float fontScale = newConfiguration.fontScale;
        if (fontScale != mFontScale) {
            changed = true;
            mFontScale = fontScale;
        }
        return changed;
    }

    @Override
    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEventInternal(child, event)) {
            // The status bar is very small so augment the view that the user is touching
            // with the content of the status bar a whole. This way an accessibility service
            // may announce the current item as well as the entire content if appropriate.
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ShadeExpandsOnStatusBarLongPress.isEnabled()
                && mStatusBarLongPressGestureDetector != null) {
            mStatusBarLongPressGestureDetector.handleTouch(event);
        }
        if (mTouchEventHandler == null) {
            Log.w(
                    TAG,
                    String.format(
                            "onTouch: No touch handler provided; eating gesture at (%d,%d)",
                            (int) event.getX(),
                            (int) event.getY()
                    )
            );
            return true;
        }
        return mTouchEventHandler.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (Flags.statusBarSwipeOverChip()) {
            return mTouchEventHandler.onInterceptTouchEvent(event);
        } else {
            mTouchEventHandler.onInterceptTouchEvent(event);
            return super.onInterceptTouchEvent(event);
        }
    }

    @Override
    public void setImeWindowStatus(int displayId, int vis, int backDisposition,
            boolean showImeSwitcher) {
        if (mRotationButtonController != null) {
            final boolean imeShown = (vis & InputMethodService.IME_VISIBLE) != 0;
            mRotationButtonController.getRotationButton().setCanShowRotationButton(!imeShown);
        }
    }

    public boolean getBrightnessControlEnabled() {
        return mBrightnessControlEnabled;
    }

    public void setBrightnessControlEnabled(boolean enabled) {
        mBrightnessControlEnabled = enabled;
    }

    public void updateResources() {
        mCutoutSideNudge = getResources().getDimensionPixelSize(
                R.dimen.display_cutout_margin_consumption);

        updateStatusBarHeight();
    }

    private void updateStatusBarHeight() {
        final int waterfallTopInset =
                mDisplayCutout == null ? 0 : mDisplayCutout.getWaterfallInsets().top;
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        mStatusBarHeight = SystemBarUtils.getStatusBarHeight(mContext);
        layoutParams.height = mStatusBarHeight - waterfallTopInset;
        updateSystemIconsContainerHeight();
        updatePaddings();
        setLayoutParams(layoutParams);
    }

    private void updateSystemIconsContainerHeight() {
        View systemIconsContainer = findViewById(R.id.system_icons);
        ViewGroup.LayoutParams layoutParams = systemIconsContainer.getLayoutParams();
        int newSystemIconsHeight =
                getResources().getDimensionPixelSize(R.dimen.status_bar_system_icons_height);
        if (layoutParams.height != newSystemIconsHeight) {
            layoutParams.height = newSystemIconsHeight;
            systemIconsContainer.setLayoutParams(layoutParams);
        }
    }

    private void updatePaddings() {
        mStatusBarContents.setPaddingRelative(
                mStatusBarPaddingLeft,
                mStatusBarPaddingTop,
                mStatusBarPaddingRight,
                0);

        findViewById(R.id.notification_lights_out)
                .setPaddingRelative(0, mStatusBarPaddingLeft, 0, 0);

        findViewById(R.id.system_icons).setPaddingRelative(
                getResources().getDimensionPixelSize(R.dimen.status_bar_icons_padding_start),
                getResources().getDimensionPixelSize(R.dimen.status_bar_icons_padding_top),
                getResources().getDimensionPixelSize(R.dimen.status_bar_icons_padding_end),
                getResources().getDimensionPixelSize(R.dimen.status_bar_icons_padding_bottom)
        );
    }

    private void updateLayoutForCutout() {
        updateStatusBarHeight();
        updateCutoutLocation();
        updateSafeInsets();
    }

    private void updateCutoutLocation() {
        // Not all layouts have a cutout (e.g., Car)
        if (mCutoutSpace == null) {
            return;
        }

        boolean hasCornerCutout;
        if (mHasCornerCutoutFetcher != null) {
            hasCornerCutout = mHasCornerCutoutFetcher.fetchHasCornerCutout();
        } else {
            Log.e(TAG, "mHasCornerCutoutFetcher unexpectedly null");
            hasCornerCutout = true;
        }

        if (mDisplayCutout == null || mDisplayCutout.isEmpty() || hasCornerCutout) {
            mCutoutSpace.setVisibility(View.GONE);
            return;
        }

        mCutoutSpace.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mCutoutSpace.getLayoutParams();

        Rect bounds = mDisplayCutout.getBoundingRectTop();

        bounds.left = bounds.left + mCutoutSideNudge;
        bounds.right = bounds.right - mCutoutSideNudge;
        lp.width = bounds.width();
        lp.height = bounds.height();
    }

    private void updateSafeInsets() {
        if (mInsetsFetcher == null) {
            Log.e(TAG, "mInsetsFetcher unexpectedly null");
            return;
        }

        Insets insets  = mInsetsFetcher.fetchInsets();
        setPadding(
                insets.left,
                insets.top,
                insets.right,
                getPaddingBottom());

        // Apply negative paddings to centered area layout so that we'll actually be on the center.
        Display display = getDisplay();
        final int winRotation = display != null ? display.getRotation() : Surface.ROTATION_0;
        LayoutParams centeredAreaParams =
                (LayoutParams) findViewById(R.id.centered_area).getLayoutParams();
        centeredAreaParams.leftMargin =
                winRotation == Surface.ROTATION_0 ? -insets.left : 0;
        centeredAreaParams.rightMargin =
                winRotation == Surface.ROTATION_0 ? -insets.right : 0;
    }

    private void updateWindowHeight() {
        if (Flags.statusBarStopUpdatingWindowHeight()) {
            return;
        }
        mStatusBarWindowControllerStore.getDefaultDisplay().refreshStatusBarHeight();
    }

    interface HasCornerCutoutFetcher {
        boolean fetchHasCornerCutout();
    }

    interface InsetsFetcher {
        Insets fetchInsets();
    }
}
