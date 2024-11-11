package mx.platacard.pagerindicator

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.platacard.pagerindicator.PagerIndicatorOrientation.Horizontal
import mx.platacard.pagerindicator.PagerIndicatorOrientation.Vertical
import kotlinx.coroutines.launch
import mx.platacard.pagerindicator.internal.PagerIndicatorInternal
import mx.platacard.pagerindicator.internal.onDotClick
import mx.platacard.pagerindicator.internal.wormLinePosAsState

/**
 * A simple pager indicator that displays dots for each page in the pager.
 * The transition between pages is animated in the style of a worm.
 *
 * @param pagerState The [PagerState] that this indicator should represent.
 * @param modifier Modifier to apply to the layout.
 * @param activeDotColor Color of the active dot.
 * @param dotColor Color of the inactive dots.
 * @param dotCount Number of dots to display.
 * @param activeDotSize Size of the active dot.
 * @param minDotSize Size of the dot displayed on the edge
 * @param space Space between the dots.
 * @param orientation Orientation of the pager.
 */
@Composable
fun PagerWormIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeDotSize: Dp = 8.dp,
    minDotSize: Dp = 4.dp,
    space: Dp = 8.dp,
    dotCount: Int = 5,
    activeDotColor: Color,
    dotColor: Color,
    orientation: PagerIndicatorOrientation = Horizontal,
) {

    val scope = rememberCoroutineScope()

    PagerWormIndicator(
        pageCount = pagerState.pageCount,
        currentPageFraction = remember {
            derivedStateOf {
                pagerState.currentPage + pagerState.currentPageOffsetFraction
            }
        },
        modifier = modifier,
        activeDotSize = activeDotSize,
        minDotSize = minDotSize,
        space = space,
        dotCount = dotCount,
        activeDotColor = activeDotColor,
        dotColor = dotColor,
        orientation = orientation,
        onDotClick = { scope.launch { pagerState.animateScrollToPage(it, 0f) } },
    )
}

/**
 * A simple pager indicator that displays dots for each page in the pager.
 * The transition between pages is animated in the style of a worm.
 *
 * @param pageCount The total number of pages in the pager.
 * @param currentPageFraction The fraction of the current page.
 * @param modifier Modifier to apply to the layout.
 * @param activeDotColor Color of the active dot.
 * @param dotColor Color of the inactive dots.
 * @param dotCount Number of dots to display.
 * @param activeDotSize Size of the active dot.
 * @param minDotSize Size of the dot displayed on the edge
 * @param space Space between the dots.
 * @param orientation Orientation of the pager.
 * @param onDotClick Callback when a dot is clicked.
 */
@Composable
fun PagerWormIndicator(
    pageCount: Int,
    currentPageFraction: State<Float>,
    modifier: Modifier = Modifier,
    activeDotSize: Dp = 8.dp,
    minDotSize: Dp = 4.dp,
    space: Dp = 8.dp,
    dotCount: Int = 5,
    activeDotColor: Color,
    dotColor: Color,
    orientation: PagerIndicatorOrientation = Horizontal,
    onDotClick: (Int) -> Unit = {},
) {

    val wormLinePos = wormLinePosAsState(currentPageFraction)

    val density = LocalDensity.current
    val dotSizePx = with(density) { activeDotSize.toPx() }
    val spacePx = with(density) { space.toPx() }

    PagerIndicatorInternal(
        pageCount = pageCount,
        currentPageFraction = currentPageFraction,
        modifier = modifier.onDotClick(
            dotCount = dotCount,
            pageCount = pageCount,
            currentPageFraction = currentPageFraction,
            onClick = onDotClick,
            orientation = orientation,
        ),
        activeDotColor = dotColor,
        dotColor = dotColor,
        dotCount = dotCount,
        normalDotSize = activeDotSize,
        activeDotSize = activeDotSize,
        minDotSize = minDotSize,
        space = space,
        orientation = orientation,
        onAfterDraw = {

            val lineStart = wormLinePos.value.first * (dotSizePx + spacePx)
            val lineEnd = wormLinePos.value.second * (dotSizePx + spacePx)

            val lineStartX = when (orientation) {
                Horizontal -> lineStart + dotSizePx / 2f
                Vertical -> dotSizePx / 2f
            }

            val lineStartY = when (orientation) {
                Horizontal -> dotSizePx / 2f
                Vertical -> lineStart + dotSizePx / 2f
            }

            val lineEndX = when (orientation) {
                Horizontal -> lineEnd + dotSizePx / 2f
                Vertical -> dotSizePx / 2f
            }

            val lineEndY = when (orientation) {
                Horizontal -> dotSizePx / 2f
                Vertical -> lineEnd + dotSizePx / 2f
            }

            drawLine(
                color = activeDotColor,
                start = Offset(lineStartX, lineStartY),
                end = Offset(lineEndX, lineEndY),
                strokeWidth = dotSizePx,
                cap = Round,
            )
        }
    )
}
