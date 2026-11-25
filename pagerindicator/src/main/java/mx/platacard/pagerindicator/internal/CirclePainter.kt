package mx.platacard.pagerindicator.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

internal object CirclePainter : Painter() {

    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawCircle(color = Color.Black)
    }
}

/** A circle that is stretched into a pill when the size is not square. */
internal object PillPainter : Painter() {

    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRoundRect(
            color = Color.Black,
            cornerRadius = CornerRadius(size.minDimension / 2f),
        )
    }
}
