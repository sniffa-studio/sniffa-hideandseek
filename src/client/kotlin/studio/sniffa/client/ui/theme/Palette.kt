package studio.sniffa.client.ui.theme

object Palette {

    const val BRAND = 0xFF4EC471.toInt()

    const val MUTED = 0xFF8B9C92.toInt()

    const val ERROR = 0xFFE8394A.toInt()

    const val WARNING = 0xFFF2B33D.toInt()

    const val TEXT = 0xFFF2F2F2.toInt()

    const val GOLD = 0xFFF2C65A.toInt()

    const val WHEEL_TRACK = 0x8C0A1222.toInt()

    const val WEDGE_IDLE_INNER = 0x8C0A1222.toInt()
    const val WEDGE_IDLE_OUTER = 0x1A0A1222

    const val WEDGE_ACTIVE_INNER = 0xB3C8102E.toInt()
    const val WEDGE_ACTIVE_OUTER = 0x73C8102E

    const val WEDGE_LOCKED_INNER = 0x8C141820.toInt()
    const val WEDGE_LOCKED_OUTER = 0x1A141820

    const val COOLDOWN_SHADE = 0xB3050812.toInt()

    const val WEDGE_DENIED_INNER = 0x8C3A4250.toInt()
    const val WEDGE_DENIED_OUTER = 0x593A4250

    const val WEDGE_EDGE = 0x59050812

    const val GLOW_HALO = 0x4DE8B33A

    const val GLOW_HALO_DENIED = 0x33808A99

    const val DOT_ACTIVE = 0xFFE8B33A.toInt()

    const val DOT_IDLE = 0x55707C90

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
