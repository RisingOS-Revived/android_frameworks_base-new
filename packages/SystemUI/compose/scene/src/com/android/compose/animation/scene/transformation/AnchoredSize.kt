/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.transformation

import androidx.compose.ui.unit.IntSize
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.content.state.TransitionState

/** Anchor the size of an element to the size of another element. */
internal class AnchoredSize
private constructor(
    private val anchor: ElementKey,
    private val anchorWidth: Boolean,
    private val anchorHeight: Boolean,
) : InterpolatedPropertyTransformation<IntSize> {
    override val property = PropertyTransformation.Property.Size

    override fun PropertyTransformationScope.transform(
        content: ContentKey,
        element: ElementKey,
        transition: TransitionState.Transition,
        idleValue: IntSize,
    ): IntSize {
        fun anchorSizeIn(content: ContentKey): IntSize {
            val size =
                anchor.targetSize(content)
                    ?: throwMissingAnchorException(
                        transformation = "AnchoredSize",
                        anchor = anchor,
                        content = content,
                    )

            return IntSize(
                width = if (anchorWidth) size.width else idleValue.width,
                height = if (anchorHeight) size.height else idleValue.height,
            )
        }

        // This simple implementation assumes that the size of [element] is the same as the size of
        // the [anchor] in [scene], so simply transform to the size of the anchor in the other
        // scene.
        return if (content == transition.fromContent) {
            anchorSizeIn(transition.toContent)
        } else {
            anchorSizeIn(transition.fromContent)
        }
    }

    class Factory(
        private val anchor: ElementKey,
        private val anchorWidth: Boolean,
        private val anchorHeight: Boolean,
    ) : Transformation.Factory {
        override fun create(): Transformation = AnchoredSize(anchor, anchorWidth, anchorHeight)
    }
}
