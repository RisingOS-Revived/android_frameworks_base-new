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
 * distributed under the License is distshellributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wm.shell.recents;

import android.app.ActivityManager.RunningTaskInfo;

import com.android.wm.shell.shared.GroupedTaskInfo;

/**
 * Listener interface that Launcher attaches to SystemUI to get split-screen callbacks.
 */
oneway interface IRecentTasksListener {

    /**
     * Called when the set of recent tasks change.
     */
    void onRecentTasksChanged();

    /**
     * Called when a running task appears.
     */
    void onRunningTaskAppeared(in RunningTaskInfo taskInfo);

    /**
     * Called when a running task vanishes.
     */
    void onRunningTaskVanished(in RunningTaskInfo taskInfo);

    /**
     * Called when a running task changes.
     */
    void onRunningTaskChanged(in RunningTaskInfo taskInfo);

    /** A task has moved to front. Only used if enableShellTopTaskTracking() is disabled. */
    void onTaskMovedToFront(in GroupedTaskInfo taskToFront);

    /** A task info has changed. Only used if enableShellTopTaskTracking() is disabled. */
    void onTaskInfoChanged(in RunningTaskInfo taskInfo);

    /**
     * If enableShellTopTaskTracking() is enabled, this reports the set of all visible tasks.
     * Otherwise, this reports only the new top most visible task.
     */
    void onVisibleTasksChanged(in GroupedTaskInfo[] visibleTasks);
}