# PagerIndicator Library for Jetpack Compose

This library provides a simple and customizable pager indicator for Android apps using Jetpack Compose. It includes two types of indicators to visually represent the user’s current page in a horizontal or vertical pager.

## Screenshots

### PagerIndicator

![Pager Indicator Demo](assets/pager_indicator_horizontal.gif)

### PagerWormIndicator

![Pager Indicator Demo](assets/worm_pager_indicator_horizontal.gif)

### PagerIndicator & PagerWormIndicator vertical

![Pager Indicator Demo](assets/pager_indicator_vertical.gif)

### PagerTimerIndicator

![Pager Timer Indicator Demo](assets/pager_timer_indicator_horizontal.gif)

## Features

- **Three Indicator Types**:
    - **Pager Worm Indicator** - A dynamic "worm-like" animation that visually represents the transition between pages.
    - **Pager Dot Indicator** - A standard dot indicator for simpler paging needs.
    - **Pager Timer Indicator** - For auto-advancing carousels (stories, onboarding, promo banners): the active page is a timer that fills up while the slide is shown.
- **Customizable**:
    - Set the colors, sizes, and spacing of the dots.
    - Choose between horizontal and vertical orientations.
    - Define the number of visible dots.
- **Flexible State Handling**:
    - Use the `PagerState` directly, or, for non-standard paginated elements, you can pass a custom `currentPageFraction` to represent the fractional position of the current page. This flexibility allows you to integrate the indicator with other components beyond `PagerState`.

## Installation

Add the library to your project by including it in your Gradle dependencies.

```gradle
dependencies {
    implementation("mx.platacard:compose-pager-indicator:$latestRelease")
}
```

## Usage

To use the indicators, simply call `PagerWormIndicator` or `PagerIndicator` within your `Composable` function, passing in the `PagerState` (or `currentPageFraction` for custom components) and other parameters to customize the appearance.

### Example

```kotlin
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import mx.platacard.pagerindicator.PagerWormIndicator
import mx.platacard.pagerindicator.PagerIndicator
import mx.platacard.pagerindicator.PagerIndicatorOrientation

@Composable
fun MyPagerIndicator(pagerState: PagerState) {
    PagerWormIndicator(
        pagerState = pagerState,
        activeDotColor = Color.Blue,
        dotColor = Color.Gray,
        dotCount = 5,
        orientation = PagerIndicatorOrientation.Horizontal
    )
    
    // OR use the dot indicator:
    
    PagerIndicator(
        pagerState = pagerState,
        activeDotColor = Color.Green,
        dotColor = Color.LightGray,
        dotCount = 5,
        orientation = PagerIndicatorOrientation.Vertical
    )
}
```

## Parameters

| Parameter              | Description                                                                                               |
|------------------------|-----------------------------------------------------------------------------------------------------------|
| `pagerState`           | The `PagerState` that represents the current state of the pager.                                          |
| `currentPageFraction`  | Fractional position representing the current page for custom components.                                  |
| `modifier`             | `Modifier` for applying layout adjustments.                                                               |
| `activeDotColor`       | Color of the active dot.                                                                                  |
| `dotColor`             | Color of the inactive dots.                                                                               |
| `dotCount`             | Number of dots displayed (should be odd unless equal to `pageCount`).                                     |
| `dotPainter` (optional)| Custom `Painter` for rendering the dots.                                                                  |
| `normalDotSize`        | Size of inactive dots.                                                                                    |
| `activeDotSize`        | Size of the active dot.                                                                                   |
| `minDotSize`           | Size of dots displayed at the edges.                                                                      |
| `space`                | Space between dots.                                                                                       |
| `orientation`          | `Horizontal` or `Vertical` orientation of the indicator.                                                  |

## PagerTimerIndicator

Use it for content that switches automatically. `rememberPagerTimerState` drives the progress and scrolls the pager to the next page when the timer is full.

```kotlin
val pagerState = rememberPagerState { slides.size }
val timerState = rememberPagerTimerState(
    pagerState = pagerState,
    slideDuration = 6.seconds,
    endBehavior = PagerTimerEndBehavior.Loop, // or Stop at 100% on the last slide
)

Box {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.pauseOnPress(timerState), // press and hold pauses the timer
    ) { page -> Slide(slides[page]) }

    PagerTimerIndicator(
        pagerState = pagerState,
        timerState = timerState,
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
    )
}
```

- Progress is linear and restarts from 0% on every page change, including a manual swipe.
- The timer pauses while the pager is scrolled, the content is pressed, the app is in background, or `timerState.isPaused` is `true` (use it while the slide is loading or a bottom sheet is open).
- When a screen reader (TalkBack) is on, the pager is not advanced automatically.
- Up to `dotCount` (5 by default) elements are visible: the timer, 6dp neighbours and 4dp edge dots. With a single page nothing is drawn.
- Draw it on dark or media backgrounds only: everything is drawn with `color` (white by default), inactive dots and the timer track with 30% opacity.

For custom state, use the overload with `pageCount`, `currentPageFraction` and `progress: () -> Float`.

## Using a Custom `currentPageFraction`

If you're not using a traditional pager but still want an indicator for a custom component, you can replace `pagerState` with a `currentPageFraction` parameter. `currentPageFraction` represents the fractional position of the current page and allows integration with custom elements.

```kotlin
PagerIndicator(
    pageCount = 10,
    currentPageFraction = remember {
        derivedStateOf { /* Your logic to calculate fractional page position */ } 
    },
    activeDotColor = Color.Blue,
    dotColor = Color.Gray,
    dotCount = 5
)
```