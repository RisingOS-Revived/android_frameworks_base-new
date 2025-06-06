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
 *
 */

package com.android.systemui.keyguard.ui.view.layout.blueprints

import com.android.systemui.communal.ui.view.layout.sections.CommunalTutorialIndicatorSection
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.keyguard.shared.model.KeyguardBlueprint
import com.android.systemui.keyguard.shared.model.KeyguardSection
import com.android.systemui.keyguard.ui.view.layout.sections.AccessibilityActionsSection
import com.android.systemui.keyguard.ui.view.layout.sections.AodBurnInSection
import com.android.systemui.keyguard.ui.view.layout.sections.AodNotificationIconsSection
import com.android.systemui.keyguard.ui.view.layout.sections.ClockSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultDeviceEntrySection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultIndicationAreaSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultIndicationAreaTopSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultNotificationStackScrollLayoutSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultSettingsPopupMenuSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultShortcutsSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultStatusBarSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultStatusViewSection
import com.android.systemui.keyguard.ui.view.layout.sections.DefaultUdfpsAccessibilityOverlaySection
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardSectionsModule.Companion.KEYGUARD_AMBIENT_INDICATION_AREA_SECTION
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardSliceViewSection
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardWidgetViewSection
import com.android.systemui.keyguard.ui.view.layout.sections.NowBarSection
import com.android.systemui.keyguard.ui.view.layout.sections.InfoWidgetsSection
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardClockStyleSection
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardPeekDisplaySection
import com.android.systemui.keyguard.ui.view.layout.sections.AODStyleSection
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardWeatherViewSection
import com.android.systemui.keyguard.ui.view.layout.sections.SmartspaceSection
import java.util.Optional
import javax.inject.Inject
import javax.inject.Named
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Positions elements of the lockscreen to the default position.
 *
 * This will be the most common use case for phones in portrait mode.
 */
@ExperimentalCoroutinesApi
@SysUISingleton
@JvmSuppressWildcards
class DefaultKeyguardBlueprint
@Inject
constructor(
    accessibilityActionsSection: AccessibilityActionsSection,
    defaultIndicationAreaSection: DefaultIndicationAreaSection,
    defaultDeviceEntrySection: DefaultDeviceEntrySection,
    defaultShortcutsSection: DefaultShortcutsSection,
    @Named(KEYGUARD_AMBIENT_INDICATION_AREA_SECTION)
    defaultAmbientIndicationAreaSection: Optional<KeyguardSection>,
    defaultSettingsPopupMenuSection: DefaultSettingsPopupMenuSection,
    defaultStatusViewSection: DefaultStatusViewSection,
    defaultStatusBarSection: DefaultStatusBarSection,
    defaultNotificationStackScrollLayoutSection: DefaultNotificationStackScrollLayoutSection,
    aodNotificationIconsSection: AodNotificationIconsSection,
    aodBurnInSection: AodBurnInSection,
    communalTutorialIndicatorSection: CommunalTutorialIndicatorSection,
    clockSection: ClockSection,
    smartspaceSection: SmartspaceSection,
    keyguardSliceViewSection: KeyguardSliceViewSection,
    keyguardWidgetViewSection: KeyguardWidgetViewSection,
    nowBarSection: NowBarSection,
    infoWidgetsSection: InfoWidgetsSection,
    keyguardClockStyleSection: KeyguardClockStyleSection,
    keyguardPeekDisplaySection: KeyguardPeekDisplaySection,
    aODStyleSection: AODStyleSection,
    keyguardWeatherViewSection: KeyguardWeatherViewSection,
    udfpsAccessibilityOverlaySection: DefaultUdfpsAccessibilityOverlaySection,
    defaultIndicationAreaTopSection: DefaultIndicationAreaTopSection,
) : KeyguardBlueprint {
    override val id: String = DEFAULT

    override val sections =
        listOfNotNull(
            accessibilityActionsSection,
            defaultIndicationAreaSection,
            defaultIndicationAreaTopSection,
            defaultShortcutsSection,
            defaultAmbientIndicationAreaSection.getOrNull(),
            defaultSettingsPopupMenuSection,
            defaultStatusViewSection,
            defaultStatusBarSection,
            defaultNotificationStackScrollLayoutSection,
            aodNotificationIconsSection,
            smartspaceSection,
            aodBurnInSection,
            communalTutorialIndicatorSection,
            clockSection,
            keyguardSliceViewSection,
            keyguardWidgetViewSection,
            nowBarSection,
            infoWidgetsSection,
            keyguardClockStyleSection,
            keyguardPeekDisplaySection,
	    aODStyleSection,
            keyguardWeatherViewSection,
            defaultDeviceEntrySection,
            udfpsAccessibilityOverlaySection, // Add LAST: Intentionally has z-order above others
        )

    companion object {
        const val DEFAULT = "default"
    }
}
