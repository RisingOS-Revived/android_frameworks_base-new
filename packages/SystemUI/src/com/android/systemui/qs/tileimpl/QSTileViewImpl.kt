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

package com.android.systemui.qs.tileimpl

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources.ID_NULL
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Trace
import android.os.UserHandle
import android.provider.Settings
import android.service.quicksettings.Tile
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AccelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.graphics.drawable.updateBounds
import com.android.app.animation.Interpolators
import com.android.app.tracing.traceSection
import com.android.settingslib.Utils
import com.android.systemui.Dependency
import com.android.systemui.Flags
import com.android.systemui.animation.Expandable
import com.android.systemui.animation.LaunchableView
import com.android.systemui.animation.LaunchableViewDelegate
import com.android.systemui.animation.view.LaunchableLinearLayout
import com.android.systemui.haptics.qs.QSLongPressEffect
import com.android.systemui.plugins.qs.QSIconView
import com.android.systemui.plugins.qs.QSTile
import com.android.systemui.plugins.qs.QSTile.AdapterState
import com.android.systemui.plugins.qs.QSTileView
import com.android.systemui.qs.TileUtils
import com.android.systemui.qs.logging.QSLogger
import com.android.systemui.qs.tileimpl.QSIconViewImpl.QS_ANIM_LENGTH
import com.android.systemui.res.R
import com.android.systemui.tuner.TunerService
import java.util.Objects
import java.util.Random

import com.android.internal.util.android.VibrationUtils

private const val TAG = "QSTileViewImpl"

open class QSTileViewImpl
@JvmOverloads
constructor(
    context: Context,
    private val collapsed: Boolean = false,
    private val longPressEffect: QSLongPressEffect? = null,
) : QSTileView(context), HeightOverrideable, LaunchableView, TunerService.Tunable {

    companion object {
        private const val INVALID = -1
        private const val BACKGROUND_NAME = "background"
        private const val LABEL_NAME = "label"
        private const val SECONDARY_LABEL_NAME = "secondaryLabel"
        private const val CHEVRON_NAME = "chevron"
        private const val OVERLAY_NAME = "overlay"
        const val UNAVAILABLE_ALPHA = 0.3f
        const val ACTIVE_ALPHA = 0.2f
        const val INACTIVE_ALPHA = 0.8f
        @VisibleForTesting internal const val TILE_STATE_RES_PREFIX = "tile_states_"
        @VisibleForTesting internal const val LONG_PRESS_EFFECT_WIDTH_SCALE = 1.1f
        @VisibleForTesting internal const val LONG_PRESS_EFFECT_HEIGHT_SCALE = 1.2f
        internal val EMPTY_RECT = Rect()

        private const val QS_TILE_UI_STYLE: String =
                "system:" + Settings.System.QS_TILE_UI_STYLE
        private const val QS_PANEL_STYLE: String =
                "system:" + Settings.System.QS_PANEL_STYLE
        private const val QS_TILE_ANIMATION_STYLE: String =
                "system:" + Settings.System.QS_TILE_ANIMATION_STYLE
        private const val QS_TILE_ANIMATION_DURATION: String =
                "system:" + Settings.System.QS_TILE_ANIMATION_DURATION
        private const val QS_TILE_ANIMATION_INTERPOLATOR: String =
                "system:" + Settings.System.QS_TILE_ANIMATION_INTERPOLATOR
    }

    private val icon: QSIconViewImpl = QSIconViewImpl(context)
    private var position: Int = INVALID
    private var hasLongClickEffect: Boolean = true

    override fun setPosition(position: Int) {
        this.position = position
    }

    override var heightOverride: Int = HeightOverrideable.NO_OVERRIDE
        set(value) {
            if (field == value) return
            field = value
            if (longPressEffect?.state != QSLongPressEffect.State.RUNNING_BACKWARDS_FROM_CANCEL) {
                updateHeight()
            }
        }

    override var squishinessFraction: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            updateHeight()
        }

    private var isA11Style: Boolean = Settings.System.getIntForUser(
            context.contentResolver,
            Settings.System.QS_TILE_UI_STYLE, 0, UserHandle.USER_CURRENT
        ) != 0

    private var qsPanelStyle: Int = Settings.System.getIntForUser(
            context.contentResolver,
            Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT
        )

    private var animStyle: Int = Settings.System.getIntForUser(
            context.contentResolver,
            Settings.System.QS_TILE_ANIMATION_STYLE, 0, UserHandle.USER_CURRENT
        )

    private var animDuration: Int = Settings.System.getIntForUser(
            context.contentResolver,
            Settings.System.QS_TILE_ANIMATION_DURATION, 1, UserHandle.USER_CURRENT
        )

    private var interpolatorType: Int = Settings.System.getIntForUser(
            context.contentResolver,
            Settings.System.QS_TILE_ANIMATION_INTERPOLATOR, 0, UserHandle.USER_CURRENT
        )

    private val colorActive = Utils.getColorAttrDefaultColor(context, R.attr.shadeActive)

    private val colorOffstate = Utils.getColorAttrDefaultColor(context, R.attr.shadeInactive) 
    private val colorInactive = if (isA11Style) Utils.applyAlpha(INACTIVE_ALPHA, colorOffstate)
            else colorOffstate
    private val colorUnavailable = Utils.applyAlpha(UNAVAILABLE_ALPHA, colorInactive)

    private val overlayColorActive =
        Utils.applyAlpha(
            /* alpha= */ 0.11f,
            Utils.getColorAttrDefaultColor(context, R.attr.onShadeActive),
        )
    private val overlayColorInactive =
        Utils.applyAlpha(
            /* alpha= */ 0.08f,
            Utils.getColorAttrDefaultColor(context, R.attr.onShadeInactive),
        )

    private val qsTileHaptic: Int = Settings.System.getIntForUser(
            context.contentResolver,
            Settings.System.QS_PANEL_TILE_HAPTIC, 0, UserHandle.USER_CURRENT
        )

    private var initialX = 0f
    private var initialY = 0f

    private val colorLabelActive = Utils.getColorAttrDefaultColor(context,
        if (isA11Style) R.attr.onShadeInactive
        else R.attr.onShadeActive)
    private val colorLabelInactive =
        Utils.getColorAttrDefaultColor(context, if (isA11Style) R.attr.onShadeInactiveVariant
        else R.attr.onShadeInactive)
    private val colorLabelUnavailable = Utils.getColorAttrDefaultColor(context, R.attr.outline)

    private val colorSecondaryLabelActive =
        Utils.getColorAttrDefaultColor(context, if (isA11Style) R.attr.onShadeInactiveVariant
        else R.attr.onShadeActiveVariant)
    private val colorSecondaryLabelInactive =
        Utils.getColorAttrDefaultColor(context, if (isA11Style) R.attr.outline
        else R.attr.onShadeInactiveVariant)
    private val colorSecondaryLabelUnavailable =
        Utils.getColorAttrDefaultColor(context, R.attr.outline)

    // QS Style 2
    private val colorActiveAlpha = Utils.applyAlpha(ACTIVE_ALPHA, Utils.getColorAttrDefaultColor(context, com.android.internal.R.attr.colorAccent))
    private val colorInactiveAlpha = resources.getColor(R.color.qs_translucent_bg)

    // QS Style 3
    private var randomColor: Random = Random()

    // QS Style 8
    private val colorActiveSurround = resources.getColor(R.color.qs_white_bg)

    @SuppressLint("NewApi")
    private var randomTint: Int = Color.rgb(
        (randomColor.nextInt(256) / 2f + 0.5).toFloat(),
        randomColor.nextInt(256).toFloat(),
        randomColor.nextInt(256).toFloat()
    )

    private val colorActiveRandom = Utils.applyAlpha(ACTIVE_ALPHA, randomTint)
    private val colorLabelActiveRandom = randomTint
    private val colorSecondaryLabelActiveRandom = randomTint
    private lateinit var iconContainer: LinearLayout
    private lateinit var label: TextView
    protected lateinit var secondaryLabel: TextView
    private lateinit var labelContainer: IgnorableChildLinearLayout
    protected lateinit var sideView: ViewGroup
    private lateinit var customDrawableView: ImageView
    private lateinit var chevronView: ImageView
    private var mQsLogger: QSLogger? = null

    /** Controls if tile background is set to a [RippleDrawable] see [setClickable] */
    protected var showRippleEffect = true

    private lateinit var qsTileBackground: RippleDrawable
    private lateinit var qsTileFocusBackground: Drawable
    private lateinit var backgroundDrawable: LayerDrawable
    protected lateinit var backgroundBaseDrawable: Drawable
    private lateinit var backgroundOverlayDrawable: Drawable

    private var backgroundColor: Int = 0
    private var backgroundOverlayColor: Int = 0

    private var radiusActive: Float = 0f
    private var radiusInactive: Float = 0f
    private val shapeAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = QS_ANIM_LENGTH
        interpolator = Interpolators.FAST_OUT_SLOW_IN
        addUpdateListener { animation ->
            changeCornerRadius(animation.animatedValue as Float)
        }
    }

    private val singleAnimator: ValueAnimator =
        ValueAnimator().apply {
            duration = QS_ANIM_LENGTH
            addUpdateListener { animation ->
                setAllColors(
                    // These casts will throw an exception if some property is missing. We should
                    // always have all properties.
                    animation.getAnimatedValue(BACKGROUND_NAME) as Int,
                    animation.getAnimatedValue(LABEL_NAME) as Int,
                    animation.getAnimatedValue(SECONDARY_LABEL_NAME) as Int,
                    animation.getAnimatedValue(CHEVRON_NAME) as Int,
                    animation.getAnimatedValue(OVERLAY_NAME) as Int,
                )
            }
        }

    private val tileAnimator = AnimatorSet().apply {
        playTogether(singleAnimator, shapeAnimator)
    }

    private var accessibilityClass: String? = null
    private var stateDescriptionDeltas: CharSequence? = null
    private var lastStateDescription: CharSequence? = null
    private var tileState = false
    private var lastState = INVALID
    private var lastIconTint = 0
    private val launchableViewDelegate =
        LaunchableViewDelegate(this, superSetVisibility = { super.setVisibility(it) })
    private var lastDisabledByPolicy = false

    private val locInScreen = IntArray(2)
    private var vertical = false
    private var labelHide = false
    private var forceHideCheveron = false
    private var labelSize = 14f

    /** Visuo-haptic long-press effects */
    private var longPressEffectAnimator: ValueAnimator? = null
    var haveLongPressPropertiesBeenReset = true
        private set

    private var paddingForLaunch = Rect()
    private var initialLongPressProperties: QSLongPressProperties? = null
    private var finalLongPressProperties: QSLongPressProperties? = null
    private val colorEvaluator = ArgbEvaluator.getInstance()
    val isLongPressEffectInitialized: Boolean
        get() = longPressEffect?.hasInitialized == true

    val areLongPressEffectPropertiesSet: Boolean
        get() = initialLongPressProperties != null && finalLongPressProperties != null

    init {
        val typedValue = TypedValue()
        if (!getContext().theme.resolveAttribute(R.attr.isQsTheme, typedValue, true)) {
            throw IllegalStateException(
                "QSViewImpl must be inflated with a theme that contains " +
                    "Theme.SystemUI.QuickSettings"
            )
        }
        setId(generateViewId())
        vertical = TileUtils.getQSTileVerticalLayout(context)
        labelHide = TileUtils.getQSTileLabelHide(context)
        forceHideCheveron = vertical || labelHide
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        clipChildren = false
        clipToPadding = false

        if (isA11Style) {
            val iconContainerSize = context.resources.getDimensionPixelSize(R.dimen.qs_quick_tile_size)
            radiusActive = iconContainerSize / 2f
            radiusInactive = iconContainerSize / 4f
            iconContainer = LaunchableLinearLayout(context)
            iconContainer.layoutParams = LayoutParams(iconContainerSize, iconContainerSize)
            iconContainer.clipChildren = false
            iconContainer.clipToPadding = false
            iconContainer.orientation = LinearLayout.VERTICAL
            iconContainer.gravity = Gravity.CENTER
            iconContainer.isFocusable = true
            iconContainer.background = createTileBackground()
        } else {
            isFocusable = true
            background = createTileBackground()
        }

        setColor(getBackgroundColorForState(QSTile.State.DEFAULT_STATE))
        
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.qs_icon_size)

        if (isA11Style) {
            changeCornerRadius(getCornerRadiusForState(QSTile.State.DEFAULT_STATE))
            iconContainer.addView(icon, LayoutParams(iconSize, iconSize))
            addView(iconContainer, 0)
        } else {
            val padding = resources.getDimensionPixelSize(R.dimen.qs_tile_padding)
            val startPadding = if (vertical) padding else resources.getDimensionPixelSize(R.dimen.qs_tile_start_padding)
            setPaddingRelative(startPadding, padding, padding, padding)
            addView(icon, LayoutParams(iconSize, iconSize))
        }

        val tunerService = Dependency.get(TunerService::class.java)
        tunerService.addTunable(this, QS_TILE_UI_STYLE)
        tunerService.addTunable(this, QS_PANEL_STYLE)
        tunerService.addTunable(this, QS_TILE_ANIMATION_STYLE)
        tunerService.addTunable(this, QS_TILE_ANIMATION_DURATION)
        tunerService.addTunable(this, QS_TILE_ANIMATION_INTERPOLATOR)

        createAndAddLabels()
        createAndAddSideView()
        updateResources()
    }

    override fun onTuningChanged(key: String, newValue: String?) {
        if (key == QS_TILE_UI_STYLE) {
            isA11Style = TunerService.parseIntegerSwitch(newValue, false)
        } else if (key == QS_PANEL_STYLE) {
            qsPanelStyle = TunerService.parseInteger(newValue, 0)
        } else if (key == QS_TILE_ANIMATION_STYLE) {
            animStyle = TunerService.parseInteger(newValue, 0)
        } else if (key == QS_TILE_ANIMATION_DURATION) {
            animDuration = TunerService.parseInteger(newValue, 1)
        } else if (key == QS_TILE_ANIMATION_INTERPOLATOR) {
            interpolatorType = TunerService.parseInteger(newValue, 0)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateResources()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Trace.traceBegin(Trace.TRACE_TAG_APP, "QSTileViewImpl#onMeasure")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Trace.endSection()
    }

    override fun resetOverride() {
        heightOverride = HeightOverrideable.NO_OVERRIDE
        updateHeight()
    }

    fun setQsLogger(qsLogger: QSLogger) {
        mQsLogger = qsLogger
    }

    fun updateResources() {
        labelSize = TileUtils.getQSTileLabelSize(context)
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSize)
        secondaryLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSize)

        if (isA11Style) {
            updateA11StyleResources()
        } else {
            updateDefaultResources()
        }
    }

    fun updateDefaultResources() {
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.qs_icon_size)
        icon.layoutParams.apply {
            height = iconSize
            width = iconSize
        }

        vertical = TileUtils.getQSTileVerticalLayout(context)
        if (vertical) {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        } else {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }

        if (labelHide)
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL

        val padding = resources.getDimensionPixelSize(R.dimen.qs_tile_padding)
        val startPadding = if (vertical) padding else resources.getDimensionPixelSize(R.dimen.qs_tile_start_padding)
        setPaddingRelative(startPadding, padding, padding, padding)

        val labelMargin = if (vertical) 0 else resources.getDimensionPixelSize(R.dimen.qs_label_container_margin)
        (labelContainer.layoutParams as MarginLayoutParams).apply { marginStart = labelMargin }

        (sideView.layoutParams as MarginLayoutParams).apply { marginStart = labelMargin }
        (chevronView.layoutParams as MarginLayoutParams).apply {
            height = iconSize
            width = iconSize
        }

        val endMargin = resources.getDimensionPixelSize(R.dimen.qs_drawable_end_margin)
        (customDrawableView.layoutParams as MarginLayoutParams).apply {
            height = iconSize
            marginEnd = endMargin
        }

        background = createTileBackground()
        setColor(backgroundColor)
        setOverlayColor(backgroundOverlayColor)
    }

    fun updateA11StyleResources() {
        labelContainer.invalidate()
        labelContainer.apply {
            ignoreLastView = collapsed
            forceUnspecifiedMeasure = collapsed
        }
        secondaryLabel.alpha = if (collapsed) 0f else 1f

        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER

        val iconContainerSize = context.resources.getDimensionPixelSize(R.dimen.qs_quick_tile_size)
        iconContainer.layoutParams.apply {
            height = iconContainerSize
            width = iconContainerSize
        }
        val padding = resources.getDimensionPixelSize(R.dimen.qs_tile_padding)
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.qs_icon_size)
        icon.layoutParams.apply {
            height = iconSize
            width = iconSize
        }
        iconContainer.setPaddingRelative(padding, padding, padding, padding)
        (labelContainer.layoutParams as MarginLayoutParams).apply {
            topMargin = padding / 2
        }
    }

    private fun createAndAddLabels() {
        labelContainer =
            LayoutInflater.from(context)
                .inflate(if (isA11Style || vertical) R.layout.qs_tile_label_vertical else R.layout.qs_tile_label, this, false)
                as IgnorableChildLinearLayout
        label = labelContainer.requireViewById(R.id.tile_label)
        secondaryLabel = labelContainer.requireViewById(R.id.app_label)
        if (isA11Style) {
            if (collapsed) {
                labelContainer.ignoreLastView = true
                // Ideally, it'd be great if the parent could set this up when measuring just this child
                // instead of the View class having to support this. However, due to the mysteries of
                // LinearLayout's double measure pass, we cannot overwrite `measureChild` or any of its
                // sibling methods to have special behavior for labelContainer.
                labelContainer.forceUnspecifiedMeasure = true
                secondaryLabel.alpha = 0f
            }
        } else {
            labelContainer.invalidate()
            labelContainer.apply {
                ignoreLastView = collapsed
                forceUnspecifiedMeasure = collapsed
            }
            secondaryLabel.alpha = if (collapsed) 0f else 1f
        }
        setLabelColor(getLabelColorForState(QSTile.State.DEFAULT_STATE))
        setSecondaryLabelColor(getSecondaryLabelColorForState(QSTile.State.DEFAULT_STATE))

        if (Flags.gsfQuickSettings()) {
            label.apply {
                typeface = Typeface.create("gsf-title-small-emphasized", Typeface.NORMAL)
            }
            secondaryLabel.apply { typeface = Typeface.create("gsf-label-medium", Typeface.NORMAL) }
        }

        if (!labelHide)
            addView(labelContainer)
    }

    private fun createAndAddSideView() {
        sideView =
            LayoutInflater.from(context)
                .inflate(if (isA11Style) R.layout.qs_tile_side_icon_a11 else R.layout.qs_tile_side_icon, this, false)
                as ViewGroup
        customDrawableView = sideView.requireViewById(R.id.customDrawable)
        chevronView = sideView.requireViewById(R.id.chevron)
        setChevronColor(getChevronColorForState(QSTile.State.DEFAULT_STATE))
        addView(sideView)
    }

    private fun createTileBackground(): Drawable {
        if (isA11Style) {
            qsTileBackground =
                if (Flags.qsTileFocusState()) {
                    mContext.getDrawable(R.drawable.qs_tile_background_flagged_no_mask) as RippleDrawable
                } else {
                    mContext.getDrawable(R.drawable.qs_tile_background_no_mask) as RippleDrawable
                }
        } else {
            qsTileBackground =
                if (Flags.qsTileFocusState()) {
                    mContext.getDrawable(R.drawable.qs_tile_background_flagged) as RippleDrawable
                } else {
                    mContext.getDrawable(R.drawable.qs_tile_background) as RippleDrawable
                }
        }
        qsTileFocusBackground = mContext.getDrawable(R.drawable.qs_tile_focused_background)!!
        backgroundDrawable =
            qsTileBackground.findDrawableByLayerId(R.id.background) as LayerDrawable
        backgroundBaseDrawable =
            backgroundDrawable.findDrawableByLayerId(R.id.qs_tile_background_base)
        backgroundOverlayDrawable =
            backgroundDrawable.findDrawableByLayerId(R.id.qs_tile_background_overlay)
        backgroundOverlayDrawable.mutate().setTintMode(PorterDuff.Mode.SRC)
        return qsTileBackground
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateHeight()
        maybeUpdateLongPressEffectWidth(measuredWidth.toFloat())
    }

    private fun maybeUpdateLongPressEffectWidth(width: Float) {
        if (!isLongClickable || longPressEffect == null || !hasLongClickEffect) return

        initialLongPressProperties?.width = width
        finalLongPressProperties?.width = LONG_PRESS_EFFECT_WIDTH_SCALE * width

        val deltaW = (LONG_PRESS_EFFECT_WIDTH_SCALE - 1f) * width
        paddingForLaunch.left = -deltaW.toInt() / 2
        paddingForLaunch.right = deltaW.toInt() / 2
    }

    private fun maybeUpdateLongPressEffectHeight(height: Float) {
        if (!isLongClickable || longPressEffect == null || !hasLongClickEffect) return

        initialLongPressProperties?.height = height
        finalLongPressProperties?.height = LONG_PRESS_EFFECT_HEIGHT_SCALE * height

        val deltaH = (LONG_PRESS_EFFECT_HEIGHT_SCALE - 1f) * height
        paddingForLaunch.top = -deltaH.toInt() / 2
        paddingForLaunch.bottom = deltaH.toInt() / 2
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (Flags.qsTileFocusState()) {
            if (gainFocus) {
                qsTileFocusBackground.setBounds(0, 0, width, height)
                overlay.add(qsTileFocusBackground)
            } else {
                overlay.clear()
            }
        }
    }

    private fun updateHeight() {
        val actualHeight =
            if (heightOverride != HeightOverrideable.NO_OVERRIDE) {
                heightOverride
            } else {
                measuredHeight
            }
        // Limit how much we affect the height, so we don't have rounding artifacts when the tile
        // is too short.
        val constrainedSquishiness = constrainSquishiness(squishinessFraction)
        bottom = top + (actualHeight * constrainedSquishiness).toInt()
        scrollY = (actualHeight - height) / if (vertical) 7 else 2
        label.alpha = if (!vertical) 1.0f else Math.pow(squishinessFraction.toDouble(), 7.0).toFloat()
        maybeUpdateLongPressEffectHeight(actualHeight.toFloat())
    }

    override fun updateAccessibilityOrder(previousView: View?): View {
        accessibilityTraversalAfter = previousView?.id ?: ID_NULL
        return this
    }

    override fun getIcon(): QSIconView {
        return icon
    }

    override fun getIconWithBackground(): View {
        return if (isA11Style) iconContainer else icon
    }

    override fun init(tile: QSTile) {
        if (!isA11Style && longPressEffect != null) {
            isHapticFeedbackEnabled = false
            longPressEffect.qsTile = tile
            longPressEffect.createExpandableFromView(this)
            initLongPressEffectCallback()
            init(
                { _: View -> longPressEffect.onTileClick() },
                { _: View ->
                    longPressEffect.onTileLongClick()
                    true
                }, // Haptics and long-clicks are handled by [QSLongPressEffect]
            )
        } else {
            val expandable = Expandable.fromView(this)
            init(
                { _: View? -> tile.click(expandable) },
                { _: View? ->
                    tile.longClick(expandable)
                    true
                },
            )
        }
    }

    private fun setAnimationTile(v: View) {
        val animTile: ObjectAnimator = when (animStyle) {
	    1 -> ObjectAnimator.ofFloat(v, "rotation", 0f, 360f) // Rotate
	    2 -> ObjectAnimator.ofFloat(v, "rotationX", 0f, 360f) // Flip X
	    3 -> ObjectAnimator.ofFloat(v, "rotationY", 0f, 360f) // Flip Y
	    4 -> ObjectAnimator.ofFloat(v, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f) // Shake
	    5 -> ObjectAnimator.ofFloat(v, "alpha", 0f, 1f) // Fade In
	    6 -> ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.2f, 0.8f, 1f) // Bounce Effect (Scale X)
	    7 -> ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.2f, 0.8f, 1f) // Bounce Effect (Scale Y)
	    8 -> ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.1f, 1f).apply {
	        repeatCount = 0
	        duration = 1000
	    } // Pulse Animation X
	    9 -> ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.1f, 1f).apply {
	        repeatCount = 0
	        duration = 1000
	    } // Pulse Animation Y
            else -> return
        }

        animTile.interpolator = when (interpolatorType) {
            0 -> LinearInterpolator()
            1 -> AccelerateInterpolator()
            2 -> DecelerateInterpolator()
            3 -> AccelerateDecelerateInterpolator()
            4 -> BounceInterpolator()
            5 -> OvershootInterpolator()
            6 -> AnticipateInterpolator()
            7 -> AnticipateOvershootInterpolator()
            else -> null
        }

        animTile.duration = (animDuration * 1000).toLong()
        animTile.start()
    }

    private fun initLongPressEffectCallback() {
        longPressEffect?.callback =
            object : QSLongPressEffect.Callback {

                override fun onResetProperties() {
                    resetLongPressEffectProperties()
                }

                override fun onEffectFinishedReversing() {
                    // The long-press effect properties finished at the same starting point.
                    // This is the same as if the properties were reset
                    haveLongPressPropertiesBeenReset = true
                }

                override fun onStartAnimator() {
                    if (longPressEffectAnimator?.isRunning != true) {
                        longPressEffectAnimator =
                            ValueAnimator.ofFloat(0f, 1f).apply {
                                this.duration = longPressEffect?.effectDuration?.toLong() ?: 0L
                                interpolator = AccelerateDecelerateInterpolator()

                                doOnStart { longPressEffect?.handleAnimationStart() }
                                addUpdateListener {
                                    val value = animatedValue as Float
                                    if (value == 0f) {
                                        bringToFront()
                                    } else {
                                        updateLongPressEffectProperties(value)
                                    }
                                }
                                doOnEnd { longPressEffect?.handleAnimationComplete() }
                                doOnCancel { longPressEffect?.handleAnimationCancel() }
                                start()
                            }
                    }
                }

                override fun onReverseAnimator(playHaptics: Boolean) {
                    longPressEffectAnimator?.let {
                        val pausedProgress = it.animatedFraction
                        if (playHaptics) longPressEffect?.playReverseHaptics(pausedProgress)
                        it.reverse()
                    }
                }

                override fun onCancelAnimator() {
                    resetLongPressEffectProperties()
                    longPressEffectAnimator?.cancel()
                }
            }
    }

    private fun getTileView(): View {
        return if (isA11Style) getIconWithBackground() else this
    }

    private fun init(click: OnClickListener?, longClick: OnLongClickListener?) {
        setOnClickListener {
            setAnimationTile(getTileView())
            click?.onClick(it)
        }
        onLongClickListener = longClick
    }

    override fun onStateChanged(state: QSTile.State) {
        // We cannot use the handler here because sometimes, the views are not attached (if they
        // are in a page that the ViewPager hasn't attached). Instead, we use a runnable where
        // all its instances are `equal` to each other, so they can be used to remove them from the
        // queue.
        // This means that at any given time there's at most one enqueued runnable to change state.
        // However, as we only ever care about the last state posted, this is fine.
        val runnable = StateChangeRunnable(state.copy())
        removeCallbacks(runnable)
        post(runnable)
    }

    override fun getDetailY(): Int {
        return top + height / 2
    }

    override fun hasOverlappingRendering(): Boolean {
        // Avoid layers for this layout - we don't need them.
        return false
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        if (isA11Style) {
            if (!Flags.qsTileFocusState()) {
                iconContainer.background =
                    if (clickable && showRippleEffect) {
                        qsTileBackground.also {
                            // In case that the colorBackgroundDrawable was used as the background, make
                            // sure
                            // it has the correct callback instead of null
                            backgroundDrawable.callback = it
                        }
                    } else {
                        backgroundDrawable
                    }
            }
        } else {
            if (!Flags.qsTileFocusState()) {
                background =
                    if (clickable && showRippleEffect) {
                        qsTileBackground.also {
                            // In case that the colorBackgroundDrawable was used as the background, make
                            // sure
                            // it has the correct callback instead of null
                            backgroundDrawable.callback = it
                        }
                    } else {
                        backgroundDrawable
                    }
            }
        }
    }

    override fun getLabelContainer(): View {
        return labelContainer
    }

    override fun getLabel(): View {
        return label
    }

    override fun getSecondaryLabel(): View {
        return secondaryLabel
    }

    override fun getSecondaryIcon(): View {
        return sideView
    }

    override fun setShouldBlockVisibilityChanges(block: Boolean) {
        launchableViewDelegate.setShouldBlockVisibilityChanges(block)
    }

    override fun getAnimatedView(): LaunchableView {
        return if (isA11Style) getIconWithBackground() as LaunchableView else this
    }

    override fun setVisibility(visibility: Int) {
        launchableViewDelegate.setVisibility(visibility)
    }

    // Accessibility

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        if (!TextUtils.isEmpty(accessibilityClass)) {
            event.className = accessibilityClass
        }
        if (
            event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION &&
                stateDescriptionDeltas != null
        ) {
            event.text.add(stateDescriptionDeltas)
            stateDescriptionDeltas = null
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        // Clear selected state so it is not announce by talkback.
        info.isSelected = false
        info.text =
            if (TextUtils.isEmpty(secondaryLabel.text)) {
                "${label.text}"
            } else {
                "${label.text}, ${secondaryLabel.text}"
            }
        if (lastDisabledByPolicy) {
            info.addAction(
                AccessibilityNodeInfo.AccessibilityAction(
                    AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id,
                    resources.getString(
                        R.string.accessibility_tile_disabled_by_policy_action_description
                    ),
                )
            )
        } else {
            if (isLongClickable) {
                info.addAction(
                    AccessibilityNodeInfo.AccessibilityAction(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK.id,
                        resources.getString(R.string.accessibility_long_click_tile),
                    )
                )
            }
        }
        if (!TextUtils.isEmpty(accessibilityClass)) {
            info.className =
                if (lastDisabledByPolicy) {
                    Button::class.java.name
                } else {
                    accessibilityClass
                }
            if (Switch::class.java.name == accessibilityClass) {
                info.isChecked = tileState
                info.isCheckable = true
            }
        }
        if (position != INVALID) {
            info.collectionItemInfo =
                AccessibilityNodeInfo.CollectionItemInfo(position, 1, 0, 1, false)
        }
    }

    override fun toString(): String {
        val sb = StringBuilder(javaClass.simpleName).append('[')
        sb.append("locInScreen=(${locInScreen[0]}, ${locInScreen[1]})")
        sb.append(", iconView=$icon")
        sb.append(", tileState=$tileState")
        sb.append("]")
        return sb.toString()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val result = super.onTouchEvent(event)
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        if (event == null) return result
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                longPressEffect?.handleActionDown()
                if (isLongClickable) {
                    postDelayed(
                        { longPressEffect?.handleTimeoutComplete() },
                        ViewConfiguration.getTapTimeout().toLong(),
                    )
                }
            }
            MotionEvent.ACTION_UP -> {
                val distanceX = Math.abs(event.x - initialX)
                val distanceY = Math.abs(event.y - initialY)
                longPressEffect?.handleActionUp()
                if (distanceX < touchSlop && distanceY < touchSlop) {
                    VibrationUtils.triggerVibration(context, qsTileHaptic)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressEffect?.handleActionCancel()
            }
        }
        return result
    }

    // HANDLE STATE CHANGES RELATED METHODS
    protected open fun handleStateChanged(state: QSTile.State) {
        val allowAnimations = animationsEnabled()
        isClickable = state.state != Tile.STATE_UNAVAILABLE
        isLongClickable = state.handlesLongClick
        hasLongClickEffect = !isA11Style && state.handlesLongClick && state.hasLongClickEffect
        icon.setIcon(state, allowAnimations)
        contentDescription = state.contentDescription

        // State handling and description
        val stateDescription = StringBuilder()
        val arrayResId = SubtitleArrayMapping.getSubtitleId(state.spec)
        val stateText = state.getStateText(arrayResId, resources)
        state.secondaryLabel = state.getSecondaryLabel(stateText)
        if (!TextUtils.isEmpty(stateText)) {
            stateDescription.append(stateText)
        }
        if (state.disabledByPolicy && state.state != Tile.STATE_UNAVAILABLE) {
            stateDescription.append(", ")
            stateDescription.append(getUnavailableText(state.spec))
        }
        if (!TextUtils.isEmpty(state.stateDescription)) {
            stateDescription.append(", ")
            stateDescription.append(state.stateDescription)
            if (
                lastState != INVALID &&
                    state.state == lastState &&
                    state.stateDescription != lastStateDescription
            ) {
                stateDescriptionDeltas = state.stateDescription
            }
        }

        setStateDescription(stateDescription.toString())
        lastStateDescription = state.stateDescription

        accessibilityClass =
            if (state.state == Tile.STATE_UNAVAILABLE) {
                null
            } else {
                state.expandedAccessibilityClassName
            }

        if (state is AdapterState) {
            val newState = state.value
            if (tileState != newState) {
                tileState = newState
            }
        }

        // Labels
        if (!Objects.equals(label.text, state.label)) {
            label.text = state.label
        }
        if (!Objects.equals(secondaryLabel.text, state.secondaryLabel)) {
            secondaryLabel.text = state.secondaryLabel
        }
        secondaryLabel.visibility =
            if (TextUtils.isEmpty(state.secondaryLabel)) {
                if (isA11Style) INVISIBLE else GONE
            } else {
                VISIBLE
            }

        // Colors
        if (state.state != lastState || state.disabledByPolicy != lastDisabledByPolicy) {
            if (isA11Style) {
                tileAnimator.cancel()
            } else {
                singleAnimator.cancel()
            }
            mQsLogger?.logTileBackgroundColorUpdateIfInternetTile(
                state.spec,
                state.state,
                state.disabledByPolicy,
                getBackgroundColorForState(state.state, state.disabledByPolicy),
            )
            if (allowAnimations) {
                if (isA11Style) {
                    shapeAnimator.setFloatValues(
                        getCornerRadiusForState(lastState), 
                        getCornerRadiusForState(state.state)
                    )
                }
                singleAnimator.setValues(
                    colorValuesHolder(
                        BACKGROUND_NAME,
                        backgroundColor,
                        getBackgroundColorForState(state.state, state.disabledByPolicy),
                    ),
                    colorValuesHolder(
                        LABEL_NAME,
                        label.currentTextColor,
                        getLabelColorForState(state.state, state.disabledByPolicy),
                    ),
                    colorValuesHolder(
                        SECONDARY_LABEL_NAME,
                        secondaryLabel.currentTextColor,
                        getSecondaryLabelColorForState(state.state, state.disabledByPolicy),
                    ),
                    colorValuesHolder(
                        CHEVRON_NAME,
                        chevronView.imageTintList?.defaultColor ?: 0,
                        getChevronColorForState(state.state, state.disabledByPolicy),
                    ),
                    colorValuesHolder(
                        OVERLAY_NAME,
                        backgroundOverlayColor,
                        getOverlayColorForState(state.state),
                    ),
                )
                if (isA11Style) {
                    tileAnimator.start()
                } else {
                   singleAnimator.start()
                }
            } else {
                setAllColors(
                    getBackgroundColorForState(state.state, state.disabledByPolicy),
                    getLabelColorForState(state.state, state.disabledByPolicy),
                    getSecondaryLabelColorForState(state.state, state.disabledByPolicy),
                    getChevronColorForState(state.state, state.disabledByPolicy),
                    getOverlayColorForState(state.state),
                )
                if (isA11Style) {
                    changeCornerRadius(getCornerRadiusForState(state.state))
                }
            }
        }

        // Right side icon
        loadSideViewDrawableIfNecessary(state)

        label.isEnabled = !state.disabledByPolicy

        lastState = state.state
        lastDisabledByPolicy = state.disabledByPolicy
        lastIconTint = icon.getColor(state)

        // Long-press effects
        longPressEffect?.qsTile?.state?.handlesLongClick = hasLongClickEffect
        if (
            hasLongClickEffect &&
                longPressEffect?.initializeEffect(longPressEffectDuration) == true
        ) {
            showRippleEffect = false
            longPressEffect.qsTile?.state?.state = lastState // Store the tile's state
            longPressEffect.resetState()
            initializeLongPressProperties(measuredHeight, measuredWidth)
        } else {
            // Long-press effects might have been enabled before but the new state does not
            // handle a long-press. In this case, we go back to the behavior of a regular tile
            // and clean-up the resources
            showRippleEffect = isClickable
            initialLongPressProperties = null
            finalLongPressProperties = null
        }
    }

    private fun setAllColors(
        backgroundColor: Int,
        labelColor: Int,
        secondaryLabelColor: Int,
        chevronColor: Int,
        overlayColor: Int,
    ) {
        setColor(backgroundColor)
        setLabelColor(labelColor)
        setSecondaryLabelColor(secondaryLabelColor)
        setChevronColor(chevronColor)
        setOverlayColor(overlayColor)
    }

    protected fun setColor(color: Int) {
        backgroundBaseDrawable.mutate().setTint(color)
        backgroundColor = color
    }

    private fun setLabelColor(color: Int) {
        label.setTextColor(color)
    }

    private fun setSecondaryLabelColor(color: Int) {
        secondaryLabel.setTextColor(color)
    }

    private fun setChevronColor(color: Int) {
        chevronView.imageTintList = ColorStateList.valueOf(color)
    }

    private fun setOverlayColor(overlayColor: Int) {
        backgroundOverlayDrawable.setTint(overlayColor)
        backgroundOverlayColor = overlayColor
    }

    private fun loadSideViewDrawableIfNecessary(state: QSTile.State) {
        if (state.sideViewCustomDrawable != null) {
            customDrawableView.setImageDrawable(state.sideViewCustomDrawable)
            customDrawableView.visibility = VISIBLE
            chevronView.visibility = GONE
        } else if ((state !is AdapterState || state.forceExpandIcon) && !forceHideCheveron) {
            customDrawableView.setImageDrawable(null)
            customDrawableView.visibility = GONE
            chevronView.visibility = VISIBLE
        } else {
            customDrawableView.setImageDrawable(null)
            customDrawableView.visibility = GONE
            chevronView.visibility = GONE
        }
    }

    private fun getUnavailableText(spec: String?): String {
        val arrayResId = SubtitleArrayMapping.getSubtitleId(spec)
        return resources.getStringArray(arrayResId)[Tile.STATE_UNAVAILABLE]
    }

    private fun getCornerRadiusForState(state: Int): Float {
        if (TileUtils.getQsUiStyle(context) == 1)
            return radiusActive

        if (TileUtils.getQsUiStyle(context) == 2)
            return radiusInactive

        return when (state) {
            Tile.STATE_ACTIVE -> radiusActive
            Tile.STATE_INACTIVE -> radiusInactive
            Tile.STATE_UNAVAILABLE -> radiusInactive
            else -> radiusInactive
        }
    }

    /*
     * The view should not be animated if it's not on screen and no part of it is visible.
     */
    protected open fun animationsEnabled(): Boolean {
        if (!isShown) {
            return false
        }
        if (alpha != 1f) {
            return false
        }
        getLocationOnScreen(locInScreen)
        return locInScreen.get(1) >= -height
    }

    protected open fun getBackgroundColorForState(state: Int, disabledByPolicy: Boolean = false): Int {
        return when {
            state == Tile.STATE_UNAVAILABLE || disabledByPolicy -> colorUnavailable
            state == Tile.STATE_ACTIVE -> 
                if (qsPanelStyle == 2 || qsPanelStyle == 9) 
                    colorActiveAlpha 
                else if (qsPanelStyle == 3) 
                    colorActiveRandom 
                else colorActive
            state == Tile.STATE_INACTIVE ->
                if (qsPanelStyle >= 1)
                    colorInactiveAlpha 
                else
                    colorInactive
            else -> {
                Log.e(TAG, "Invalid state $state")
                0
            }
        }
    }

    private fun getLabelColorForState(state: Int, disabledByPolicy: Boolean = false): Int {
        return when {
            state == Tile.STATE_UNAVAILABLE || disabledByPolicy -> colorLabelUnavailable
            state == Tile.STATE_ACTIVE -> 
                if (qsPanelStyle == 1 || qsPanelStyle == 2 || qsPanelStyle == 9)
                    colorActive
                else if (qsPanelStyle == 3) 
                    colorLabelActiveRandom
                else if (qsPanelStyle == 4 || qsPanelStyle == 6 || qsPanelStyle == 8 || qsPanelStyle == 9)   
                    colorActiveSurround
                else colorLabelActive
            state == Tile.STATE_INACTIVE -> colorLabelInactive
            else -> {
                Log.e(TAG, "Invalid state $state")
                0
            }
        }
    }

    private fun getSecondaryLabelColorForState(state: Int, disabledByPolicy: Boolean = false): Int {
        return when {
            state == Tile.STATE_UNAVAILABLE || disabledByPolicy -> colorSecondaryLabelUnavailable
            state == Tile.STATE_ACTIVE -> 
                if(qsPanelStyle == 1 || qsPanelStyle == 2 || qsPanelStyle == 9) 
                    colorActive
                else if(qsPanelStyle == 3) 
                    colorSecondaryLabelActiveRandom
                else if(qsPanelStyle == 4 || qsPanelStyle == 6 || qsPanelStyle == 8 || qsPanelStyle == 9)   
                    colorActiveSurround
                else colorSecondaryLabelActive
            state == Tile.STATE_INACTIVE -> colorSecondaryLabelInactive
            else -> {
                Log.e(TAG, "Invalid state $state")
                0
            }
        }
    }

    private fun getChevronColorForState(state: Int, disabledByPolicy: Boolean = false): Int =
        getSecondaryLabelColorForState(state, disabledByPolicy)

    private fun getOverlayColorForState(state: Int): Int {
        return when (state) {
            Tile.STATE_ACTIVE -> overlayColorActive
            Tile.STATE_INACTIVE -> overlayColorInactive
            else -> Color.TRANSPARENT
        }
    }

    override fun onActivityLaunchAnimationEnd() {
        longPressEffect?.resetState()
        if (!isA11Style && longPressEffect != null && !haveLongPressPropertiesBeenReset) {
            resetLongPressEffectProperties()
        }
    }

    private fun prepareForLaunch() {
        val startingHeight = initialLongPressProperties?.height?.toInt() ?: 0
        val startingWidth = initialLongPressProperties?.width?.toInt() ?: 0
        val deltaH = finalLongPressProperties?.height?.minus(startingHeight)?.toInt() ?: 0
        val deltaW = finalLongPressProperties?.width?.minus(startingWidth)?.toInt() ?: 0
        paddingForLaunch.left = -deltaW / 2
        paddingForLaunch.top = -deltaH / 2
        paddingForLaunch.right = deltaW / 2
        paddingForLaunch.bottom = deltaH / 2
    }

    override fun getPaddingForLaunchAnimation(): Rect =
        if (longPressEffect?.state == QSLongPressEffect.State.LONG_CLICKED) {
            paddingForLaunch
        } else {
            EMPTY_RECT
        }

    fun updateLongPressEffectProperties(effectProgress: Float) {
        if (!isLongClickable || longPressEffect == null || !hasLongClickEffect) return

        if (haveLongPressPropertiesBeenReset) haveLongPressPropertiesBeenReset = false

        // Dimensions change
        val newHeight =
            interpolateFloat(
                    effectProgress,
                    initialLongPressProperties?.height ?: 0f,
                    finalLongPressProperties?.height ?: 0f,
                )
                .toInt()
        val newWidth =
            interpolateFloat(
                    effectProgress,
                    initialLongPressProperties?.width ?: 0f,
                    finalLongPressProperties?.width ?: 0f,
                )
                .toInt()

        val startingHeight = initialLongPressProperties?.height?.toInt() ?: 0
        val startingWidth = initialLongPressProperties?.width?.toInt() ?: 0
        val deltaH = (newHeight - startingHeight) / 2
        val deltaW = (newWidth - startingWidth) / 2

        background?.let {
            it.updateBounds(
                left = -deltaW,
                top = -deltaH,
                right = newWidth - deltaW,
                bottom = newHeight - deltaH,
            )
        }

        // Radius change
        val newRadius =
            interpolateFloat(
                effectProgress,
                initialLongPressProperties?.cornerRadius ?: 0f,
                finalLongPressProperties?.cornerRadius ?: 0f,
            )
        changeCornerRadius(newRadius)

        // Color change
        setAllColors(
            colorEvaluator.evaluate(
                effectProgress,
                initialLongPressProperties?.backgroundColor ?: 0,
                finalLongPressProperties?.backgroundColor ?: 0,
            ) as Int,
            colorEvaluator.evaluate(
                effectProgress,
                initialLongPressProperties?.labelColor ?: 0,
                finalLongPressProperties?.labelColor ?: 0,
            ) as Int,
            colorEvaluator.evaluate(
                effectProgress,
                initialLongPressProperties?.secondaryLabelColor ?: 0,
                finalLongPressProperties?.secondaryLabelColor ?: 0,
            ) as Int,
            colorEvaluator.evaluate(
                effectProgress,
                initialLongPressProperties?.chevronColor ?: 0,
                finalLongPressProperties?.chevronColor ?: 0,
            ) as Int,
            colorEvaluator.evaluate(
                effectProgress,
                initialLongPressProperties?.overlayColor ?: 0,
                finalLongPressProperties?.overlayColor ?: 0,
            ) as Int,
        )
        icon.setTint(
            icon.mIcon as ImageView,
            colorEvaluator.evaluate(
                effectProgress,
                initialLongPressProperties?.iconColor ?: 0,
                finalLongPressProperties?.iconColor ?: 0,
            ) as Int,
        )
    }

    private fun interpolateFloat(fraction: Float, start: Float, end: Float): Float =
        start + fraction * (end - start)

    fun resetLongPressEffectProperties() {
        background?.let {
            it.updateBounds(
                left = 0,
                top = 0,
                right = initialLongPressProperties?.width?.toInt() ?: measuredWidth,
                bottom = initialLongPressProperties?.height?.toInt() ?: measuredHeight,
            )
        }
        if (isA11Style) {
            changeCornerRadius(getCornerRadiusForState(lastState))
        } else {
            changeCornerRadius(resources.getDimensionPixelSize(R.dimen.qs_corner_radius).toFloat())
        }
        setAllColors(
            getBackgroundColorForState(lastState, lastDisabledByPolicy),
            getLabelColorForState(lastState, lastDisabledByPolicy),
            getSecondaryLabelColorForState(lastState, lastDisabledByPolicy),
            getChevronColorForState(lastState, lastDisabledByPolicy),
            getOverlayColorForState(lastState),
        )
        icon.setTint(icon.mIcon as ImageView, lastIconTint)
        haveLongPressPropertiesBeenReset = true
    }

    @VisibleForTesting
    fun initializeLongPressProperties(startingHeight: Int, startingWidth: Int) {
        initialLongPressProperties =
            QSLongPressProperties(
                height = startingHeight.toFloat(),
                width = startingWidth.toFloat(),
                resources.getDimensionPixelSize(R.dimen.qs_corner_radius).toFloat(),
                getBackgroundColorForState(lastState),
                getLabelColorForState(lastState),
                getSecondaryLabelColorForState(lastState),
                getChevronColorForState(lastState),
                getOverlayColorForState(lastState),
                lastIconTint,
            )

        finalLongPressProperties =
            QSLongPressProperties(
                height = LONG_PRESS_EFFECT_HEIGHT_SCALE * startingHeight,
                width = LONG_PRESS_EFFECT_WIDTH_SCALE * startingWidth,
                resources.getDimensionPixelSize(R.dimen.qs_corner_radius).toFloat() - 20,
                getBackgroundColorForState(Tile.STATE_ACTIVE),
                getLabelColorForState(Tile.STATE_ACTIVE),
                getSecondaryLabelColorForState(Tile.STATE_ACTIVE),
                getChevronColorForState(Tile.STATE_ACTIVE),
                getOverlayColorForState(Tile.STATE_ACTIVE),
                Utils.getColorAttrDefaultColor(context, R.attr.onShadeActive),
            )
        prepareForLaunch()
    }

    private fun changeCornerRadius(radius: Float) {
        for (i in 0 until backgroundDrawable.numberOfLayers) {
            val layer = backgroundDrawable.getDrawable(i)
            if (layer is GradientDrawable) {
                layer.cornerRadius = radius
            }
        }
    }

    @VisibleForTesting
    internal fun getCurrentColors(): List<Int> =
        listOf(
            backgroundColor,
            label.currentTextColor,
            secondaryLabel.currentTextColor,
            chevronView.imageTintList?.defaultColor ?: 0,
        )

    inner class StateChangeRunnable(private val state: QSTile.State) : Runnable {
        override fun run() {
            var traceTag = "QSTileViewImpl#handleStateChanged"
            if (!state.spec.isNullOrEmpty()) {
                traceTag += ":"
                traceTag += state.spec
            }
            traceSection(traceTag.take(Trace.MAX_SECTION_NAME_LEN)) { handleStateChanged(state) }
        }

        // We want all instances of this runnable to be equal to each other, so they can be used to
        // remove previous instances from the Handler/RunQueue of this view
        override fun equals(other: Any?): Boolean {
            return other is StateChangeRunnable
        }

        // This makes sure that all instances have the same hashcode (because they are `equal`)
        override fun hashCode(): Int {
            return StateChangeRunnable::class.hashCode()
        }
    }
}

fun constrainSquishiness(squish: Float): Float {
    return 0.1f + squish * 0.9f
}

private fun colorValuesHolder(name: String, vararg values: Int): PropertyValuesHolder {
    return PropertyValuesHolder.ofInt(name, *values).apply {
        setEvaluator(ArgbEvaluator.getInstance())
    }
}
