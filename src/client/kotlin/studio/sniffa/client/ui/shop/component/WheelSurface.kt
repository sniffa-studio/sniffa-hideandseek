package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.ParentUIComponent
import io.wispforest.owo.ui.core.Surface
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Spacing
import studio.sniffa.client.ui.theme.WheelMetrics

class WheelSurface(
    private val slots: Int,
    private val metrics: WheelMetrics,
) : Surface {

    private val painter = RadialPainter(
        slots = slots,
        metrics = metrics,
        firstCentreDegrees = 90.0,
        gapDegrees = Spacing.WEDGE_GAP_DEGREES,
    )

    private val locked = BooleanArray(slots)
    private val cooling = FloatArray(slots)

    private var active: Int? = null

    fun describe(index: Int, locked: Boolean, cooling: Float) {
        if (index !in 0 until slots) return
        this.locked[index] = locked
        this.cooling[index] = cooling
    }

    fun update(active: Int?) {
        this.active = active
    }

    override fun draw(graphics: OwoUIGraphics, component: ParentUIComponent) {
        if (component.width() <= 0 || component.height() <= 0) return

        painter.advance(active)

        val centerX = component.x() + component.width() / 2
        val centerY = component.y() + component.height() / 2

        for (index in 0 until slots) {
            painter.drawWedge(
                graphics, centerX, centerY, index,
                if (locked[index]) Palette.WEDGE_DENIED else Palette.WEDGE,
                cooling[index],
            )
        }

    }
}
