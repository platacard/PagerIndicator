package mx.platacard.pagerindicator

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What the timer does when the last slide is over.
 */
enum class PagerTimerEndBehavior {
    /** The timer stays at 100% on the last slide. */
    Stop,

    /** The pager scrolls to the first slide and the timer starts again. */
    Loop,
}

/**
 * Timer of an auto-advancing pager.
 *
 * The [progress] of the current slide grows linearly from 0 to 1 during the slide duration, then
 * the pager scrolls to the next slide and the progress starts from 0 again.
 *
 * The timer runs only while the slide is fully visible. It pauses automatically while the pager
 * is scrolled, while the content is pressed (see [pauseOnPress]), while the app is in background,
 * and it never auto-advances while a screen reader (TalkBack) is on.
 *
 * Create it with [rememberPagerTimerState].
 */
@Stable
class PagerTimerState internal constructor(
    private val pagerState: PagerState,
    slideDuration: (page: Int) -> Duration,
    endBehavior: PagerTimerEndBehavior,
) {

    /** Progress of the current slide in [0, 1]. */
    var progress: Float by mutableFloatStateOf(0f)
        private set

    /**
     * Pauses the timer, the progress continues from the same point after resume.
     *
     * Pause it while the slide content is loading, while a modal or a bottom sheet is open,
     * or while the carousel is off-screen.
     */
    var isPaused: Boolean by mutableStateOf(false)

    internal var isPressed: Boolean by mutableStateOf(false)
    internal var isScreenReaderOn: Boolean by mutableStateOf(false)
    internal var slideDuration: (page: Int) -> Duration by mutableStateOf(slideDuration)
    internal var endBehavior: PagerTimerEndBehavior by mutableStateOf(endBehavior)

    private var elapsedNanos = 0L

    // The page the progress belongs to, it follows the target page of the pager.
    private var timedPage = pagerState.targetPage

    // The last settled page and its progress at the moment the pager started to leave it.
    private var settledPage = pagerState.settledPage
    private var settledElapsedNanos = 0L
    private var settledProgress = 0f

    // An empty pager has no slide to time.
    private val isHeld: Boolean
        get() = isPaused || isPressed || isScreenReaderOn || pagerState.isScrollInProgress ||
            pagerState.pageCount == 0

    /** Starts the progress of the current slide from 0. */
    fun restart() {
        elapsedNanos = 0L
        progress = 0f
    }

    internal suspend fun run() = coroutineScope {
        launch {
            snapshotFlow { pagerState.targetPage to pagerState.settledPage }.collect { (target, settled) ->
                onPageChanged(target, settled)
            }
        }
        launch {
            snapshotFlow { !isHeld && progress < 1f }.collectLatest { running ->
                if (running) tick()
            }
        }
        // Keyed by the end behavior and the page count too: switching Stop -> Loop or adding a page
        // on a finished last slide must advance.
        snapshotFlow {
            if (!isHeld && progress >= 1f) endBehavior to pagerState.pageCount else null
        }.collect { finished ->
            if (finished != null) advance()
        }
    }

    // A drag towards a neighbour changes the target page before anything settles. The progress of
    // the settled page is kept, so a drag that snaps back continues it instead of starting over.
    private fun onPageChanged(target: Int, settled: Int) {
        if (settled != settledPage) {
            settledPage = settled
            settledElapsedNanos = 0L
            settledProgress = 0f
        }
        if (target == timedPage) return

        if (timedPage == settledPage) {
            settledElapsedNanos = elapsedNanos
            settledProgress = progress
        }
        timedPage = target
        // A finished slide starts over, otherwise coming back to it would advance at once.
        if (target == settledPage && settledProgress < 1f) {
            elapsedNanos = settledElapsedNanos
            progress = settledProgress
        } else {
            restart()
        }
    }

    private suspend fun tick() {
        var lastFrame = withFrameNanos { it }
        while (progress < 1f) {
            val frame = withFrameNanos { it }
            // A long gap between frames means nothing was drawn (e.g. the app was in background),
            // none of that time counts: the frame only becomes the new baseline.
            val gap = frame - lastFrame
            if (gap <= MaxFrameGapNanos) elapsedNanos += gap
            lastFrame = frame
            val duration = slideDuration(pagerState.targetPage).inWholeNanoseconds.coerceAtLeast(1L)
            progress = (elapsedNanos.toDouble() / duration).toFloat().coerceAtMost(1f)
        }
    }

    private suspend fun advance() {
        val page = pagerState.targetPage
        val nextPage = when {
            page < pagerState.pageCount - 1 -> page + 1
            endBehavior == PagerTimerEndBehavior.Loop -> 0
            else -> return
        }
        try {
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = MorphDurationMillis, easing = FastOutSlowInEasing),
            )
        } catch (e: CancellationException) {
            // The user interrupted the scroll, they control the pager now.
            currentCoroutineContext().ensureActive()
        }
    }

    private companion object {
        const val MorphDurationMillis = 300
        const val MaxFrameGapNanos = 100_000_000L
    }
}

/**
 * Creates a [PagerTimerState] that auto-advances [pagerState].
 *
 * @param pagerState Pager to advance.
 * @param slideDuration How long a slide is shown. Use the same duration for all slides of one carousel.
 * @param endBehavior What happens when the last slide is over.
 */
@Composable
fun rememberPagerTimerState(
    pagerState: PagerState,
    slideDuration: Duration = DefaultSlideDuration,
    endBehavior: PagerTimerEndBehavior = PagerTimerEndBehavior.Stop,
): PagerTimerState {
    return rememberPagerTimerState(
        pagerState = pagerState,
        slideDuration = { slideDuration },
        endBehavior = endBehavior,
    )
}

/**
 * Creates a [PagerTimerState] that auto-advances [pagerState].
 *
 * @param pagerState Pager to advance.
 * @param slideDuration How long the given page is shown. Change it only for slides with
 * noticeably more text or with video.
 * @param endBehavior What happens when the last slide is over.
 */
@Composable
fun rememberPagerTimerState(
    pagerState: PagerState,
    slideDuration: (page: Int) -> Duration,
    endBehavior: PagerTimerEndBehavior = PagerTimerEndBehavior.Stop,
): PagerTimerState {
    val state = remember(pagerState) {
        PagerTimerState(
            pagerState = pagerState,
            slideDuration = slideDuration,
            endBehavior = endBehavior,
        )
    }

    val isScreenReaderOn = isTouchExplorationEnabled()

    SideEffect {
        state.slideDuration = slideDuration
        state.endBehavior = endBehavior
        state.isScreenReaderOn = isScreenReaderOn
    }

    LaunchedEffect(state) {
        state.run()
    }

    return state
}

/**
 * Pauses [timerState] while the content is pressed. The progress continues after release.
 *
 * Apply it to the pager or to the slide content. It only observes touches and does not consume them.
 */
fun Modifier.pauseOnPress(timerState: PagerTimerState): Modifier {
    return pointerInput(timerState) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            timerState.isPressed = true
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                timerState.isPressed = false
            }
        }
    }
}

/** Default slide duration: a promo banner with a title and short text. */
val DefaultSlideDuration: Duration = 6.seconds

@Composable
private fun isTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
    var enabled by remember(manager) { mutableStateOf(manager.isTouchExplorationEnabled) }

    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager.addTouchExplorationStateChangeListener(listener)
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }

    return enabled
}
