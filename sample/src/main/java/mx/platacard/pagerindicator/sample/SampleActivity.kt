package mx.platacard.pagerindicator.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.seconds
import mx.platacard.pagerindicator.PagerIndicator
import mx.platacard.pagerindicator.PagerIndicatorOrientation
import mx.platacard.pagerindicator.PagerTimerEndBehavior
import mx.platacard.pagerindicator.PagerTimerIndicator
import mx.platacard.pagerindicator.PagerWormIndicator
import mx.platacard.pagerindicator.pauseOnPress
import mx.platacard.pagerindicator.rememberPagerTimerState

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TimerPagerSample()
                    HorizontalPagerSample()
                    VerticalPagerSample()
                }
            }
        }
    }
}

@Composable
private fun TimerPagerSample() {

    var pageCount by rememberSaveable { mutableStateOf(7) }
    var loop by rememberSaveable { mutableStateOf(true) }
    var paused by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState { pageCount }
    val timerState = rememberPagerTimerState(
        pagerState = pagerState,
        slideDuration = 4.seconds,
        endBehavior = if (loop) PagerTimerEndBehavior.Loop else PagerTimerEndBehavior.Stop,
    )
    timerState.isPaused = paused

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .pauseOnPress(timerState),
                state = pagerState,
            ) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(140.dp)
                        .background(Color(0xFFFF5D15), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "$i / ${pagerState.pageCount - 1}",
                        color = { Color.White },
                        style = TextStyle(fontSize = 24.sp),
                    )
                }
            }

            PagerTimerIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                pagerState = pagerState,
                timerState = timerState,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SampleButton("Pages: $pageCount") { pageCount = if (pageCount >= 10) 2 else pageCount + 1 }
            SampleButton(if (loop) "Loop" else "Stop") { loop = !loop }
            SampleButton(if (paused) "Resume" else "Pause") { paused = !paused }
        }
    }
}

@Composable
private fun SampleButton(text: String, onClick: () -> Unit) {
    BasicText(
        text = text,
        modifier = Modifier
            .background(Color(0xFFEDEFF3), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = TextStyle(fontSize = 14.sp),
    )
}

@Composable
private fun HorizontalPagerSample() {

    val pageCount = 11
    val pagerState = rememberPagerState(pageCount / 2) { pageCount }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState,
        ) { i ->

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(140.dp)
                    .background(Color(0xFFFF5D15), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "$i / ${pagerState.pageCount - 1}",
                    color = { Color.White },
                    style = TextStyle(
                        fontSize = 24.sp,
                    ),
                )
            }
        }

        PagerIndicator(
            pagerState = pagerState,
            activeDotColor = Color.Blue,
            normalDotSize = 8.dp,
            activeDotSize = 12.dp,
            minDotSize = 4.dp,
            dotColor = Color.Gray,
        )

        val starResource = painterResource(R.drawable.ic_16_star)
        val giftResource = painterResource(R.drawable.ic_16_gift)

        PagerIndicator(
            pagerState = pagerState,
            activeDotColor = Color.Blue,
            normalDotSize = 8.dp,
            activeDotSize = 12.dp,
            minDotSize = 4.dp,
            dotColor = Color.Gray,
            dotPainter = { i ->
                if (i % 2 == 0) {
                    giftResource
                } else {
                    starResource
                }
            }
        )

        PagerWormIndicator(
            pageCount = pagerState.pageCount,
            currentPageFraction = remember {
                derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            },
            activeDotColor = Color.Blue,
            activeDotSize = 12.dp,
            minDotSize = 4.dp,
            dotColor = Color.Gray,
        )
    }
}

@Composable
private fun VerticalPagerSample() {

    val pageCount = 11
    val pagerState = rememberPagerState(pageCount / 2) { pageCount }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        VerticalPager(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(180.dp),
            state = pagerState,
        ) { i ->

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .height(140.dp)
                    .background(Color(0xFFFF5D15), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "$i / ${pagerState.pageCount - 1}",
                    color = { Color.White },
                    style = TextStyle(
                        fontSize = 24.sp,
                    ),
                )
            }
        }

        val starResource = painterResource(R.drawable.ic_16_star)
        val giftResource = painterResource(R.drawable.ic_16_gift)

        PagerIndicator(
            pagerState = pagerState,
            activeDotColor = Color.Blue,
            normalDotSize = 10.dp,
            activeDotSize = 14.dp,
            minDotSize = 6.dp,
            dotColor = Color.Gray,
            orientation = PagerIndicatorOrientation.Vertical,
            dotPainter = { i ->
                if (i % 2 == 0) {
                    giftResource
                } else {
                    starResource
                }
            }
        )


        PagerWormIndicator(
            pagerState = pagerState,
            activeDotColor = Color.Blue,
            activeDotSize = 12.dp,
            minDotSize = 4.dp,
            orientation = PagerIndicatorOrientation.Vertical,
            dotColor = Color.Gray,
        )
    }
}
