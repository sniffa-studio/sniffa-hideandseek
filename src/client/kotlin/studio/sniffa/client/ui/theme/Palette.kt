package studio.sniffa.client.ui.theme

object Palette {

    const val BRAND = 0xFF4EC471.toInt()

    const val MUTED = 0xFF8B9C92.toInt()

    const val ERROR = 0xFFFC301E.toInt()

    const val TEXT = 0xFFF2F2F2.toInt()

    const val WHEEL_TRACK = 0x8C0B120F.toInt()

    const val WEDGE_IDLE_INNER = 0x8C0B120F.toInt()
    const val WEDGE_IDLE_OUTER = 0x1A0B120F

    const val WEDGE_ACTIVE_INNER = 0x994EC471.toInt()
    const val WEDGE_ACTIVE_OUTER = 0x664EC471

    const val WEDGE_LOCKED_INNER = 0x8C1E0B09.toInt()
    const val WEDGE_LOCKED_OUTER = 0x1A1E0B09

    const val COOLDOWN_SHADE = 0xB3060B09.toInt()

    const val WEDGE_DENIED_INNER = 0x8CFC301E.toInt()
    const val WEDGE_DENIED_OUTER = 0x59FC301E

    const val WEDGE_EDGE = 0x59060B09

    const val GLOW_HALO = 0x3D4EC471

    const val GLOW_HALO_DENIED = 0x33FC301E

    const val DOT_ACTIVE = 0xFF4EC471.toInt()

    const val DOT_IDLE = 0x556F7A74

    const val SCRIM = 0x40000000

    val WEDGE = WedgeStyle(
        innerIdle = WEDGE_IDLE_INNER,
        outerIdle = WEDGE_IDLE_OUTER,
        innerHot = WEDGE_ACTIVE_INNER,
        outerHot = WEDGE_ACTIVE_OUTER,
        halo = GLOW_HALO,
    )

    val WEDGE_DENIED = WedgeStyle(
        innerIdle = WEDGE_LOCKED_INNER,
        outerIdle = WEDGE_LOCKED_OUTER,
        innerHot = WEDGE_DENIED_INNER,
        outerHot = WEDGE_DENIED_OUTER,
        halo = GLOW_HALO_DENIED,
    )
}
