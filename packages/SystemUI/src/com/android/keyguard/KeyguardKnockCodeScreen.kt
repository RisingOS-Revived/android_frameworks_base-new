/*
 * Copyright (C) 2026 RisingOS (revived) Android Project
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

package com.android.keyguard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.systemui.res.R

private val MESSAGE_HORIZONTAL_PADDING = 24.dp

@Composable
fun KeyguardKnockCodeScreen(state: KnockCodeUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KnockCodeMessage(state = state, modifier = Modifier.fillMaxWidth())
        KnockCodePad(state = state, modifier = Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun KnockCodeMessage(state: KnockCodeUiState, modifier: Modifier = Modifier) {
    Text(
        text = state.message?.toString().orEmpty(),
        modifier =
            modifier
                .heightIn(min = dimensionResource(R.dimen.knock_code_message_min_height))
                .padding(horizontal = MESSAGE_HORIZONTAL_PADDING)
                .semantics { liveRegion = LiveRegionMode.Polite },
        color = state.messageColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
    )
}
