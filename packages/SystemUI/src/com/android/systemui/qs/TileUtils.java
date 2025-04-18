/*
* Copyright (C) 2022-2024 crDroid Android Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.systemui.res.R;
import com.android.systemui.flags.Flags;
import com.android.systemui.flags.RefactorFlag;

public class TileUtils {

    private static boolean useSmallLandscapeLockscreenResources(Resources res) {
        return RefactorFlag.forView(Flags.LOCKSCREEN_ENABLE_LANDSCAPE).isEnabled()
                && res.getBoolean(R.bool.is_small_screen_landscape);
    }

    public static int getQSColumnsCount(Context context) {
        Resources res = context.getResources();
        int columns = useSmallLandscapeLockscreenResources(res)
                ? res.getInteger(R.integer.small_land_lockscreen_quick_settings_num_columns)
                : res.getInteger(R.integer.quick_settings_num_columns);
        return getQSColumnsCount(context, columns);
    }

    private static int getQSColumnsCount(Context context, int resourceCount) {
        final int QS_COLUMNS_MIN = 2;
        final Resources res = context.getResources();
        int value = QS_COLUMNS_MIN;
        if (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            value = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QS_LAYOUT_COLUMNS,
                    resourceCount, UserHandle.USER_CURRENT);
        } else {
            value = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE,
                    resourceCount, UserHandle.USER_CURRENT);
        }
        return Math.max(QS_COLUMNS_MIN, value);
    }

    public static int getQSRowsCount(Context context) {
        final int QS_ROWS_MIN = 1;
        int value = QS_ROWS_MIN;
        int valueQQS = QS_ROWS_MIN;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            value = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QS_LAYOUT_ROWS,
                    4, UserHandle.USER_CURRENT);
            valueQQS = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QQS_LAYOUT_ROWS,
                    2, UserHandle.USER_CURRENT);
        } else {
            value = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QS_LAYOUT_ROWS_LANDSCAPE,
                    2, UserHandle.USER_CURRENT);
            valueQQS = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE,
                    1, UserHandle.USER_CURRENT);
        }
        return Math.max(value, valueQQS);
    }

    public static int getQQSRowsCount(Context context) {
        final int QS_ROWS_MIN = 1;
        int value = QS_ROWS_MIN;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            value = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QQS_LAYOUT_ROWS,
                    2, UserHandle.USER_CURRENT);
        } else {
            value = Settings.System.getIntForUser(
                    context.getContentResolver(), Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE,
                    1, UserHandle.USER_CURRENT);
        }
        return Math.max(QS_ROWS_MIN, value);
    }

    public static boolean getQSTileLabelHide(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QS_TILE_LABEL_HIDE,
                0, UserHandle.USER_CURRENT) != 0;
    }

    public static float getQSTileLabelSize(Context context) {
        int labelSize = Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QS_TILE_LABEL_SIZE,
                14, UserHandle.USER_CURRENT);
        if (getQsUiStyle(context) != 0) labelSize = labelSize - 2;
        return (float) labelSize;
    }

    public static boolean getQSTileVerticalLayout(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QS_TILE_VERTICAL_LAYOUT,
                0, UserHandle.USER_CURRENT) != 0;
    }

   public static int getQsUiStyle(Context context) {
       return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QS_TILE_UI_STYLE,
                0, UserHandle.USER_CURRENT);
   }

   public static boolean isCompactQSMediaPlayerEnforced(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
            "qs_compact_media_player_mode",0, UserHandle.USER_CURRENT) != 0;
   }

   public static boolean canShowSplitShade(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
            "qs_split_shade_enabled",0, UserHandle.USER_CURRENT) != 0
            && context.getResources().getConfiguration().orientation
            == Configuration.ORIENTATION_LANDSCAPE;
   }

   public static boolean isQsWidgetsEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), 
            "qs_widgets_enabled",0, UserHandle.USER_CURRENT) != 0;
   }

   public static boolean canShowQsWidgets(Context context) {
        return isQsWidgetsEnabled(context)
            && context.getResources().getConfiguration().orientation 
            != Configuration.ORIENTATION_LANDSCAPE;
   }
}
