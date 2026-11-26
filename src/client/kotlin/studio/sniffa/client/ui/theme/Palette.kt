package studio.sniffa.client.ui.theme

object Palette {

    const val BRAND = 0xFF4EC471.toInt()

    const val MUTED = 0xFF8B9C92.toInt()

    const val ERROR = 0xFFE8394A.toInt()

    const val WARNING = 0xFFF2B33D.toInt()

    const val TEXT = 0xFFF2F2F2.toInt()

    const val GOLD = 0xFFF2C65A.toInt()

    const val ICE = 0xFFB8E4FA.toInt()

    const val SOFT_RED = 0xFFF07A7A.toInt()

    const val WHEEL_TRACK = 0x8C0A1222.toInt()

    const val WEDGE_IDLE_INNER = 0xB00C1A2E.toInt()
    const val WEDGE_IDLE_OUTER = 0x8C21405E.toInt()

    const val WEDGE_ACTIVE_INNER = 0xC06A1428.toInt()
    const val WEDGE_ACTIVE_OUTER = 0xA8C8203A.toInt()

    const val WEDGE_LOCKED_INNER = 0xA0141820.toInt()
    const val WEDGE_LOCKED_OUTER = 0x7A2A303C

    const val WEDGE_DENIED_INNER = 0xB0262C38.toInt()
    const val WEDGE_DENIED_OUTER = 0x8C4A5262.toInt()

    const val FROST = 0x40DDF2FF
    const val FROST_DENIED = 0x26C8CED8

    const val WEDGE_EDGE = 0x59050812

    const val GLOW_HALO = 0x40E8B33A

    const val GLOW_HALO_DENIED = 0x26808A99

    const val COOLDOWN_SHADE = 0xB0050812.toInt()

    const val ICON_DIMMED = 0xC09AA4B4.toInt()

    const val BADGE = 0xC80A0F1C.toInt()

    const val HUB_INNER = 0xD80A1222.toInt()
    const val HUB_OUTER = 0xC0142440.toInt()
    const val HUB_RIM = 0x40B8E4FA

    const val WREATH_BASE = 0xF0123A30.toInt()

    val BULBS = intArrayOf(0xFF4A5A, 0xFFC94A, 0x6BE07A, 0x8FD8FF)

    const val SCRIM = 0x40000000

    val WEDGE = WedgeStyle(
        innerIdle = WEDGE_IDLE_INNER,
        outerIdle = WEDGE_IDLE_OUTER,
        innerHot = WEDGE_ACTIVE_INNER,
        outerHot = WEDGE_ACTIVE_OUTER,
        frost = FROST,
        halo = GLOW_HALO,
    )

    val WEDGE_DENIED = WedgeStyle(
        innerIdle = WEDGE_LOCKED_INNER,
        outerIdle = WEDGE_LOCKED_OUTER,
        innerHot = WEDGE_DENIED_INNER,
        outerHot = WEDGE_DENIED_OUTER,
        frost = FROST_DENIED,
        halo = GLOW_HALO_DENIED,
    )
}
