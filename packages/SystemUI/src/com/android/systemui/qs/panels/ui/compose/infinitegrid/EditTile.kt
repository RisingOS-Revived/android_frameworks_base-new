/*
 * Copyright (C) 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package com.android.systemui.qs.panels.ui.compose.infinitegrid

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import com.android.compose.gesture.effect.rememberOffsetOverscrollEffectFactory
import com.android.compose.modifiers.height
import com.android.compose.modifiers.thenIf
import com.android.compose.theme.LocalAndroidColorScheme
import com.android.compose.ui.graphics.painter.rememberDrawablePainter
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.common.ui.compose.Icon
import com.android.systemui.common.ui.compose.load
import com.android.systemui.common.ui.icons.MoreVert
import com.android.systemui.common.ui.icons.Undo
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.qs.panels.shared.model.SizedTileImpl
import com.android.systemui.qs.panels.ui.compose.DragAndDropState
import com.android.systemui.qs.panels.ui.compose.DragType
import com.android.systemui.qs.panels.ui.compose.EditTileListState
import com.android.systemui.qs.panels.ui.compose.EditTileListState.Companion.INVALID_INDEX
import com.android.systemui.qs.panels.ui.compose.GridBackground
import com.android.systemui.qs.panels.ui.compose.dragAndDropRemoveZone
import com.android.systemui.qs.panels.ui.compose.dragAndDropTileList
import com.android.systemui.qs.panels.ui.compose.dragAndDropTileSource
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.InactiveCornerRadius
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.TileArrangementPadding
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.TileHeight
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.ToggleTargetSize
import com.android.systemui.qs.panels.ui.compose.selection.MutableSelectionState
import com.android.systemui.qs.panels.ui.compose.selection.StaticTileBadge
import com.android.systemui.qs.panels.ui.compose.selection.TileState
import com.android.systemui.qs.panels.ui.compose.selection.rememberSelectionState
import com.android.systemui.qs.panels.ui.compose.selection.selectableTile
import com.android.systemui.qs.panels.ui.model.GridCell
import com.android.systemui.qs.panels.ui.model.SpacerGridCell
import com.android.systemui.qs.panels.ui.model.TileGridCell
import com.android.systemui.qs.panels.ui.viewmodel.EditTileViewModel
import com.android.systemui.qs.panels.ui.viewmodel.EditTileViewModelConstants.APP_ICON_INLINE_CONTENT_ID
import com.android.systemui.qs.panels.ui.viewmodel.EditTopBarActionViewModel
import com.android.systemui.qs.panels.ui.viewmodel.IconTilesViewModel
import com.android.systemui.qs.panels.ui.viewmodel.InfiniteGridSnapshotViewModel
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.qs.shared.model.TileCategory
import com.android.systemui.qs.shared.model.groupAndSort
import com.android.systemui.qs.ui.compose.borderOnFocus
import com.android.systemui.res.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalTileDragHandler = staticCompositionLocalOf<((TileSpec) -> Unit)?> { null }

object TileType

object EditModeTileDefaults {
    const val PLACEHOLDER_ALPHA = .3f
    const val AUTO_SCROLL_DISTANCE = 100
    const val AUTO_SCROLL_SPEED = 2
    const val AVAILABLE_TILES_GRID_ALPHA = .32f
    val CurrentTilesGridPadding = 10.dp
    val GridBackgroundCornerRadius = 28.dp

    @Composable
    fun editTileColors(): TileColors =
        TileColors(
            background = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
            label = MaterialTheme.colorScheme.onSurface,
            secondaryLabel = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = MaterialTheme.colorScheme.onSurfaceVariant,
        )
}

private const val CURRENT_TILES_GRID_TEST_TAG = "CurrentTilesGrid"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditModeTopBar(
    onStopEditing: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val surfaceEffect2 = LocalAndroidColorScheme.current.surfaceEffect2
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        title = {
            Text(
                text = stringResource(id = R.string.qs_edit_tiles),
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(start = 24.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onStopEditing,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = surfaceEffect2,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription =
                        stringResource(id = com.android.internal.R.string.action_bar_up_description),
                )
            }
        },
        actions = actions,
        modifier = modifier.padding(vertical = 8.dp),
        windowInsets = WindowInsets(0.dp),
    )
}

sealed interface EditAction {
    data class InsertTile(val tileSpec: TileSpec, val position: Int) : EditAction
    data class RemoveTile(val tileSpec: TileSpec) : EditAction
    data class SetTiles(val tileSpecs: List<TileSpec>) : EditAction
    data class ResizeTileGrid(val tileSpec: TileSpec, val spanCols: Int, val spanRows: Int) : EditAction
    data object ResetGrid : EditAction
}

@Composable
private fun SingleTopBarAction(
    editTopBarActionViewModel: EditTopBarActionViewModel,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { editTopBarActionViewModel.onClick() },
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = modifier,
    ) {
        Icon(
            editTopBarActionViewModel.icon,
            contentDescription = stringResource(id = editTopBarActionViewModel.labelId),
        )
    }
}

@Composable
private fun TopBarActionOverflow(
    actionsViewModel: SnapshotStateList<EditTopBarActionViewModel>,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        val density = LocalDensity.current
        val offset =
            with(density) {
                val safeContent = WindowInsets.safeDrawing
                val layoutDirection = LocalLayoutDirection.current
                DpOffset(
                    -safeContent.getLeft(this, layoutDirection).toDp(),
                    -safeContent.getTop(this).toDp(),
                )
            }
        IconButton(
            onClick = { showMenu = !showMenu },
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
        ) {
            Icon(
                MoreVert,
                contentDescription = stringResource(R.string.qs_edit_menu_content_description),
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.sysuiResTag("OptionsDropdown").requiredWidthIn(min = 216.dp),
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            offset = offset,
        ) {
            actionsViewModel.forEach { action ->
                key(action.labelId) {
                    DropdownMenuElement(action, dismissDropdown = { showMenu = false })
                }
            }
        }
    }
}

@Composable
private fun DropdownMenuElement(
    action: EditTopBarActionViewModel,
    dismissDropdown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        onClick = {
            action.onClick()
            dismissDropdown()
        },
        text = {
            Box(modifier = Modifier.padding(start = 6.dp)) {
                Text(
                    text = stringResource(action.labelId),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.wrapContentHeight(Alignment.CenterVertically),
                )
            }
        },
        leadingIcon = {
            Icon(action.icon, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        colors = menuItemColors(),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.heightIn(min = 52.dp),
    )
}

@ReadOnlyComposable
@Composable
private fun menuItemColors() =
    MenuItemColors(
        textColor = MaterialTheme.colorScheme.onSurface,
        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingIconColor = Color.Transparent,
        disabledTextColor = Color.Transparent,
        disabledLeadingIconColor = Color.Transparent,
        disabledTrailingIconColor = Color.Transparent,
    )

@Composable
fun DefaultEditTileGrid(
    listState: EditTileListState,
    allTiles: List<EditTileViewModel>,
    snapshotViewModel: InfiniteGridSnapshotViewModel,
    topBarActions: SnapshotStateList<EditTopBarActionViewModel>,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onStopEditing: () -> Unit = {},
    iconTilesViewModel: IconTilesViewModel,
    onEditAction: (EditAction) -> Unit = {},
) {
    val selectionState = rememberSelectionState()

    LaunchedEffect(selectionState.placementEvent) {
        selectionState.placementEvent?.let { event ->
            listState
                .targetIndexForPlacement(event)
                .takeIf { it != INVALID_INDEX }
                ?.let { onEditAction(EditAction.InsertTile(event.movingSpec, it)) }
        }
    }

    Scaffold(
        modifier =
            modifier
                .consumeWindowInsets(WindowInsets.displayCutout)
                .sysuiResTag("EditModeRoot"),
        containerColor = Color.Transparent,
        topBar = {
            EditModeTopBar(onStopEditing = onStopEditing, modifier = Modifier.statusBarsPadding()) {
                AnimatedVisibility(snapshotViewModel.canUndo, enter = fadeIn(), exit = fadeOut()) {
                    IconButton(
                        enabled = snapshotViewModel.canUndo,
                        onClick = {
                            selectionState.unSelect()
                            snapshotViewModel.undo()
                        },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Icon(
                            Undo,
                            contentDescription =
                                stringResource(id = com.android.internal.R.string.undo),
                        )
                    }
                }
                if (topBarActions.size == 1) {
                    SingleTopBarAction(topBarActions.single())
                } else if (topBarActions.size > 1) {
                    TopBarActionOverflow(topBarActions)
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalOverscrollFactory provides rememberOffsetOverscrollEffectFactory()
        ) {
            AutoScrollGrid(listState, scrollState, innerPadding)

            LaunchedEffect(listState.dragType) {
                if (listState.dragInProgress && listState.dragType == DragType.Add) {
                    scrollState.animateScrollTo(0)
                }
            }

            EditModeScrollableColumn(
                listState = listState,
                selectionState = selectionState,
                innerPadding = innerPadding,
                scrollState = scrollState,
                onEditAction = onEditAction,
            ) {
                CurrentTilesGridHeader(
                    listState,
                    selectionState,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                )

                CurrentTilesGrid(
                    listState = listState,
                    selectionState = selectionState,
                    onEditAction = onEditAction,
                    iconTilesViewModel = iconTilesViewModel,
                )
            }
        }
    }
}

@Composable
private fun EditModeScrollableColumn(
    listState: EditTileListState,
    selectionState: MutableSelectionState,
    innerPadding: PaddingValues,
    scrollState: ScrollState,
    onEditAction: (EditAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = spacedBy(dimensionResource(id = R.dimen.qs_label_container_margin)),
        modifier =
            modifier
                .fillMaxSize()
                // Apply top padding before the scroll so the scrollable doesn't show under
                // the top bar
                .padding(top = innerPadding.calculateTopPadding())
                .clipScrollableContainer(Orientation.Vertical)
                .verticalScroll(scrollState)
                .dragAndDropRemoveZone(listState) { spec, removalEnabled ->
                    if (removalEnabled) {
                        // If removal is enabled, remove the tile
                        onEditAction(EditAction.RemoveTile(spec))
                    } else {
                        // Otherwise submit the new tile ordering
                        onEditAction(EditAction.SetTiles(listState.tileSpecs()))
                        selectionState.select(spec)
                    }
                },
    ) {
        content()

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}

@Composable
private fun AutoScrollGrid(
    listState: EditTileListState,
    scrollState: ScrollState,
    padding: PaddingValues,
) {
    val density = LocalDensity.current
    val (top, bottom) =
        remember(density) {
            with(density) {
                padding.calculateTopPadding().roundToPx() to
                    padding.calculateBottomPadding().roundToPx()
            }
        }
    val scrollTarget by
        remember(listState, scrollState, top, bottom) {
            derivedStateOf {
                val position = listState.draggedPosition
                if (position.isSpecified) {
                    val y = position.y.roundToInt()
                    when {
                        y < EditModeTileDefaults.AUTO_SCROLL_DISTANCE + top -> 0
                        y > scrollState.viewportSize - bottom - EditModeTileDefaults.AUTO_SCROLL_DISTANCE ->
                            scrollState.maxValue
                        else -> null
                    }
                } else {
                    null
                }
            }
        }
    LaunchedEffect(scrollTarget) {
        scrollTarget?.let {
            val distance = abs(it - scrollState.value)
            scrollState.animateScrollTo(
                it,
                animationSpec =
                    tween(durationMillis = distance * EditModeTileDefaults.AUTO_SCROLL_SPEED, easing = LinearEasing),
            )
        }
    }
}

private enum class EditModeHeaderState {
    Place,
    Idle,
}

@Composable
private fun rememberEditModeState(
    listState: EditTileListState,
    selectionState: MutableSelectionState,
): State<EditModeHeaderState> {
    val editGridHeaderState = remember { mutableStateOf(EditModeHeaderState.Idle) }
    LaunchedEffect(
        listState.dragInProgress,
        selectionState.selected,
        selectionState.placementEnabled,
    ) {
        editGridHeaderState.value =
            when {
                selectionState.placementEnabled -> EditModeHeaderState.Place
                else -> EditModeHeaderState.Idle
            }
    }

    return editGridHeaderState
}

@Composable
private fun CurrentTilesGridHeader(
    listState: EditTileListState,
    selectionState: MutableSelectionState,
    modifier: Modifier = Modifier,
) {
    val editGridHeaderState by rememberEditModeState(listState, selectionState)

    AnimatedContent(
        targetState = editGridHeaderState,
        label = "QSEditHeader",
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) { state ->
        EditGridHeader {
            when (state) {
                EditModeHeaderState.Idle -> {
                    EditGridCenteredText(
                        text = stringResource(id = R.string.select_to_rearrange_tiles)
                    )
                }
                EditModeHeaderState.Place -> {
                    EditGridCenteredText(text = stringResource(id = R.string.tap_to_position_tile))
                }
            }
        }
    }
}

@Composable
private fun EditGridHeader(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun EditGridCenteredText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun CurrentTilesGrid(
    listState: EditTileListState,
    selectionState: MutableSelectionState,
    onEditAction: (EditAction) -> Unit,
    iconTilesViewModel: IconTilesViewModel,
) {
    val currentListState by rememberUpdatedState(listState)
    val density = LocalDensity.current
    val totalRows = listState.tiles.lastOrNull()?.row ?: 0
    val totalHeight by
        animateDpAsState(
            gridHeight(totalRows + 1, TileHeight, TileArrangementPadding, EditModeTileDefaults.CurrentTilesGridPadding),
            label = "QSEditCurrentTilesGridHeight",
        )
    val gridState = rememberLazyGridState()
    var gridContentOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    val coroutineScope = rememberCoroutineScope()

    val primaryColor = MaterialTheme.colorScheme.primary
    val cellSize = TileHeight
    val spacing = TileArrangementPadding

    Box(Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height { totalHeight.roundToPx() }
                .border(
                    width = 2.dp,
                    color = primaryColor,
                    shape = RoundedCornerShape(EditModeTileDefaults.GridBackgroundCornerRadius),
                )
                .dragAndDropTileList(gridState, { gridContentOffset }, listState) { spec ->
                    onEditAction(EditAction.SetTiles(currentListState.tileSpecs()))
                    selectionState.select(spec)
                }
                .onGloballyPositioned { coordinates ->
                    gridContentOffset = coordinates.positionInRoot()
                }
                .drawBehind {
                    drawRoundRect(
                        primaryColor,
                        cornerRadius = CornerRadius(EditModeTileDefaults.GridBackgroundCornerRadius.toPx()),
                        alpha = .15f,
                    )
                }
                .sysuiResTag(CURRENT_TILES_GRID_TEST_TAG)
        ) {
            val totalWidth = maxWidth
            
            if (listState.dragInProgress || selectionState.placementEnabled) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = 2.dp.toPx()
                    val color = Color.Gray.copy(alpha = 0.3f)
                    
                    for (r in 0 until (totalRows + 1)) { 
                        for (c in 0 until listState.columns) {
                            val x = (cellSize.toPx() + spacing.toPx()) * c + (cellSize.toPx() / 2)
                            val y = (cellSize.toPx() + spacing.toPx()) * r + (cellSize.toPx() / 2)
                            drawCircle(color, radius, androidx.compose.ui.geometry.Offset(x, y))
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(EditModeTileDefaults.CurrentTilesGridPadding)
            ) {
                EditTiles(
                    listState = listState,
                    selectionState = selectionState,
                    gridState = gridState,
                    coroutineScope = coroutineScope,
                    iconTilesViewModel = iconTilesViewModel,
                    onRemoveTile = { onEditAction(EditAction.RemoveTile(it)) },
                    onResize = { spec, w, h -> onEditAction(EditAction.ResizeTileGrid(spec, w, h)) },
                    totalWidth = totalWidth,
                    cellSize = cellSize,
                    spacing = spacing,
                    density = density
                )
            }
        }
    }
}

data class GridPlacement(val gridX: Int, val gridY: Int, val spanX: Int, val spanY: Int)

private fun calculateGridPositions(tiles: List<TileGridCell>, columns: Int): List<GridPlacement> {
    val placements = mutableListOf<GridPlacement>()
    val occupied = mutableSetOf<Pair<Int, Int>>()
    
    tiles.forEach { cell ->
        val spanCols = cell.tile.spanCols
        val spanRows = cell.tile.spanRows
        var placed = false
        var searchRow = 0
        
        while (!placed) {
            for (col in 0 until columns) {
                if (col + spanCols > columns) continue
                
                var fits = true
                for (r in 0 until spanRows) {
                    for (c in 0 until spanCols) {
                        if (occupied.contains((searchRow + r) to (col + c))) {
                            fits = false
                            break
                        }
                    }
                    if (!fits) break
                }
                
                if (fits) {
                    placements.add(GridPlacement(col, searchRow, spanCols, spanRows))
                    for (r in 0 until spanRows) {
                        for (c in 0 until spanCols) {
                            occupied.add((searchRow + r) to (col + c))
                        }
                    }
                    placed = true
                    break
                }
            }
            if (!placed) searchRow++
        }
    }
    return placements
}

@Composable
private fun gridHeight(rows: Int, tileHeight: Dp, tilePadding: Dp, gridPadding: Dp): Dp {
    return ((tileHeight + tilePadding) * rows) + gridPadding * 2
}

@Composable
private fun EditTiles(
    listState: EditTileListState,
    selectionState: MutableSelectionState,
    gridState: LazyGridState,
    coroutineScope: CoroutineScope,
    onRemoveTile: (TileSpec) -> Unit,
    iconTilesViewModel: IconTilesViewModel,
    onResize: (TileSpec, Int, Int) -> Unit,
    totalWidth: Dp,
    cellSize: Dp,
    spacing: Dp,
    density: Density,
) {
    val tileCells = listState.tiles.filterIsInstance<TileGridCell>()
    val placements = remember(tileCells, listState.columns) {
        calculateGridPositions(tileCells, listState.columns)
    }
    
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragStartGlobalOffset by remember { mutableStateOf(Offset.Zero) }
    var totalDragOffset by remember { mutableStateOf(Offset.Zero) }
    
    tileCells.forEachIndexed { index, cell ->
        key(cell.tile.tileSpec.spec) {
            val placement = placements.getOrElse(index) { GridPlacement(0, 0, 1, 1) }
            val tileState by rememberTileState(cell.tile, selectionState)
            
            val xPos = (cellSize + spacing) * placement.gridX
            val yPos = (cellSize + spacing) * placement.gridY
            val xPosPx = with(density) { xPos.toPx() }
            val yPosPx = with(density) { yPos.toPx() }
            
            val isDragging = index == draggingIndex
            
            val targetOffset = if (isDragging) {
                IntOffset(
                    (dragStartGlobalOffset.x + totalDragOffset.x).roundToInt(),
                    (dragStartGlobalOffset.y + totalDragOffset.y).roundToInt()
                )
            } else {
                IntOffset(xPosPx.roundToInt(), yPosPx.roundToInt())
            }

            val animationSpec = if (isDragging) {
                snap<IntOffset>()
            } else {
                androidx.compose.animation.core.spring<IntOffset>(
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            }
            
            val animatedOffset by animateIntOffsetAsState(
                targetValue = targetOffset,
                animationSpec = animationSpec,
                label = "tile_offset"
            )

            val zIndex = if (isDragging) 10f else 0f
            val scale = if (isDragging) 1.05f else 1f
            val alpha = if (isDragging) 0.9f else 1f

            val targetWidth = (cellSize * cell.tile.spanCols) + (spacing * (cell.tile.spanCols - 1))
            val targetHeight = (cellSize * cell.tile.spanRows) + (spacing * (cell.tile.spanRows - 1))
            
            val animatedWidth by animateDpAsState(targetValue = targetWidth, label = "tile_width")
            val animatedHeight by animateDpAsState(targetValue = targetHeight, label = "tile_height")

            Box(
                modifier = Modifier
                    .offset { animatedOffset }
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .zIndex(zIndex)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                TileGridCell(
                    cell = cell,
                    index = index,
                    iconTilesViewModel = iconTilesViewModel,
                    dragAndDropState = listState,
                    selectionState = selectionState,
                    gridState = gridState,
                    onResize = onResize,
                    onRemoveTile = onRemoveTile,
                    coroutineScope = coroutineScope,
                    onDragStart = {
                        draggingIndex = tileCells.indexOfFirst { it.tile.tileSpec == cell.tile.tileSpec }
                        dragStartGlobalOffset = Offset(xPosPx, yPosPx)
                        totalDragOffset = Offset.Zero
                    },
                    onDragEnd = {
                        draggingIndex = -1
                        totalDragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragOffset += dragAmount
                        
                        val currentCenterX = dragStartGlobalOffset.x + totalDragOffset.x + 
                            with(density) { targetWidth.toPx() / 2 }
                        val currentCenterY = dragStartGlobalOffset.y + totalDragOffset.y + 
                            with(density) { targetHeight.toPx() / 2 }
                        
                        val targetIndex = placements.indexOfFirst { p ->
                            val px = with(density) { ((cellSize + spacing) * p.gridX).toPx() }
                            val py = with(density) { ((cellSize + spacing) * p.gridY).toPx() }
                            val pw = with(density) { ((cellSize * p.spanX) + (spacing * (p.spanX - 1))).toPx() }
                            val ph = with(density) { ((cellSize * p.spanY) + (spacing * (p.spanY - 1))).toPx() }
                            
                            currentCenterX in px..(px + pw) && currentCenterY in py..(py + ph)
                        }

                        if (targetIndex != -1 && targetIndex != draggingIndex) {
                            listState.onTargeting(targetIndex, false)
                            draggingIndex = targetIndex
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberTileState(
    tile: EditTileViewModel,
    selectionState: MutableSelectionState,
): State<TileState> {
    val tileState = remember { mutableStateOf(TileState.None) }

    LaunchedEffect(selectionState.selection, selectionState.placementEnabled, tile.isRemovable) {
        tileState.value =
           selectionState.tileStateFor(tile.tileSpec, tileState.value, tile.isRemovable)
    }
    return tileState
}

@Composable
private fun TileGridCell(
    cell: TileGridCell,
    index: Int,
    iconTilesViewModel: IconTilesViewModel,
    dragAndDropState: DragAndDropState,
    selectionState: MutableSelectionState,
    gridState: LazyGridState,
    onResize: (TileSpec, Int, Int) -> Unit,
    onRemoveTile: (TileSpec) -> Unit,
    coroutineScope: CoroutineScope,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val stateDescription = stringResource(id = R.string.accessibility_qs_edit_position, index + 1)
    val tileState by rememberTileState(cell.tile, selectionState)
    val onDragToMainQS = LocalTileDragHandler.current
    var isDraggingToMainQS by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val rowSpanHeight = remember(cell.tile.spanRows, density) {
        with(density) {
            (TileHeight.toPx() * cell.tile.spanRows + TileArrangementPadding.toPx() * (cell.tile.spanRows - 1)).toDp()
        }
    }

    val tileShape = RoundedCornerShape(InactiveCornerRadius)
    val colors = EditModeTileDefaults.editTileColors()

    val placeableColor = MaterialTheme.colorScheme.primary.copy(alpha = .4f)
    val backgroundColor by
        animateColorAsState(
            if (tileState == TileState.Placeable) placeableColor else colors.background
        )

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDrag by rememberUpdatedState(onDrag)
    
    val draggableModifier = Modifier.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                if (onDragToMainQS != null) {
                    onDragToMainQS(cell.tile.tileSpec)
                    isDraggingToMainQS = true
                } else {
                    dragAndDropState.onStarted(
                        SizedTileImpl(cell.tile, cell.width),
                        DragType.Move
                    )
                    currentOnDragStart()
                }
            },
            onDragEnd = {
                if (onDragToMainQS == null) {
                    dragAndDropState.onDrop()
                    currentOnDragEnd()
                } else {
                    isDraggingToMainQS = false
                }
            },
            onDragCancel = {
                if (onDragToMainQS == null) {
                    dragAndDropState.onDrop()
                    currentOnDragEnd()
                } else {
                    isDraggingToMainQS = false
                }
            },
            onDrag = { change: PointerInputChange, amount: Offset ->
                if (!isDraggingToMainQS) {
                    currentOnDrag(change, amount)
                }
            }
        )
    }

    val placeTileLabel = stringResource(R.string.accessibility_qs_edit_place_tile_action)
    val toggleSelectionLabel = stringResource(R.string.accessibility_qs_edit_toggle_selection)
    val tileAlpha = 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowSpanHeight)
            .clearAndSetSemantics {
                this.stateDescription = stateDescription
                contentDescription = cell.tile.label.text
                
                val actions = mutableListOf<CustomAccessibilityAction>()
                if (selectionState.placementEnabled) {
                     actions.add(
                        CustomAccessibilityAction(placeTileLabel) {
                            selectionState.placeTileAt(cell.tile.tileSpec)
                            true
                        }
                    )
                } else {
                     actions.add(
                        CustomAccessibilityAction(toggleSelectionLabel) {
                            selectionState.toggleSelection(cell.tile.tileSpec)
                            true
                        }
                    )
                }
                customActions = actions
            }
            .selectableTile(cell.tile.tileSpec, selectionState)
            .then(draggableModifier)
            .graphicsLayer { alpha = tileAlpha }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor,
            shape = tileShape,
            shadowElevation = if (tileState == TileState.Selected) 4.dp else 0.dp
        ) {
            EditTile(
                tile = cell.tile,
                tileState = tileState,
                progress = { if (cell.isIcon) 0f else 1f },
            )
        }

        if (tileState == TileState.Selected) {
            val currentSize = iconTilesViewModel.getTileSize(cell.tile.tileSpec)
            
            ResizeHandle(
                currentCols = currentSize.first,
                currentRows = currentSize.second,
                onResize = { newCols, newRows ->
                    onResize(cell.tile.tileSpec, newCols, newRows)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
        
        if (cell.tile.isRemovable && (tileState == TileState.Selected || tileState == TileState.Removable)) {
            Surface(
                onClick = { onRemoveTile(cell.tile.tileSpec) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

@Composable
private fun ResizeHandle(
    currentCols: Int,
    currentRows: Int,
    onResize: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val stepPx = with(density) { (TileHeight + TileArrangementPadding).toPx() }
    
    var accumulatedDragX by remember { mutableStateOf(0f) }
    var accumulatedDragY by remember { mutableStateOf(0f) }
    var hasMoved by remember { mutableStateOf(false) }
    val touchSlop = 20f
    
    Box(
        modifier = modifier
            .size(40.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        accumulatedDragX = 0f
                        accumulatedDragY = 0f
                        hasMoved = false
                    },
                    onDragEnd = {
                        accumulatedDragX = 0f
                        accumulatedDragY = 0f
                        hasMoved = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragX += dragAmount.x
                        accumulatedDragY += dragAmount.y
                        
                        if (!hasMoved) {
                            val dist = (accumulatedDragX * accumulatedDragX) + 
                                      (accumulatedDragY * accumulatedDragY)
                            if (dist > touchSlop * touchSlop) {
                                hasMoved = true
                            }
                        }

                        if (hasMoved) {
                            val threshold = stepPx * 0.75f
                            
                            val colChange = if (abs(accumulatedDragX) > threshold) 
                                (accumulatedDragX / threshold).toInt() else 0
                            val rowChange = if (abs(accumulatedDragY) > threshold) 
                                (accumulatedDragY / threshold).toInt() else 0
                            
                            if (colChange != 0 || rowChange != 0) {
                                val newCols = (currentCols + colChange).coerceIn(1, 4)
                                val newRows = (currentRows + rowChange).coerceIn(1, 3)
                                
                                if (newCols != currentCols || newRows != currentRows) {
                                    onResize(newCols, newRows)
                                    if (newCols != currentCols) accumulatedDragX = 0f
                                    if (newRows != currentRows) accumulatedDragY = 0f
                                }
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Resize",
            modifier = Modifier
                .padding(8.dp)
                .rotate(45f)
                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                .padding(4.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun EditTile(
    tile: EditTileViewModel,
    tileState: TileState,
    progress: () -> Float,
    colors: TileColors = EditModeTileDefaults.editTileColors(),
) {
    val iconSizeDiff = CommonTileDefaults.IconSize - CommonTileDefaults.LargeTileIconSize
    val containerAlpha by animateFloatAsState(1f) // Removed greyed out dimming completely
    Row(
        horizontalArrangement = spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.layout { measurable, constraints ->
                    val width = constraints.maxWidth
                    val placeable =
                        measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    
                    val currentProgress = progress()
                    val startPadding = if (currentProgress == 0f) {
                         iconHorizontalCenter(constraints.maxWidth)
                    } else {
                         0f
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(startPadding.roundToInt(), 0)
                    }
                }
                .largeTilePadding()
                .graphicsLayer { this.alpha = containerAlpha },
    ) {
        Box(
            Modifier.size(ToggleTargetSize).thenIf(tile.isDualTarget) {
                Modifier.drawBehind { drawCircle(colors.iconBackground, alpha = progress()) }
            }
        ) {
            SmallTileContent(
                iconProvider = { tile.icon },
                color = colors.icon,
                animateToEnd = true,
                size = { CommonTileDefaults.IconSize - iconSizeDiff * progress() },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        LargeTileLabels(
            label = tile.label.text,
            secondaryLabel = tile.appName?.text,
            colors = colors,
            modifier = Modifier.weight(1f).graphicsLayer { this.alpha = progress() },
        )
    }
}

private fun MeasureScope.iconHorizontalCenter(containerSize: Int): Float {
    return (containerSize - ToggleTargetSize.roundToPx()) / 2f -
        CommonTileDefaults.TileStartPadding.toPx()
}

private fun Modifier.tileBackground(color: () -> Color): Modifier {
    return clip(RoundedCornerShape(InactiveCornerRadius)).drawBehind { drawRect(color()) }
}
