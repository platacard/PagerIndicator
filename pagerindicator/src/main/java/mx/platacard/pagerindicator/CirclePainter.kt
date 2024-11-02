package mx.platacard.pagerindicator

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
