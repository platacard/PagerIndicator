package mx.platacard.pagerindicator

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.platacard.pagerindicator.internal.PagerIndicatorInternal
import mx.platacard.pagerindicator.internal.PillPainter
import mx.platacard.pagerindicator.internal.calculateCenteredDotStateForPage
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A pager indicator for auto-advancing pagers. The active page is displayed as a timer
 * that is filled while the page is shown.
 *
 * @param pagerState The [PagerState] that this indicator should represent.
 * @param timerState The [PagerTimerState] that drives the progress of the timer.
 * @param modifier Modifier to apply to the layout.
 * @param color Color of the timer progress.
 * @param inactiveAlpha Opacity of the inactive dots and the timer track.
 * @param dotCount Number of dots to display.
 * @param timerWidth Width of the timer.
 * @param timerHeight Height of the timer.
 * @param normalDotSize Size of the inactive dots.
 * @param minDotSize Size of the dot displayed on the edge
 * @param space Space between the dots.
 */
@Composable
fun PagerTimerIndicator(
    pagerState: PagerState,
    timerState: PagerTimerState,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    inactiveAlpha: Float = 0.3f,
    dotCount: Int = 5,
    timerWidth: Dp = 36.dp,
    timerHeight: Dp = 8.dp,
    normalDotSize: Dp = 6.dp,
    minDotSize: Dp = 4.dp,
    space: Dp = 8.dp,
) {
    PagerTimerIndicator(
        pageCount = pagerState.pageCount,
        currentPageFraction = remember(pagerState) {
            derivedStateOf {
                pagerState.currentPage + pagerState.currentPageOffsetFraction
            }
        },
        currentPage = pagerState.targetPage,
        progress = { timerState.progress },
        modifier = modifier,
        color = color,
        inactiveAlpha = inactiveAlpha,
        dotCount = dotCount,
        timerWidth = timerWidth,
        timerHeight = timerHeight,
        normalDotSize = normalDotSize,
        minDotSize = minDotSize,
        space = space,
    )
}

/**
 * A pager indicator for auto-advancing pagers. The active page is displayed as a timer
 * that is filled while the page is shown.
 *
 * @param pageCount The number of pages in the pager.
 * @param currentPageFraction The fraction [0, pageCount] of the current page based on the scrolling position
 * @param progress A lambda that returns the progress [0, 1] of the current page timer.
 * @param modifier Modifier to apply to the layout.
 * @param color Color of the timer progress.
 * @param inactiveAlpha Opacity of the inactive dots and the timer track.
 * @param dotCount Number of dots to display.
 * @param timerWidth Width of the timer.
 * @param timerHeight Height of the timer.
 * @param normalDotSize Size of the inactive dots.
 * @param minDotSize Size of the dot displayed on the edge
 * @param space Space between the dots.
 */
@Composable
fun PagerTimerIndicator(
    pageCount: Int,
    currentPageFraction: State<Float>,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    inactiveAlpha: Float = 0.3f,
    dotCount: Int = 5,
    timerWidth: Dp = 36.dp,
    timerHeight: Dp = 8.dp,
    normalDotSize: Dp = 6.dp,
    minDotSize: Dp = 4.dp,
    space: Dp = 8.dp,
) {
    val currentPage by remember(currentPageFraction) {
        derivedStateOf { currentPageFraction.value.roundToInt() }
    }

    PagerTimerIndicator(
        pageCount = pageCount,
        currentPageFraction = currentPageFraction,
        currentPage = currentPage,
        progress = progress,
        modifier = modifier,
        color = color,
        inactiveAlpha = inactiveAlpha,
        dotCount = dotCount,
        timerWidth = timerWidth,
        timerHeight = timerHeight,
        normalDotSize = normalDotSize,
        minDotSize = minDotSize,
        space = space,
    )
}

@Composable
private fun PagerTimerIndicator(
    pageCount: Int,
    currentPageFraction: State<Float>,
    currentPage: Int,
    progress: () -> Float,
    modifier: Modifier,
    color: Color,
    inactiveAlpha: Float,
    dotCount: Int,
    timerWidth: Dp,
    timerHeight: Dp,
    normalDotSize: Dp,
    minDotSize: Dp,
    space: Dp,
) {
    if (pageCount < 2) return

    val inactiveColor = color.copy(alpha = color.alpha * inactiveAlpha)

    // The page that is left keeps its progress while its timer collapses into a dot,
    // even though the progress is already reset for the new page.
    val lastDrawn = remember { DrawnProgress(currentPage) }
    val previous = remember(currentPage) { lastDrawn.copy() }

    PagerIndicatorInternal(
        pageCount = pageCount,
        currentPageFraction = currentPageFraction,
        modifier = modifier,
        activeDotColor = inactiveColor,
        dotColor = inactiveColor,
        dotPainter = { PillPainter },
        dotCount = dotCount,
        normalDotSize = normalDotSize,
        activeDotSize = timerHeight,
        minDotSize = minDotSize,
        space = space,
        activeDotMainSize = timerWidth,
        packed = true,
        dotState = ::calculateCenteredDotStateForPage,
        onDrawDot = { page, topLeft, size, selection ->
            val pageProgress = when (page) {
                currentPage -> progress().coerceIn(0f, 1f).also {
                    lastDrawn.page = page
                    lastDrawn.progress = it
                }
                previous.page -> previous.progress
                else -> 0f
            }
            drawTimerProgress(topLeft, size, pageProgress, color, alpha = selection)
        },
    )
}

private fun DrawScope.drawTimerProgress(
    topLeft: Offset,
    size: Size,
    progress: Float,
    color: Color,
    alpha: Float,
) {
    val progressWidth = size.width * progress
    if (progressWidth <= 0f || alpha <= 0f) return

    val radius = CornerRadius(size.height / 2f)
    val track = Path().apply {
        addRoundRect(RoundRect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height, radius))
    }
    // The progress is a pill that slides in from the left, so its right end is always rounded
    // and it stays inside the track while it is narrower than its height.
    val pillWidth = max(progressWidth, size.height)
    clipPath(track) {
        drawRoundRect(
            color = color,
            topLeft = Offset(topLeft.x + progressWidth - pillWidth, topLeft.y),
            size = Size(pillWidth, size.height),
            cornerRadius = radius,
            alpha = alpha,
        )
    }
}

private class DrawnProgress(var page: Int, var progress: Float = 0f) {
    fun copy() = DrawnProgress(page, progress)
}
