package mx.platacard.pagerindicator.internal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import mx.platacard.pagerindicator.PagerIndicatorOrientation
import mx.platacard.pagerindicator.PagerIndicatorOrientation.*
import kotlin.math.roundToInt

internal enum class DotState {
    Normal,
    Selected,
    SmallEdge,
    Invisible,
}

/**
 * @param activeDotMainSize Size of the active dot along the main axis, [activeDotSize] is its cross axis size.
 * @param packed If `false`, every dot takes a slot of [activeDotSize] and the window is scrolled.
 * If `true`, dots take as much space as they need, hidden dots take none, and the visible group is centered.
 * @param dotState State of the dot [i] when the [currentPage] is selected.
 * @param onDrawDot Called after the dot of the [page] is drawn. `selection` is 1 for the selected dot,
 * 0 for the others and in between while the pager is scrolled.
 */
@Composable
internal fun PagerIndicatorInternal(
    pageCount: Int,
    currentPageFraction: State<Float>,
    modifier: Modifier = Modifier,
    activeDotColor: Color,
    dotColor: Color,
    dotPainter: (Int) -> Painter = { CirclePainter },
    dotCount: Int = 5,
    normalDotSize: Dp = 6.dp,
    activeDotSize: Dp = 8.dp,
    minDotSize: Dp = 4.dp,
    space: Dp = 8.dp,
    orientation: PagerIndicatorOrientation = Horizontal,
    activeDotMainSize: Dp = activeDotSize,
    packed: Boolean = false,
    dotState: ((i: Int, currentPage: Int, pageCount: Int, dotCount: Int) -> DotState) = ::calculateTargetDotSizeForPage,
    onDrawDot: (DrawScope.(page: Int, topLeft: Offset, size: Size, selection: Float) -> Unit)? = null,
    onAfterDraw: DrawScope.() -> Unit = {},
) {

    val adjustedDotCount = when {
        dotCount >= pageCount -> pageCount
        else -> if (dotCount % 2 == 0) dotCount - 1 else dotCount
    }

    val density = LocalDensity.current

    val dotMinSizePx = with(density) { minDotSize.toPx() }
    val dotNormalSizePx = with(density) { normalDotSize.toPx() }
    val dotSizePx = with(density) { activeDotSize.toPx() }
    val dotMainSizePx = with(density) { activeDotMainSize.toPx() }

    val spacePx = with(density) { space.toPx() }

    fun calculateTargetDotStateForPage(i: Int, page: Int): DotState {
        return dotState(i, page, pageCount, adjustedDotCount)
    }

    // Size across the main axis.
    fun DotState.size(): Float {
        return when (this) {
            DotState.Selected -> dotSizePx
            DotState.Normal -> dotNormalSizePx
            DotState.SmallEdge -> dotMinSizePx
            // A packed hidden dot keeps its size and fades out instead of shrinking.
            DotState.Invisible -> if (packed) dotMinSizePx else 0f
        }
    }

    // Size along the main axis.
    fun DotState.mainSize(): Float {
        return if (this == DotState.Selected) dotMainSizePx else size()
    }

    // Space that the dot takes along the main axis.
    fun DotState.slot(): Float {
        return when {
            !packed -> dotSizePx
            this == DotState.Invisible -> 0f
            else -> mainSize()
        }
    }

    fun DotState.space(): Float {
        return if (packed && this == DotState.Invisible) 0f else spacePx
    }

    fun DotState.alpha(): Float {
        return if (packed && this == DotState.Invisible) 0f else 1f
    }

    fun DotState.color(): Color {
        return when (this) {
            DotState.Selected -> activeDotColor
            else -> dotColor
        }
    }

    val mainAxisSize = if (packed) {
        activeDotMainSize + (normalDotSize + space) * (adjustedDotCount - 1)
    } else {
        activeDotSize * adjustedDotCount + space * (adjustedDotCount - 1)
    }

    Canvas(
        modifier = modifier
            .width(if (orientation == Horizontal) mainAxisSize else activeDotSize)
            .height(if (orientation == Horizontal) activeDotSize else mainAxisSize)
    ) {

        val pagerFraction = currentPageFraction.value

        val itemsCount = (pagerFraction - adjustedDotCount / 2)
            .coerceIn(
                minimumValue = 0f,
                maximumValue = pageCount - adjustedDotCount.toFloat(),
            )

        val (firstVisible, lastVisible) = calculateVisibleDotIndices(
            dotCount = adjustedDotCount,
            currentPage = pagerFraction.roundToInt(),
            pageCount = pageCount,
        ).let { (first, second) ->
            (first - 1).coerceAtLeast(0) to (second + 1).coerceAtMost(pageCount - 1)
        }

        val pagerFractionInt = pagerFraction.toInt()
        val scrollFraction = pagerFraction - pagerFractionInt

        val scroll = if (packed) {
            // Center the visible group.
            var groupSize = -spacePx
            for (i in firstVisible..lastVisible) {
                val current = calculateTargetDotStateForPage(i, page = pagerFractionInt)
                val future = calculateTargetDotStateForPage(i, page = pagerFractionInt + 1)
                groupSize += lerp(
                    current.slot() + current.space(),
                    future.slot() + future.space(),
                    scrollFraction,
                )
            }
            val canvasMainSize = if (orientation == Horizontal) size.width else size.height
            (canvasMainSize - groupSize) / 2f
        } else {
            -itemsCount * (dotSizePx + spacePx)
        }

        translate(
            left = if (orientation == Horizontal) scroll else 0f,
            top = if (orientation == Vertical) scroll else 0f,
        ) {

            var packedDotStart = 0f

            for (i in firstVisible..lastVisible) {

                val currentDotState = calculateTargetDotStateForPage(i, page = pagerFractionInt)
                val futureDotState = calculateTargetDotStateForPage(i, page = pagerFractionInt + 1)

                val slot = lerp(currentDotState.slot(), futureDotState.slot(), scrollFraction)
                val dotStart = if (packed) packedDotStart else i * (dotSizePx + spacePx)
                packedDotStart += slot +
                    lerp(currentDotState.space(), futureDotState.space(), scrollFraction)

                val targetDotSize =
                    lerp(currentDotState.size(), futureDotState.size(), scrollFraction)
                val targetDotMainSize =
                    lerp(currentDotState.mainSize(), futureDotState.mainSize(), scrollFraction)
                val targetDotAlpha =
                    lerp(currentDotState.alpha(), futureDotState.alpha(), scrollFraction)
                val targetDotColor =
                    lerp(currentDotState.color(), futureDotState.color(), scrollFraction)
                val dotColorFilter = ColorFilter.tint(
                    if (targetDotAlpha == 1f) {
                        targetDotColor
                    } else {
                        targetDotColor.copy(alpha = targetDotColor.alpha * targetDotAlpha)
                    }
                )

                val mainAxisStart = dotStart + slot / 2f - targetDotMainSize / 2f
                val crossAxisStart = dotSizePx / 2f - targetDotSize / 2f

                val left = if (orientation == Horizontal) mainAxisStart else crossAxisStart
                val top = if (orientation == Horizontal) crossAxisStart else mainAxisStart
                val dotSize = if (orientation == Horizontal) {
                    Size(targetDotMainSize, targetDotSize)
                } else {
                    Size(targetDotSize, targetDotMainSize)
                }

                with(dotPainter(i)) {
                    translate(
                        left = left,
                        top = top,
                    ) {
                        if (intrinsicSize == Size.Unspecified) {
                            draw(
                                size = dotSize,
                                colorFilter = dotColorFilter,
                            )
                        } else {
                            scale(
                                scaleX = dotSize.width / intrinsicSize.width,
                                scaleY = dotSize.height / intrinsicSize.height,
                                pivot = Offset(0f, 0f),
                            ) {
                                draw(
                                    size = intrinsicSize,
                                    colorFilter = dotColorFilter,
                                )
                            }
                        }
                    }
                }

                if (onDrawDot != null) {
                    val selection = lerp(
                        if (currentDotState == DotState.Selected) 1f else 0f,
                        if (futureDotState == DotState.Selected) 1f else 0f,
                        scrollFraction,
                    )
                    onDrawDot(i, Offset(left, top), dotSize, selection)
                }

                onAfterDraw()
            }
        }
    }
}

internal fun Modifier.onDotClick(
    dotCount: Int,
    pageCount: Int,
    currentPageFraction: State<Float>,
    onClick: (Int) -> Unit,
    orientation: PagerIndicatorOrientation,
): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures { offset ->
            val (start, stop) = calculateVisibleDotIndices(
                dotCount = dotCount,
                currentPage = currentPageFraction.value.roundToInt(),
                pageCount = pageCount,
            )

            val fraction = when (orientation) {
                Horizontal -> offset.x / size.width
                Vertical -> offset.y / size.height
            }

            val clickedIndex = lerp(start.toFloat(), stop.toFloat(), fraction).roundToInt()

            onClick(clickedIndex)
        }
    }
}

@Composable
internal fun wormLinePosAsState(
    currentPageFraction: State<Float>,
): State<Pair<Float, Float>> {
    return remember {

        var lastAnchor = currentPageFraction.value.toInt()

        derivedStateOf {
            val cur = currentPageFraction.value

            val pagerFractionInt = cur.toInt()
            val scrollFraction = cur - pagerFractionInt

            if (cur > lastAnchor) {
                if (cur >= lastAnchor + 1) {
                    lastAnchor = cur.roundToInt()
                }

                val start = lastAnchor + (2f * scrollFraction - 1f).coerceIn(0f, 1f)
                val end = lastAnchor + (scrollFraction * 2).coerceIn(0f, 1f)

                start to end
            } else if (cur < lastAnchor) {

                if (cur < lastAnchor - 1) {
                    lastAnchor = cur.roundToInt()
                }

                val start = lastAnchor - (1f - 2f * scrollFraction).coerceIn(0f, 1f)
                val end = lastAnchor - (2f - 2f * scrollFraction).coerceIn(0f, 1f)

                start to end
            } else {
                lastAnchor.toFloat() to lastAnchor.toFloat()
            }
        }
    }
}
