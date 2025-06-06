/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.systemui.qs;

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;

import com.android.internal.logging.UiEventLogger;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.res.R;
import com.android.systemui.qs.TileUtils;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.tuner.TunerService;

/**
 * Version of QSPanel that only shows N Quick Tiles in the QS Header.
 */
public class QuickQSPanel extends QSPanel implements TunerService.Tunable {

    private static final String TAG = "QuickQSPanel";
    // A fallback value for max tiles number when setting via Tuner (parseNumTiles)
    public static final int TUNER_MAX_TILES_FALLBACK = 6;

    private boolean mDisabledByPolicy;
    private int mMaxTiles;

    public QuickQSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMaxTiles();
    }

    @Override
    protected void setHorizontalContentContainerClipping() {
        mHorizontalContentContainer.setClipToPadding(false);
        mHorizontalContentContainer.setClipChildren(false);
    }

    View getBrightnessView() {
        return mBrightnessView;
    }

    @Override
    protected void setBrightnessViewMargin(boolean top) {
        if (mBrightnessView != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mBrightnessView.getLayoutParams();
            if (top) {
                lp.topMargin = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.qqs_top_brightness_margin_top);
                lp.bottomMargin = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.qqs_top_brightness_margin_bottom);
            } else {
                lp.topMargin = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.qqs_bottom_brightness_margin_top);
                lp.bottomMargin = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.qqs_bottom_brightness_margin_bottom);
            }
            mBrightnessView.setLayoutParams(lp);
        }
    }

    @Override
    void initialize(QSLogger qsLogger, boolean usingMediaPlayer) {
        super.initialize(qsLogger, usingMediaPlayer);
        if (mHorizontalContentContainer != null) {
            mHorizontalContentContainer.setClipChildren(false);
        }
    }

    @Override
    public TileLayout getOrCreateTileLayout() {
        QQSSideLabelTileLayout layout = new QQSSideLabelTileLayout(mContext);
        layout.setId(R.id.qqs_tile_layout);
        return layout;
    }


    @Override
    protected boolean displayMediaMarginsOnMedia() {
        // Margins should be on the container to visually center the view
        return false;
    }

    @Override
    protected boolean mediaNeedsTopMargin() {
        boolean isA11Style = TileUtils.getQsUiStyle(mContext) != 0;
        boolean isCompactPlayerEnabled = TileUtils.isCompactQSMediaPlayerEnforced(mContext);
        return !isCompactPlayerEnabled || isCompactPlayerEnabled && !isA11Style;
    }

    @Override
    protected void updatePadding() {
        int bottomPadding = getResources().getDimensionPixelSize(R.dimen.qqs_layout_padding_bottom);
        setPaddingRelative(getPaddingStart(),
                getPaddingTop(),
                getPaddingEnd(),
                bottomPadding);
    }

    @Override
    protected String getDumpableTag() {
        return TAG;
    }

    @Override
    protected boolean shouldShowDetail() {
        return !mExpanded;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        setMaxTiles();
        super.onConfigurationChanged(newConfig);
    }

    public void setMaxTiles() {
        int columns = TileUtils.getQSColumnsCount(mContext);
        int maxTiles = columns * TileUtils.getQQSRowsCount(mContext);

        while (maxTiles > columns && (maxTiles % columns != 0)) {
            maxTiles--;
        }

        if (mMaxTiles != maxTiles) {
            mMaxTiles = maxTiles;
            requestLayout();
        }
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case QS_SHOW_BRIGHTNESS_SLIDER:
                boolean value =
                        TunerService.parseInteger(newValue, 1) > 1;
                super.onTuningChanged(key, value ? newValue : "0");
                break;
            case QS_LAYOUT_COLUMNS:
            case QS_LAYOUT_COLUMNS_LANDSCAPE:
            case QQS_LAYOUT_ROWS:
            case QQS_LAYOUT_ROWS_LANDSCAPE:
                setMaxTiles();
                super.onTuningChanged(key, newValue);
                break;
            default:
                super.onTuningChanged(key, newValue);
         }
    }

    public int getNumQuickTiles() {
        setMaxTiles();
        return mMaxTiles;
    }

    /**
     * Parses the String setting into the number of tiles. Defaults to
     * {@link #TUNER_MAX_TILES_FALLBACK}
     *
     * @param numTilesValue value of the setting to parse
     * @return parsed value of numTilesValue OR {@link #TUNER_MAX_TILES_FALLBACK} on error
     */
    public static int parseNumTiles(String numTilesValue) {
        try {
            return Integer.parseInt(numTilesValue);
        } catch (NumberFormatException e) {
            // Couldn't read an int from the new setting value. Use default.
            return TUNER_MAX_TILES_FALLBACK;
        }
    }

    void setDisabledByPolicy(boolean disabled) {
        if (disabled != mDisabledByPolicy) {
            mDisabledByPolicy = disabled;
            setVisibility(disabled ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Sets the visibility of this {@link QuickQSPanel}. This method has no effect when this panel
     * is disabled by policy through {@link #setDisabledByPolicy(boolean)}, and in this case the
     * visibility will always be {@link View#GONE}. This method is called externally by
     * {@link QSAnimator} only.
     */
    @Override
    public void setVisibility(int visibility) {
        if (mDisabledByPolicy) {
            if (getVisibility() == View.GONE) {
                return;
            }
            visibility = View.GONE;
        }
        super.setVisibility(visibility);
    }

    @Override
    protected QSEvent openPanelEvent() {
        return QSEvent.QQS_PANEL_EXPANDED;
    }

    @Override
    protected QSEvent closePanelEvent() {
        return QSEvent.QQS_PANEL_COLLAPSED;
    }

    @Override
    protected QSEvent tileVisibleEvent() {
        return QSEvent.QQS_TILE_VISIBLE;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        // Remove the collapse action from QSPanel
        info.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
    }

    static class QQSSideLabelTileLayout extends SideLabelTileLayout {

        private boolean mLastSelected;

        QQSSideLabelTileLayout(Context context) {
            super(context, null);
            setClipChildren(false);
            setClipToPadding(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            setLayoutParams(lp);
        }

        @Override
        public boolean updateResources() {
            mResourceCellHeightResId = R.dimen.qs_quick_tile_size;
            boolean b = super.updateResources();
            mMaxAllowedRows = getResourceRows();
            return b;
        }

        @Override
        protected void estimateCellHeight() {
            FontSizeUtils.updateFontSize(mTempTextView, R.dimen.qs_tile_text_size);
            int unspecifiedSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            mTempTextView.measure(unspecifiedSpec, unspecifiedSpec);
            int padding = mContext.getResources().getDimensionPixelSize(R.dimen.qs_tile_padding);
            // the QQS only have 1 label
            mEstimatedCellHeight = mTempTextView.getMeasuredHeight() + padding * 2;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // Make sure to always use the correct number of rows. As it's determined by the
            // columns, just use as many as needed.
            updateMaxRows(10000, mRecords.size());
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        public void setListening(boolean listening, UiEventLogger uiEventLogger) {
            boolean startedListening = !mListening && listening;
            super.setListening(listening, uiEventLogger);
            if (startedListening) {
                // getNumVisibleTiles() <= mRecords.size()
                for (int i = 0; i < getNumVisibleTiles(); i++) {
                    QSTile tile = mRecords.get(i).tile;
                    uiEventLogger.logWithInstanceId(QSEvent.QQS_TILE_VISIBLE, 0,
                            tile.getMetricsSpec(), tile.getInstanceId());
                }
            }
        }

        @Override
        public void setExpansion(float expansion, float proposedTranslation) {
            if (expansion > 0f && expansion < 1f) {
                return;
            }
            // The cases we must set select for marquee when QQS/QS collapsed, and QS full expanded.
            // Expansion == 0f is when QQS is fully showing (as opposed to 1f, which is QS). At this
            // point we want them to be selected so the tiles will marquee (but not at other points
            // of expansion.
            boolean selected = (expansion == 1f || proposedTranslation < 0f);
            if (mLastSelected == selected) {
                return;
            }
            // We set it as not important while we change this, so setting each tile as selected
            // will not cause them to announce themselves until the user has actually selected the
            // item.
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).setSelected(selected);
            }
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            mLastSelected = selected;
        }

        @Override
        public int getResourceRows() {
            return TileUtils.getQQSRowsCount(mContext);
        }
    }
}
