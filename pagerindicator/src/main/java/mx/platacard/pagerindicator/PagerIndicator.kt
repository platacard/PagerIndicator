package mx.platacard.pagerindicator

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.platacard.pagerindicator.PagerIndicatorOrientation.Horizontal
import kotlinx.coroutines.launch
import mx.platacard.pagerindicator.internal.CirclePainter
import mx.platacard.pagerindicator.internal.PagerIndicatorInternal
import mx.platacard.pagerindicator.internal.onDotClick

enum class PagerIndicatorOrientation {
    Horizontal,
    Vertical,
}

/**
 * A simple pager indicator that displays dots for each page in the pager.
 *
 * @param pagerState The [PagerState] that this indicator should represent.
 * @param activeDotColor Color of the active dot.
 * @param dotColor Color of the inactive dots.
 * @param modifier Modifier to apply to the layout.
 * @param dotCount Number of dots to display.
 * @param dotPainter Painter to use for drawing the dots.
 * @param normalDotSize Size of the inactive dots.
 * @param activeDotSize Size of the active dot.
 * @param minDotSize Size of the dot displayed on the edge
 * @param space Space between the dots.
 * @param orientation Orientation of the pager.
 */
@Composable
fun PagerIndicator(
    pagerState: PagerState,
    activeDotColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier,
    dotCount: Int = 5,
    dotPainter: Painter = CirclePainter,
    normalDotSize: Dp = 6.dp,
    activeDotSize: Dp = 8.dp,
    minDotSize: Dp = 4.dp,
    space: Dp = 8.dp,
    orientation: PagerIndicatorOrientation = Horizontal,
) {

    val scope = rememberCoroutineScope()

    PagerIndicator(
        modifier = modifier,
        pageCount = pagerState.pageCount,
        currentPageFraction = remember {
            derivedStateOf {
                pagerState.currentPage + pagerState.currentPageOffsetFraction
            }
        },
        dotPainter = dotPainter,
        dotCount = dotCount,
        normalDotSize = normalDotSize,
        activeDotSize = activeDotSize,
        minDotSize = minDotSize,
        space = space,
        activeDotColor = activeDotColor,
        dotColor = dotColor,
        orientation = orientation,
        onDotClick = { scope.launch { pagerState.animateScrollToPage(it, 0f) } },
    )
}

/**
 * A simple pager indicator that displays dots for each page in the pager.
 *
 * @param pageCount The number of pages in the pager.
 * @param currentPageFraction A lambda that returns the fraction [0, pageCount] of the current page based on the scrolling position
 * @param activeDotColor Color of the active dot.
 * @param dotColor Color of the inactive dots.
 * @param modifier Modifier to apply to the layout.
 * @param dotCount Number of dots to display.
 * @param dotPainter Painter to use for drawing the dots.
 * @param normalDotSize Size of the inactive dots.
 * @param activeDotSize Size of the active dot.
 * @param minDotSize Size of the dot displayed on the edge
 * @param space Space between the dots.
 * @param orientation Orientation of the pager.
 * @param onDotClick Lambda that is called when a dot is clicked.
 */
@Composable
fun PagerIndicator(
    pageCount: Int,
    currentPageFraction: State<Float>,
    activeDotColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier,
    dotPainter: Painter = CirclePainter,
    dotCount: Int = 5,
    normalDotSize: Dp = 6.dp,
    activeDotSize: Dp = 8.dp,
    minDotSize: Dp = 4.dp,
    space: Dp = 8.dp,
    orientation: PagerIndicatorOrientation = Horizontal,
    onDotClick: (Int) -> Unit = {},
) {
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
        activeDotColor = activeDotColor,
        dotColor = dotColor,
        dotPainter = dotPainter,
        dotCount = dotCount,
        normalDotSize = normalDotSize,
        activeDotSize = activeDotSize,
        minDotSize = minDotSize,
        space = space,
        orientation = orientation,
    )
}
