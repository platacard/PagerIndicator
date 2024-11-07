package mx.platacard.pagerindicator.internal

internal fun calculateDrawableDotIndices(
    dotCount: Int,
    currentPage: Int,
    pageCount: Int,
): Pair<Int, Int> {

    val onTheSidesDotCount = dotCount / 2

    val firstVisible: Int
    val lastVisible: Int

    if (currentPage < pageCount / 2) {
        firstVisible = (currentPage - onTheSidesDotCount).coerceAtLeast(0)
        lastVisible = (firstVisible + onTheSidesDotCount * 2)
    } else {
        lastVisible = (currentPage + onTheSidesDotCount).coerceAtMost(pageCount - 1)
        firstVisible = (lastVisible - onTheSidesDotCount * 2)
    }
    return firstVisible to lastVisible
}

internal fun calculateTargetDotSizeForPage(
    i: Int,
    currentPage: Int,
    pageCount: Int,
    dotCount: Int,
): DotState {

    val onTheSidesDotCount = dotCount / 2

    val (firstVisible, lastVisible) = calculateDrawableDotIndices(
        dotCount = dotCount,
        currentPage = currentPage,
        pageCount = pageCount,
    )

    // ● - selected dot
    // ○ - normal dot
    // • - small edge dot

    return when {
        // [• ○ ● ○ •]
        //      ^
        currentPage == i -> DotState.Selected

        // [• ○ ● ○ •] //
        //    ^   ^    //
        currentPage - i in -(onTheSidesDotCount - 1)..<onTheSidesDotCount -> DotState.Normal

        // [• ○ ● ○ ○] //
        //    ^        //
        currentPage + onTheSidesDotCount >= pageCount &&
                i > currentPage - (dotCount - (pageCount - currentPage - 1) - 1) -> DotState.Normal

        // [○ ○ ● ○ •] //
        //  ^          //
        currentPage - onTheSidesDotCount <= 0 &&
                i < currentPage + (dotCount - currentPage - 1) -> DotState.Normal

        // [• ○ ● ○ ○] //
        //          ^  //
        currentPage + onTheSidesDotCount == i && i == pageCount - 1 -> DotState.Normal

        // in case if of equality we don't need to use dotMinSizePx
        // [○ ○ ○ ○ ●] //
        //  ^          //
        dotCount == pageCount -> DotState.Normal

        // [• ○ ● ○ •] [• ○ ● ○ ○] [○ ○ ● ○ •] //
        //  ^       ^   ^                   ^  //
        i in firstVisible..lastVisible -> DotState.SmallEdge

        else -> DotState.Invisible
    }
}

