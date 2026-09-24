package studio.sniffa.client.map

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import studio.sniffa.client.border.BorderZone
import net.minecraft.client.gui.navigation.ScreenRectangle
import studio.sniffa.net.payload.MapTeleportPayload
import studio.sniffa.net.payload.ZonePickPayload
import kotlin.math.floor
import kotlin.math.hypot

class MapScreen(
    private val map: MapState.ReadyMap,
    private val zoneRadius: Int? = null,
) : Screen(Component.literal("Karte")) {

    private var lookX = map.meta.originX + map.meta.size / 2.0
    private var lookZ = map.meta.originZ + map.meta.size / 2.0

    private var zoom = 1.0

    private var pressTravel = 0.0

    private val openedAt = System.currentTimeMillis()

    private val waitingMillis: Long
        get() = (ARM_MILLIS - (System.currentTimeMillis() - openedAt)).coerceAtLeast(0L)

    private val armed: Boolean get() = waitingMillis == 0L

    private val fitScale: Double
        get() = minOf(width - 2 * INSET, height - 2 * INSET).toDouble() / map.meta.size

    private val scale: Double get() = fitScale * zoom

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val sheet = ScreenRectangle(0, 0, width, height)
        val inside = ScreenRectangle(INSET, INSET, width - 2 * INSET, height - 2 * INSET)

        MapOverlay.panel(graphics, MapOverlay.SHEET, sheet, OPAQUE)

        graphics.enableScissor(inside.left(), inside.top(), inside.right(), inside.bottom())
        drawTerrain(graphics)
        drawBorder(graphics, inside)
        drawSelf(graphics)
        drawPick(graphics, mouseX, mouseY, inside)
        graphics.disableScissor()

        drawRose(graphics, sheet)
        drawReadout(graphics, mouseX, mouseY, sheet)
    }

    private fun drawTerrain(graphics: GuiGraphics) {
        val texture = MapTexture.of(map)
        val size = map.meta.size

        graphics.blit(
            RenderPipelines.GUI_TEXTURED, texture,
            toScreenX(map.meta.originX.toDouble()).toInt(),
            toScreenZ(map.meta.originZ.toDouble()).toInt(),
            0f, 0f,
            (size * scale).toInt(), (size * scale).toInt(),
            size, size,
            size, size,
        )
    }

    private fun drawRose(graphics: GuiGraphics, sheet: ScreenRectangle) {
        val centreX = sheet.right() - ROSE_INSET.toDouble()
        val centreY = sheet.top() + ROSE_INSET.toDouble()

        MapOverlay.circle(
            graphics,
            MapOverlay.ROSE,
            MapOverlay.boxAround(centreX, centreY, ROSE_RADIUS * 1.6),
            centreX, centreY, ROSE_RADIUS,
            INK,
        )

        val letters = ROSE_RADIUS.toInt() + 7
        mark(graphics, "N", centreX, centreY - letters)
        mark(graphics, "S", centreX, centreY + letters)
        mark(graphics, "W", centreX - letters, centreY)
        mark(graphics, "O", centreX + letters, centreY)
    }

    private fun mark(graphics: GuiGraphics, letter: String, atX: Double, atY: Double) {
        graphics.drawString(
            font, letter,
            (atX - font.width(letter) / 2.0).toInt(),
            (atY - font.lineHeight / 2.0).toInt(),
            INK, false,
        )
    }

    private fun drawPick(graphics: GuiGraphics, mouseX: Int, mouseY: Int, clip: ScreenRectangle) {
        val worldX = toWorldX(mouseX.toDouble())
        val worldZ = toWorldZ(mouseY.toDouble())
        if (worldX < map.meta.originX || worldX >= map.meta.originX + map.meta.size ||
            worldZ < map.meta.originZ || worldZ >= map.meta.originZ + map.meta.size
        ) {
            return
        }

        val allowed = BorderZone.current()?.let {
            hypot(worldX - it.centerX, worldZ - it.centerZ) <= it.radius
        } ?: true

        val onScreen = if (zoneRadius != null) zoneRadius * scale else PICK_RADIUS
        val ink = when {
            !armed -> PICK_WAITING
            zoneRadius != null -> if (allowed) ZONE_ALLOWED else PICK_REFUSED
            allowed -> PICK_ALLOWED
            else -> PICK_REFUSED
        }

        MapOverlay.circle(
            graphics,
            MapOverlay.PICK,
            MapOverlay.boxAround(mouseX.toDouble(), mouseY.toDouble(), onScreen * PICK_REACH),
            mouseX.toDouble(),
            mouseY.toDouble(),
            onScreen,
            ink,
            clip,
        )
    }

    private fun drawBorder(graphics: GuiGraphics, clip: ScreenRectangle) {
        val ring = BorderZone.current() ?: return

        MapOverlay.circle(
            graphics,
            MapOverlay.BORDER,
            MapOverlay.wholeScreen(width, height),
            toScreenX(ring.centerX),
            toScreenZ(ring.centerZ),
            ring.radius * scale,
            (BorderZone.tint() and 0xFFFFFF) or BORDER_OPACITY,
            clip,
        )
    }

    private fun drawSelf(graphics: GuiGraphics) {
        val player = minecraft?.player ?: return
        val x = toScreenX(player.x).toInt()
        val z = toScreenZ(player.z).toInt()

        graphics.fill(x - 3, z - 3, x + 3, z + 3, INK)
        graphics.fill(x - 2, z - 2, x + 2, z + 2, SELF)
    }

    private fun drawReadout(graphics: GuiGraphics, mouseX: Int, mouseY: Int, sheet: ScreenRectangle) {
        val worldX = floor(toWorldX(mouseX.toDouble())).toInt()
        val worldZ = floor(toWorldZ(mouseY.toDouble())).toInt()

        val line = if (armed) {
            if (zoneRadius != null) {
                "$worldX, $worldZ  ·  Klicken setzt die Gefahrenzone  ·  Esc bricht ab"
            } else {
                "$worldX, $worldZ  ·  Klicken teleportiert  ·  Rad zoomt  ·  Esc schließt"
            }
        } else {
            "$worldX, $worldZ  ·  Gerät fährt hoch  ·  noch ${(waitingMillis + 999) / 1000} s"
        }
        graphics.drawString(
            font, line,
            (width - font.width(line)) / 2,
            sheet.bottom() - INSET - font.lineHeight,
            INK, false,
        )
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == 0) {
            pressTravel = 0.0
            return true
        }
        return super.mouseClicked(event, doubled)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (event.button() != 0) {
            return super.mouseDragged(event, dragX, dragY)
        }

        pressTravel += hypot(dragX, dragY)
        if (pressTravel > CLICK_SLOP) {
            lookX -= dragX / scale
            lookZ -= dragY / scale
            clampView()
        }
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && !armed) {
            return true
        }

        if (event.button() == 0 && pressTravel <= CLICK_SLOP) {
            val worldX = floor(toWorldX(event.x())).toInt()
            val worldZ = floor(toWorldZ(event.y())).toInt()

            if (worldX >= map.meta.originX && worldX < map.meta.originX + map.meta.size &&
                worldZ >= map.meta.originZ && worldZ < map.meta.originZ + map.meta.size
            ) {
                if (zoneRadius != null) {
                    ClientPlayNetworking.send(ZonePickPayload(worldX, worldZ))
                } else {
                    ClientPlayNetworking.send(MapTeleportPayload(worldX, worldZ))
                }
                onClose()
            }
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (scrollY == 0.0) {
            return false
        }

        val underX = toWorldX(mouseX)
        val underZ = toWorldZ(mouseY)

        zoom = (zoom * if (scrollY > 0) ZOOM_STEP else 1 / ZOOM_STEP).coerceIn(1.0, MOST_ZOOM)

        lookX = underX - (mouseX - width / 2.0) / scale
        lookZ = underZ - (mouseY - height / 2.0) / scale
        clampView()
        return true
    }

    private fun clampView() {
        lookX = held(lookX, map.meta.originX.toDouble(), (width - 2 * INSET) / 2.0 / scale)
        lookZ = held(lookZ, map.meta.originZ.toDouble(), (height - 2 * INSET) / 2.0 / scale)
    }

    private fun held(look: Double, origin: Double, halfVisible: Double): Double {
        val span = map.meta.size.toDouble()
        if (halfVisible * 2 >= span) {
            return origin + span / 2.0
        }
        return look.coerceIn(origin + halfVisible, origin + span - halfVisible)
    }

    private fun toScreenX(worldX: Double) = width / 2.0 + (worldX - lookX) * scale
    private fun toScreenZ(worldZ: Double) = height / 2.0 + (worldZ - lookZ) * scale
    private fun toWorldX(atX: Double) = lookX + (atX - width / 2.0) / scale
    private fun toWorldZ(atZ: Double) = lookZ + (atZ - height / 2.0) / scale

    override fun isPauseScreen(): Boolean = false

    companion object {
        private const val INSET = 19

        private const val ROSE_RADIUS = 20.0
        private const val ROSE_INSET = 46

        private const val INK = 0xFF32231A.toInt()
        private const val SELF = 0xFFB3402B.toInt()

        private const val OPAQUE = 0xFFFFFFFF.toInt()

        private const val ZOOM_STEP = 1.3
        private const val MOST_ZOOM = 8.0

        private const val CLICK_SLOP = 4.0

        private const val PICK_RADIUS = 9.0

        private const val PICK_REACH = 2.0

        private const val ARM_MILLIS = 3_000L

        private const val PICK_ALLOWED = 0xD84EC471.toInt()
        private const val PICK_REFUSED = 0xD8FC301E.toInt()
        private const val PICK_WAITING = 0xC08A7A66.toInt()
        private const val ZONE_ALLOWED = 0xB3FC301E.toInt()

        private const val BORDER_OPACITY = 0xE6000000.toInt()
    }
}
