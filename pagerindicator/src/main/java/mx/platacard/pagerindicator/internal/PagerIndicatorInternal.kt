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

@Composable
internal fun PagerIndicatorInternal(
    pageCount: Int,
    currentPageFraction: State<Float>,
    modifier: Modifier = Modifier,
    activeDotColor: Color,
    dotColor: Color,
    dotPainter: Painter = CirclePainter,
    dotCount: Int = 5,
    dotNormalSize: Dp = 6.dp,
    dotSelectedSize: Dp = 8.dp,
    dotMinSize: Dp = 4.dp,
    space: Dp = 8.dp,
    orientation: PagerIndicatorOrientation = Horizontal,
    onAfterDraw: DrawScope.() -> Unit = {},
) {

    val adjustedDotCount = minOf(dotCount, pageCount)

    val density = LocalDensity.current

    val dotMinSizePx = with(density) { dotMinSize.toPx() }
    val dotNormalSizePx = with(density) { dotNormalSize.toPx() }
    val dotSizePx = with(density) { dotSelectedSize.toPx() }

    val spacePx = with(density) { space.toPx() }

    fun calculateTargetDotStateForPage(i: Int, page: Int): DotState {
        return calculateTargetDotSizeForPage(
            i = i,
            currentPage = page,
            pageCount = pageCount,
            dotCount = adjustedDotCount,
        )
    }

    fun DotState.size(): Float {
        return when (this) {
            DotState.Selected -> dotSizePx
            DotState.Normal -> dotNormalSizePx
            DotState.SmallEdge -> dotMinSizePx
            DotState.Invisible -> 0f
        }
    }

    fun DotState.color(): Color {
        return when (this) {
            DotState.Selected -> activeDotColor
            else -> dotColor
        }
    }

    val mainAxisSize = dotSelectedSize * adjustedDotCount + space * (adjustedDotCount - 1)

    Canvas(
        modifier = modifier
            .width(if (orientation == Horizontal) mainAxisSize else dotSelectedSize)
            .height(if (orientation == Horizontal) dotSelectedSize else mainAxisSize)
    ) {

        val centerDot = pageCount / 2

        val pagerFraction = currentPageFraction.value
        val pagesFromCenter = pagerFraction - centerDot

        val additionalOffset = if (pageCount % 2 == 0 && adjustedDotCount != pageCount) 1 else 0

        val mainAxisCenterX = if (orientation == Horizontal) center.x else center.y

        val firstItemStart = mainAxisCenterX - (pageCount / 2f + additionalOffset) * dotSizePx -
                ((pageCount - 1 - additionalOffset) / 2f) * spacePx

        val scroll = when {
            adjustedDotCount == pageCount -> 0f
            else -> -pagesFromCenter.coerceIn(
                minimumValue = (adjustedDotCount / 2 - centerDot).toFloat(),
                maximumValue = (-adjustedDotCount / 2 + centerDot - additionalOffset).toFloat(),
            ) * (dotSizePx + spacePx)
        } + firstItemStart

        val (firstVisible, lastVisible) = calculateDrawableDotIndices(
            dotCount = dotCount,
            currentPage = pagerFraction.roundToInt(),
            pageCount = pageCount,
        ).let { (first, second) ->
            (first - 1).coerceAtLeast(0) to (second + 1).coerceAtMost(pageCount - 1)
        }

        translate(
            left = if (orientation == Horizontal) scroll else 0f,
            top = if (orientation == Vertical) scroll else 0f,
        ) {

            for (i in firstVisible..lastVisible) {

                val dotStart = i * (dotSizePx + spacePx)

                val pagerFractionInt = pagerFraction.toInt()
                val scrollFraction = pagerFraction - pagerFractionInt

                val currentDotState = calculateTargetDotStateForPage(i, page = pagerFractionInt)
                val futureDotState = calculateTargetDotStateForPage(i, page = pagerFractionInt + 1)

                val targetDotSize =
                    lerp(currentDotState.size(), futureDotState.size(), scrollFraction)
                val targetDotColor =
                    lerp(currentDotState.color(), futureDotState.color(), scrollFraction)
                val dotColorFilter = ColorFilter.tint(targetDotColor)

                val mainAxisStart = dotStart + dotSizePx / 2f - targetDotSize / 2f
                val crossAxisStart = dotSizePx / 2f - targetDotSize / 2f

                val left = if (orientation == Horizontal) mainAxisStart else crossAxisStart
                val top = if (orientation == Horizontal) crossAxisStart else mainAxisStart

                with(dotPainter) {
                    translate(
                        left = left,
                        top = top,
                    ) {
                        if (intrinsicSize == Size.Unspecified) {
                            draw(
                                size = Size(targetDotSize, targetDotSize),
                                colorFilter = dotColorFilter,
                            )
                        } else {
                            scale(
                                scaleX = targetDotSize / intrinsicSize.width,
                                scaleY = targetDotSize / intrinsicSize.height,
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
            val (start, stop) = calculateDrawableDotIndices(
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
internal fun warmLinePosAsState(
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
