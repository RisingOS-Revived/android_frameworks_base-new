/*
 * Copyright (C) 2026 The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.penguin.glass;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

/**
 * @hide
 */
public final class LiquidTabBars {
    private static final String TAG = "LiquidTabBars";

    public static final String SETTING_ENABLED = "penguin_glass_tab_bars";

    private static final String[] DISABLED_PACKAGES = {
            "com.google.android.apps.photos",
    };

    private static final String[] BAR_CLASSES = {
            "BottomNavigationView", "NavigationBarView", "PivotBar", "BottomNavigation",
            "BottomBar", "TabBar",
    };
    private static final String[] SCROLLER_CLASSES = {
            "RecyclerView", "ListView", "ScrollView", "GridView",
    };
    private static final long SCAN_INTERVAL_MS = 500;
    private static final long EAGER_MS = 3000;

    private final float mDensity;
    private final List<LiquidTabBar> mBars = new ArrayList<>();
    private Floater mFloater;
    private ComposeTabBar mComposeBar;
    private long mNextComposeScan;
    private int mComposeMisses;
    private final WeakHashMap<View, Boolean> mPadded = new WeakHashMap<>();
    private long mLastScan;
    private boolean mScanPending;

    private LiquidTabBars(Context context) {
        mDensity = context.getResources().getDisplayMetrics().density;
        mCreated = SystemClock.uptimeMillis();
    }

    private final long mCreated;

    public static LiquidTabBars create(Context context) {
        if (Process.myUid() < Process.FIRST_APPLICATION_UID) return null;
        try {
            if (Settings.Secure.getInt(context.getContentResolver(), SETTING_ENABLED, 0) == 0) {
                return null;
            }
            if (Arrays.asList(DISABLED_PACKAGES).contains(context.getPackageName())) return null;
            return new LiquidTabBars(context);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public void onLayout(View root) {
        boolean attached = false;
        for (int i = mBars.size() - 1; i >= 0; i--) {
            LiquidTabBar bar = mBars.get(i);
            if (bar.mBar.isAttachedToWindow()) {
                attached = true;
            } else {
                bar.remove();
                mBars.remove(i);
            }
        }
        if (mBars.isEmpty()) mFloater = null;
        if (mComposeBar != null) {
            if (mComposeBar.mView.isAttachedToWindow() && mComposeBar.update()) {
                attached = true;
            } else {
                mComposeBar.remove();
                mComposeBar = null;
                mComposeMisses = 0;
                mNextComposeScan = 0;
            }
        }
        if (attached || !(root instanceof ViewGroup)) return;
        long now = SystemClock.uptimeMillis();
        boolean eager = now - mCreated < EAGER_MS;
        long wait = eager ? 0 : SCAN_INTERVAL_MS - (now - mLastScan);
        if (wait > 0) {
            if (!mScanPending) {
                mScanPending = true;
                root.postDelayed(() -> {
                    mScanPending = false;
                    if (root.isAttachedToWindow()) onLayout(root);
                }, wait);
            }
            return;
        }
        mLastScan = now;
        ViewGroup found = find((ViewGroup) root, root.getWidth(), root.getHeight());
        if (found == null) {
            if (eager || now >= mNextComposeScan) {
                findCompose((ViewGroup) root, root.getHeight());
                if (mComposeBar != null) {
                    mComposeBar.setPoll(() -> {
                        if (root.isAttachedToWindow()) onLayout(root);
                    });
                }
                if (mComposeBar == null) {
                    mNextComposeScan = now + (SCAN_INTERVAL_MS << mComposeMisses);
                    mComposeMisses = Math.min(mComposeMisses + 1, 2);
                } else {
                    mComposeMisses = 0;
                }
            }
            if (mComposeBar == null && !mScanPending) {
                mScanPending = true;
                root.postDelayed(() -> {
                    mScanPending = false;
                    if (root.isAttachedToWindow()) onLayout(root);
                }, Math.max(mNextComposeScan - now, SCAN_INTERVAL_MS));
            }
            return;
        }
        try {
            mBars.add(new LiquidTabBar(found, this::onNormal));
            mFloater = floatOverContent(found);
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not restyle " + found.getClass().getName(), e);
        }
    }

    private void onNormal(float normal) {
        if (mFloater != null) mFloater.setNormal(normal);
    }

    public boolean onTouch(MotionEvent ev) {
        if (mComposeBar != null && mComposeBar.onTouch(ev)) return true;
        for (int i = 0; i < mBars.size(); i++) {
            LiquidTabBar bar = mBars.get(i);
            View view = bar.mBar;
            if (!view.isAttachedToWindow() || !view.isShown()) continue;
            view.getLocationInWindow(mLocation);
            MotionEvent local = MotionEvent.obtain(ev);
            local.offsetLocation(-mLocation[0], -mLocation[1]);
            float sx = view.getScaleX();
            float sy = view.getScaleY();
            if (sx != 1f || sy != 1f) {
                local.setLocation(local.getX() / sx, local.getY() / sy);
            }
            bar.onObserveTouch(local);
            local.recycle();
        }
        return false;
    }

    private final int[] mLocation = new int[2];

    private void findCompose(ViewGroup group, int rootHeight) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE || child.getWidth() == 0) continue;
            child.getLocationInWindow(mLocation);
            if (mLocation[1] + child.getHeight() < rootHeight - 4) continue;
            if (child.getAccessibilityNodeProvider() != null) {
                try {
                    mComposeBar = ComposeTabBar.find(child);
                } catch (RuntimeException e) {
                    Log.w(TAG, "Could not look for a Compose tab bar", e);
                }
                if (mComposeBar != null) {
                    return;
                }
            }
            if (child instanceof ViewGroup g) {
                findCompose(g, rootHeight);
                if (mComposeBar != null) return;
            }
        }
    }

    private ViewGroup find(ViewGroup group, int rootWidth, int rootHeight) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE || !(child instanceof ViewGroup g)) continue;
            if (isBar(g, rootWidth, rootHeight)) return g;
            ViewGroup found = find(g, rootWidth, rootHeight);
            if (found != null) return found;
        }
        return null;
    }

    private boolean isBar(ViewGroup v, int rootWidth, int rootHeight) {
        int h = v.getHeight();
        if (v.getWidth() < rootWidth * 0.9f || h < 40 * mDensity || h > 160 * mDensity) {
            return false;
        }
        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        if (loc[1] + h < rootHeight - 4) return false;
        return matches(v.getClass(), BAR_CLASSES);
    }

    private static boolean matches(Class<?> cls, String[] names) {
        for (Class<?> c = cls; c != null && c != View.class; c = c.getSuperclass()) {
            String name = c.getSimpleName();
            for (String n : names) {
                if (name.contains(n)) return true;
            }
        }
        return false;
    }

    private interface Step {
        void apply(float normal);
    }

    private static final class Floater {
        private final ArrayList<Step> mSteps = new ArrayList<>();

        void add(Step step) {
            mSteps.add(step);
        }

        void setNormal(float normal) {
            for (int i = 0; i < mSteps.size(); i++) mSteps.get(i).apply(normal);
        }
    }

    private Floater floatOverContent(ViewGroup bar) {
        ViewParent p = bar.getParent();
        if (!(p instanceof ViewGroup parent)) return null;
        int barHeight = bar.getHeight();
        Floater floater = new Floater();
        View content = null;
        if (parent instanceof LinearLayout ll && ll.getOrientation() == LinearLayout.VERTICAL) {
            int index = parent.indexOfChild(bar);
            if (index > 0 && bar.getLayoutParams() instanceof ViewGroup.MarginLayoutParams lp) {
                floater.add(margin(bar, lp.topMargin, barHeight, true));
                content = parent.getChildAt(index - 1);
            }
        } else {
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child == bar || Math.abs(child.getBottom() - bar.getTop()) > 2) continue;
                Step step = extendToBottom(child, bar, barHeight);
                if (step != null) {
                    floater.add(step);
                    content = child;
                    break;
                }
            }
        }
        if (content != null) padScrollers(content, floater, barHeight);
        floater.setNormal(0f);
        return floater;
    }

    private static Step margin(View view, int base, int barHeight, boolean top) {
        return n -> {
            if (!(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams lp)) return;
            int v = base - Math.round(barHeight * (1f - n));
            if ((top ? lp.topMargin : lp.bottomMargin) == v) return;
            if (top) lp.topMargin = v;
            else lp.bottomMargin = v;
            view.setLayoutParams(lp);
        };
    }

    private static Step extendToBottom(View child, View bar, int barHeight) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        int barId = bar.getId();
        if (lp instanceof RelativeLayout.LayoutParams rl
                && rl.getRule(RelativeLayout.ABOVE) == barId) {
            return relative(child, barId);
        }
        if (hasConstraint(lp, barId)) {
            return constraint(child, barId);
        }
        if (lp instanceof ViewGroup.MarginLayoutParams m && m.bottomMargin >= barHeight - 2) {
            return margin(child, m.bottomMargin, barHeight, false);
        }
        return null;
    }

    private static Step relative(View child, int barId) {
        boolean[] floating = {false};
        return n -> {
            boolean f = n < 0.5f;
            if (f == floating[0]) return;
            if (!(child.getLayoutParams() instanceof RelativeLayout.LayoutParams rl)) return;
            floating[0] = f;
            if (f) {
                rl.removeRule(RelativeLayout.ABOVE);
                rl.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            } else {
                rl.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                rl.addRule(RelativeLayout.ABOVE, barId);
            }
            child.setLayoutParams(rl);
        };
    }

    private static boolean hasConstraint(ViewGroup.LayoutParams lp, int barId) {
        if (barId == View.NO_ID) return false;
        try {
            return lp.getClass().getField("bottomToTop").getInt(lp) == barId;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static Step constraint(View child, int barId) {
        boolean[] floating = {false};
        return n -> {
            boolean f = n < 0.5f;
            if (f == floating[0]) return;
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            try {
                Class<?> cls = lp.getClass();
                cls.getField("bottomToTop").setInt(lp, f ? -1 : barId);
                cls.getField("bottomToBottom").setInt(lp, f ? 0 : -1);
                floating[0] = f;
                child.setLayoutParams(lp);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        };
    }

    private void padScrollers(View view, Floater floater, int barHeight) {
        if (!(view instanceof ViewGroup group)) return;
        if (matches(view.getClass(), SCROLLER_CLASSES) && !mPadded.containsKey(view)) {
            mPadded.put(view, true);
            group.setClipToPadding(false);
            int base = view.getPaddingBottom();
            floater.add(n -> {
                int v = base + Math.round(barHeight * (1f - n));
                if (view.getPaddingBottom() == v) return;
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                        view.getPaddingRight(), v);
            });
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            padScrollers(group.getChildAt(i), floater, barHeight);
        }
    }
}
