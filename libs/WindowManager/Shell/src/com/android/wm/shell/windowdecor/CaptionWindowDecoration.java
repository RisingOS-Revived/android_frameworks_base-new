/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.wm.shell.windowdecor;

import static android.window.DesktopModeFlags.ENABLE_WINDOWING_SCALED_RESIZING;

import static com.android.wm.shell.windowdecor.DragResizeWindowGeometry.getFineResizeCornerSize;
import static com.android.wm.shell.windowdecor.DragResizeWindowGeometry.getLargeResizeCornerSize;
import static com.android.wm.shell.windowdecor.DragResizeWindowGeometry.getResizeEdgeHandleSize;
import static com.android.wm.shell.windowdecor.DragResizeWindowGeometry.getResizeHandleEdgeInset;

import static com.android.launcher3.icons.BaseIconFactory.MODE_DEFAULT;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.WindowConfiguration;
import android.app.WindowConfiguration.WindowingMode;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Choreographer;
import android.view.Display;
import android.view.InsetsState;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.window.WindowContainerTransaction;

import com.android.internal.policy.ScreenDecorationsUtils;

import com.android.launcher3.icons.BaseIconFactory;
import com.android.launcher3.icons.IconProvider;

import com.android.internal.annotations.VisibleForTesting;
import com.android.wm.shell.R;
import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.common.DisplayController;
import com.android.wm.shell.common.DisplayLayout;
import com.android.wm.shell.common.ShellExecutor;
import com.android.wm.shell.common.SyncTransactionQueue;
import com.android.wm.shell.shared.annotations.ShellBackgroundThread;
import com.android.wm.shell.windowdecor.common.viewhost.WindowDecorViewHost;
import com.android.wm.shell.windowdecor.common.viewhost.WindowDecorViewHostSupplier;
import com.android.wm.shell.windowdecor.extension.TaskInfoKt;

/**
 * Defines visuals and behaviors of a window decoration of a caption bar and shadows. It works with
 * {@link CaptionWindowDecorViewModel}. The caption bar contains a back button, minimize button,
 * maximize button and close button.
 */
public class CaptionWindowDecoration extends WindowDecoration<WindowDecorLinearLayout> {
    private static final String TAG = "CaptionWindowDecoration";

    private final Handler mHandler;
    private final @ShellBackgroundThread ShellExecutor mBgExecutor;
    private final Choreographer mChoreographer;
    private final SyncTransactionQueue mSyncQueue;

    private View.OnClickListener mOnCaptionButtonClickListener;
    private View.OnTouchListener mOnCaptionTouchListener;
    private DragPositioningCallback mDragPositioningCallback;
    private DragResizeInputListener mDragResizeListener;

    private RelayoutParams mRelayoutParams = new RelayoutParams();
    private final RelayoutResult<WindowDecorLinearLayout> mResult =
            new RelayoutResult<>();

    private ResizeVeil mResizeVeil;
    private Bitmap mResizeVeilBitmap;

    CaptionWindowDecoration(
            Context context,
            @NonNull Context userContext,
            DisplayController displayController,
            ShellTaskOrganizer taskOrganizer,
            RunningTaskInfo taskInfo,
            SurfaceControl taskSurface,
            Handler handler,
            @ShellBackgroundThread ShellExecutor bgExecutor,
            Choreographer choreographer,
            SyncTransactionQueue syncQueue,
            @NonNull WindowDecorViewHostSupplier<WindowDecorViewHost> windowDecorViewHostSupplier) {
        super(context, userContext, displayController, taskOrganizer, taskInfo,
                taskSurface, windowDecorViewHostSupplier);
        mHandler = handler;
        mBgExecutor = bgExecutor;
        mChoreographer = choreographer;
        mSyncQueue = syncQueue;
        
        loadAppInfo();
    }

    void setCaptionListeners(
            View.OnClickListener onCaptionButtonClickListener,
            View.OnTouchListener onCaptionTouchListener) {
        mOnCaptionButtonClickListener = onCaptionButtonClickListener;
        mOnCaptionTouchListener = onCaptionTouchListener;
    }

    void setDragPositioningCallback(DragPositioningCallback dragPositioningCallback) {
        mDragPositioningCallback = dragPositioningCallback;
    }

    @Override
    @NonNull
    Rect calculateValidDragArea() {
        final Context displayContext = mDisplayController.getDisplayContext(mTaskInfo.displayId);
        if (displayContext == null) return new Rect();
        final int leftButtonsWidth = loadDimensionPixelSize(mContext.getResources(),
                R.dimen.caption_left_buttons_width);

        // On a smaller screen, don't require as much empty space on screen, as offscreen
        // drags will be restricted too much.
        final int requiredEmptySpaceId = displayContext
                .getResources().getConfiguration().smallestScreenWidthDp >= 600
                ? R.dimen.freeform_required_visible_empty_space_in_header :
                R.dimen.small_screen_required_visible_empty_space_in_header;
        final int requiredEmptySpace = loadDimensionPixelSize(mContext.getResources(),
                requiredEmptySpaceId);

        final int rightButtonsWidth = loadDimensionPixelSize(mContext.getResources(),
                R.dimen.caption_right_buttons_width);
        final int taskWidth = mTaskInfo.configuration.windowConfiguration.getBounds().width();
        final DisplayLayout layout = mDisplayController.getDisplayLayout(mTaskInfo.displayId);
        final int displayWidth = layout.width();
        final Rect stableBounds = new Rect();
        layout.getStableBounds(stableBounds);
        return new Rect(
                determineMinX(leftButtonsWidth, rightButtonsWidth, requiredEmptySpace,
                        taskWidth),
                stableBounds.top,
                determineMaxX(leftButtonsWidth, rightButtonsWidth, requiredEmptySpace, taskWidth,
                        displayWidth),
                determineMaxY(requiredEmptySpace, stableBounds));
    }


    /**
     * Determine the lowest x coordinate of a freeform task. Used for restricting drag inputs.
     */
    private int determineMinX(int leftButtonsWidth, int rightButtonsWidth, int requiredEmptySpace,
            int taskWidth) {
        // Do not let apps with < 48dp empty header space go off the left edge at all.
        if (leftButtonsWidth + rightButtonsWidth + requiredEmptySpace > taskWidth) {
            return 0;
        }
        return -taskWidth + requiredEmptySpace + rightButtonsWidth;
    }

    /**
     * Determine the highest x coordinate of a freeform task. Used for restricting drag inputs.
     */
    private int determineMaxX(int leftButtonsWidth, int rightButtonsWidth, int requiredEmptySpace,
            int taskWidth, int displayWidth) {
        // Do not let apps with < 48dp empty header space go off the right edge at all.
        if (leftButtonsWidth + rightButtonsWidth + requiredEmptySpace > taskWidth) {
            return displayWidth - taskWidth;
        }
        return displayWidth - requiredEmptySpace - leftButtonsWidth;
    }

    /**
     * Determine the highest y coordinate of a freeform task. Used for restricting drag inputs.
     */
    private int determineMaxY(int requiredEmptySpace, Rect stableBounds) {
        return stableBounds.bottom - requiredEmptySpace;
    }

    @Override
    void relayout(RunningTaskInfo taskInfo, boolean hasGlobalFocus,
            @NonNull Region displayExclusionRegion) {
        final SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        // The crop and position of the task should only be set when a task is fluid resizing. In
        // all other cases, it is expected that the transition handler positions and crops the task
        // in order to allow the handler time to animate before the task before the final
        // position and crop are set.
        final boolean shouldSetTaskVisibilityPositionAndCrop =
                mTaskDragResizer.isResizingOrAnimating();
        // Use |applyStartTransactionOnDraw| so that the transaction (that applies task crop) is
        // synced with the buffer transaction (that draws the View). Both will be shown on screen
        // at the same, whereas applying them independently causes flickering. See b/270202228.
        relayout(taskInfo, t, t, true /* applyStartTransactionOnDraw */,
                shouldSetTaskVisibilityPositionAndCrop, hasGlobalFocus, displayExclusionRegion);
    }

    @VisibleForTesting
    static void updateRelayoutParams(
            RelayoutParams relayoutParams,
            @NonNull Context context,
            ActivityManager.RunningTaskInfo taskInfo,
            boolean applyStartTransactionOnDraw,
            boolean shouldSetTaskVisibilityPositionAndCrop,
            boolean isStatusBarVisible,
            boolean isKeyguardVisibleAndOccluded,
            DisplayController displayController,
            boolean hasGlobalFocus,
            @NonNull Region globalExclusionRegion) {
        relayoutParams.reset();
        relayoutParams.mRunningTaskInfo = taskInfo;
        relayoutParams.mLayoutResId = R.layout.caption_window_decor;
        relayoutParams.mCaptionHeightId = getCaptionHeightIdStatic(taskInfo.getWindowingMode());
        relayoutParams.mShadowRadius = hasGlobalFocus
                ? context.getResources().getDimensionPixelSize(
                        R.dimen.freeform_decor_shadow_focused_thickness)
                : context.getResources().getDimensionPixelSize(
                        R.dimen.freeform_decor_shadow_unfocused_thickness);
        relayoutParams.mApplyStartTransactionOnDraw = applyStartTransactionOnDraw;
        relayoutParams.mSetTaskVisibilityPositionAndCrop = shouldSetTaskVisibilityPositionAndCrop;
        relayoutParams.mIsCaptionVisible = taskInfo.isFreeform()
                || (isStatusBarVisible && !isKeyguardVisibleAndOccluded);
        relayoutParams.mDisplayExclusionRegion.set(globalExclusionRegion);
        relayoutParams.mCornerRadius =
                getCornerRadius(context, displayController.getDisplay(taskInfo.displayId));

        if (TaskInfoKt.isTransparentCaptionBarAppearance(taskInfo)) {
            // If the app is requesting to customize the caption bar, allow input to fall
            // through to the windows below so that the app can respond to input events on
            // their custom content.
            relayoutParams.mInputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_SPY;
        }
        final RelayoutParams.OccludingCaptionElement backButtonElement =
                new RelayoutParams.OccludingCaptionElement();
        backButtonElement.mWidthResId = R.dimen.caption_left_buttons_width;
        backButtonElement.mAlignment = RelayoutParams.OccludingCaptionElement.Alignment.START;
        relayoutParams.mOccludingCaptionElements.add(backButtonElement);
        // Then, the right-aligned section (minimize, maximize and close buttons).
        final RelayoutParams.OccludingCaptionElement controlsElement =
                new RelayoutParams.OccludingCaptionElement();
        controlsElement.mWidthResId = R.dimen.caption_right_buttons_width;
        controlsElement.mAlignment = RelayoutParams.OccludingCaptionElement.Alignment.END;
        relayoutParams.mOccludingCaptionElements.add(controlsElement);
        relayoutParams.mCaptionTopPadding = getTopPadding(relayoutParams,
                taskInfo.getConfiguration().windowConfiguration.getBounds(),
                displayController.getInsetsState(taskInfo.displayId));
    }

    @SuppressLint("MissingPermission")
    void relayout(RunningTaskInfo taskInfo,
            SurfaceControl.Transaction startT, SurfaceControl.Transaction finishT,
            boolean applyStartTransactionOnDraw, boolean shouldSetTaskVisibilityPositionAndCrop,
            boolean hasGlobalFocus,
            @NonNull Region globalExclusionRegion) {
        final boolean isFreeform =
                taskInfo.getWindowingMode() == WindowConfiguration.WINDOWING_MODE_FREEFORM;
        final boolean isDragResizeable = ENABLE_WINDOWING_SCALED_RESIZING.isTrue()
                ? isFreeform : isFreeform && taskInfo.isResizeable;

        final WindowDecorLinearLayout oldRootView = mResult.mRootView;
        final SurfaceControl oldDecorationSurface = mDecorationContainerSurface;
        final WindowContainerTransaction wct = new WindowContainerTransaction();

        updateRelayoutParams(mRelayoutParams, mContext, taskInfo, applyStartTransactionOnDraw,
                shouldSetTaskVisibilityPositionAndCrop, mIsStatusBarVisible,
                mIsKeyguardVisibleAndOccluded,
                mDisplayController, hasGlobalFocus,
                globalExclusionRegion);

        relayout(mRelayoutParams, startT, finishT, wct, oldRootView, mResult);
        // After this line, mTaskInfo is up-to-date and should be used instead of taskInfo

        mBgExecutor.execute(() -> mTaskOrganizer.applyTransaction(wct));

        if (mResult.mRootView == null) {
            // This means something blocks the window decor from showing, e.g. the task is hidden.
            // Nothing is set up in this case including the decoration surface.
            return;
        }
        if (oldRootView != mResult.mRootView) {
            setupRootView();
        }

        bindData(mResult.mRootView, taskInfo);

        if (!isDragResizeable) {
            closeDragResizeListener();
            return;
        }

        if (oldDecorationSurface != mDecorationContainerSurface || mDragResizeListener == null) {
            closeDragResizeListener();
            mDragResizeListener = new DragResizeInputListener(
                    mContext,
                    mTaskInfo,
                    mHandler,
                    mChoreographer,
                    mDisplay.getDisplayId(),
                    mDecorationContainerSurface,
                    mDragPositioningCallback,
                    mSurfaceControlBuilderSupplier,
                    mSurfaceControlTransactionSupplier,
                    mDisplayController);
        }

        final int touchSlop = ViewConfiguration.get(mResult.mRootView.getContext())
                .getScaledTouchSlop();

        final Resources res = mResult.mRootView.getResources();
        mDragResizeListener.setGeometry(new DragResizeWindowGeometry(0 /* taskCornerRadius */,
                        new Size(mResult.mWidth, mResult.mHeight),
                        getResizeEdgeHandleSize(res),
                        getResizeHandleEdgeInset(res), getFineResizeCornerSize(res),
                        getLargeResizeCornerSize(res), DragResizeWindowGeometry.DisabledEdge.NONE),
                touchSlop);
    }

    private static int getCornerRadius(Context context, Display display) {
        // Show rounded corners only on the internal display as we can't get rounded corners for
        // external displays.
        if (display.getType() != Display.TYPE_INTERNAL) {
            return 0;
        }
        final TypedArray ta = context.obtainStyledAttributes(
                new int[]{android.R.attr.dialogCornerRadius});
        final int cornerRadius = ta.getDimensionPixelSize(0, 0);
        ta.recycle();
        return cornerRadius;
    }

    /**
     * Sets up listeners when a new root view is created.
     */
    private void setupRootView() {
        final View caption = mResult.mRootView.findViewById(R.id.caption);
        caption.setOnTouchListener(mOnCaptionTouchListener);
        final View close = caption.findViewById(R.id.close_window);
        close.setOnClickListener(mOnCaptionButtonClickListener);
        final View back = caption.findViewById(R.id.back_button);
        back.setOnClickListener(mOnCaptionButtonClickListener);
        final View minimize = caption.findViewById(R.id.minimize_window);
        minimize.setOnClickListener(mOnCaptionButtonClickListener);
        final View maximize = caption.findViewById(R.id.maximize_window);
        maximize.setOnClickListener(mOnCaptionButtonClickListener);
    }

    private void bindData(View rootView, RunningTaskInfo taskInfo) {
        // Set up the tint first so that the drawable can be stylized when loaded.
        setupCaptionColor(taskInfo);

        final boolean isFullscreen =
                taskInfo.getWindowingMode() == WindowConfiguration.WINDOWING_MODE_FULLSCREEN;
        rootView.findViewById(R.id.maximize_window)
                .setBackgroundResource(isFullscreen ? R.drawable.decor_restore_button_dark
                        : R.drawable.decor_maximize_button_dark);
    }

    private void setupCaptionColor(RunningTaskInfo taskInfo) {
        if (TaskInfoKt.isTransparentCaptionBarAppearance(taskInfo)) {
            setCaptionColor(Color.TRANSPARENT);
        } else {
            final int statusBarColor = taskInfo.taskDescription.getStatusBarColor();
            setCaptionColor(statusBarColor);
        }
    }

    private void setCaptionColor(int captionColor) {
        if (mResult.mRootView == null) {
            return;
        }
        int nightModeFlags = mContext.getResources().getConfiguration().uiMode & 
                             android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isNightMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int mCaptionColor = isNightMode ? Color.BLACK : Color.WHITE;
        int buttonTintColorRes = isNightMode ? R.color.decor_button_light_color : R.color.decor_button_dark_color;
        ColorStateList buttonTintColor = mContext.getResources().getColorStateList(buttonTintColorRes, null /* theme */);

        final View caption = mResult.mRootView.findViewById(R.id.caption);
        final GradientDrawable captionDrawable = (GradientDrawable) caption.getBackground();
        captionDrawable.setColor(mCaptionColor);

        final View back = caption.findViewById(R.id.back_button);
        back.setBackgroundTintList(buttonTintColor);

        final View minimize = caption.findViewById(R.id.minimize_window);
        minimize.setBackgroundTintList(buttonTintColor);

        final View maximize = caption.findViewById(R.id.maximize_window);
        maximize.setBackgroundTintList(buttonTintColor);

        final View close = caption.findViewById(R.id.close_window);
        close.setBackgroundTintList(buttonTintColor);
    }

    boolean isHandlingDragResize() {
        return mDragResizeListener != null && mDragResizeListener.isHandlingDragResize();
    }

    private void closeDragResizeListener() {
        if (mDragResizeListener == null) {
            return;
        }
        mDragResizeListener.close();
        mDragResizeListener = null;
    }

    private void loadAppInfo() {
        String packageName = mTaskInfo.realActivity.getPackageName();
        PackageManager pm = mContext.getApplicationContext().getPackageManager();
        try {
            IconProvider provider = new IconProvider(mContext);
            Drawable appIcon = provider.getIcon(pm.getActivityInfo(mTaskInfo.baseActivity,
                    PackageManager.ComponentInfoFlags.of(0)));
            final BaseIconFactory resizeVeilIconFactory = createIconFactory(mContext,
                    R.dimen.desktop_mode_resize_veil_icon_size);
            mResizeVeilBitmap = resizeVeilIconFactory
                    .createScaledBitmap(appIcon, MODE_DEFAULT);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Package not found: " + packageName, e);
        }
    }

    private BaseIconFactory createIconFactory(Context context, int dimensions) {
        final Resources resources = context.getResources();
        final int densityDpi = resources.getDisplayMetrics().densityDpi;
        final int iconSize = resources.getDimensionPixelSize(dimensions);
        return new BaseIconFactory(context, densityDpi, iconSize);
    }

    /**
     * Create the resize veil for this task. Note the veil's visibility is View.GONE by default
     * until a resize event calls showResizeVeil below.
     */
    void createResizeVeil() {
        mResizeVeil = new ResizeVeil(mContext, mDisplayController, mResizeVeilBitmap,
                mTaskSurface, mSurfaceControlTransactionSupplier, mTaskInfo);
    }

    /**
     * Fade in the resize veil
     */
    void showResizeVeil(Rect taskBounds) {
        mResizeVeil.showVeil(mTaskSurface, taskBounds, mTaskInfo);
    }

    /**
     * Set new bounds for the resize veil
     */
    void updateResizeVeil(Rect newBounds) {
        mResizeVeil.updateResizeVeil(newBounds);
    }

    /**
     * Fade the resize veil out.
     */
    void hideResizeVeil() {
        mResizeVeil.hideVeil();
    }

    private void disposeResizeVeil() {
        if (mResizeVeil == null) return;
        mResizeVeil.dispose();
        mResizeVeil = null;
    }

    private static int getTopPadding(RelayoutParams params, Rect taskBounds,
            InsetsState insetsState) {
        if (!params.mRunningTaskInfo.isFreeform()) {
            Insets systemDecor = insetsState.calculateInsets(taskBounds,
                    WindowInsets.Type.systemBars() & ~WindowInsets.Type.captionBar(),
                    false /* ignoreVisibility */);
            return systemDecor.top;
        } else {
            return 0;
        }
    }

    /**
     * Checks whether the touch event falls inside the customizable caption region.
     */
    boolean checkTouchEventInCustomizableRegion(MotionEvent ev) {
        return mResult.mCustomizableCaptionRegion.contains((int) ev.getRawX(), (int) ev.getRawY());
    }

    boolean shouldResizeListenerHandleEvent(@NonNull MotionEvent e, @NonNull Point offset) {
        return mDragResizeListener != null && mDragResizeListener.shouldHandleEvent(e, offset);
    }

    @Override
    public void close() {
        closeDragResizeListener();
        disposeResizeVeil();
        super.close();
    }

    @Override
    int getCaptionHeightId(@WindowingMode int windowingMode) {
        return getCaptionHeightIdStatic(windowingMode);
    }

    private static int getCaptionHeightIdStatic(@WindowingMode int windowingMode) {
        return R.dimen.freeform_decor_caption_height;
    }

    @Override
    int getCaptionViewId() {
        return R.id.caption;
    }
}
