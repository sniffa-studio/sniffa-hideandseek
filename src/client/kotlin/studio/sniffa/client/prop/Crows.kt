package studio.sniffa.client.prop

import com.geckolib.constant.DataTickets
import com.geckolib.renderer.GeoObjectRenderer
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.entity.Display
import net.minecraft.world.level.LightLayer
import studio.sniffa.Hideandseek
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Crows {

    val ROOST = Hideandseek.id("props/kraehen")

    private const val SCAN_EVERY_TICKS = 10
    private const val CLOCK = 1_000_000L
    private const val FLY_SECONDS = 7.0
    private const val LAND_SECONDS = 3.2
    private const val SHOWN_WITHIN = 96.0
    private const val SPREAD = 0.55

    private class Roost(val x: Double, val y: Double, val z: Double, val count: Int, val away: Boolean, val changedAt: Long, val crows: List<Crow>)

    private val roosts = HashMap<Int, Roost>()
    private var ticks = 0
    private val renderer = CrowRenderer()

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val level = client.level
            if (level == null) {
                roosts.clear()
                return@register
            }
            if (++ticks % SCAN_EVERY_TICKS == 0) scan(level)
        }
        LevelRenderEvents.COLLECT_SUBMITS.register { context -> draw(context.poseStack(), context) }
    }

    private fun scan(level: ClientLevel) {
        val seen = HashSet<Int>()
        for (entity in level.entitiesForRendering()) {
            if (entity !is Display.ItemDisplay) continue
            val stack = entity.itemRenderState()?.itemStack() ?: continue
            if (stack.get(DataComponents.ITEM_MODEL) != ROOST) continue
            val floats = stack.get(DataComponents.CUSTOM_MODEL_DATA)?.floats() ?: continue
            val count = (floats.getOrNull(0) ?: 4f).toInt().coerceIn(1, 8)
            val away = (floats.getOrNull(1) ?: 0f) >= 0.5f
            val changedAt = (floats.getOrNull(2) ?: 0f).toLong()
            seen += entity.id
            val at = entity.position()
            val known = roosts[entity.id]
            if (known == null || known.count != count || known.away != away || known.changedAt != changedAt || known.x != at.x || known.y != at.y || known.z != at.z) {
                val crows = known?.crows?.takeIf { it.size == count } ?: List(count) { Crow(hash(entity.id, it, 9) * 20.0) }
                roosts[entity.id] = Roost(at.x, at.y, at.z, count, away, changedAt, crows)
            }
        }
        roosts.keys.retainAll(seen)
    }

    private fun hash(seed: Int, index: Int, salt: Int): Double {
        var h = seed * 0x27d4eb2d + index * -0x61c88647 + salt * 0x165667b1
        h = h xor (h ushr 15)
        h *= 0x2c1b3c6d
        h = h xor (h ushr 12)
        return (h and 0xffff) / 65535.0
    }

    private fun place(roost: Roost, id: Int, index: Int, crow: Crow, since: Double, now: Double) {
        val perchX = roost.x + (hash(id, index, 1) - 0.5) * 2.0 * SPREAD
        val perchZ = roost.z + (hash(id, index, 2) - 0.5) * 2.0 * SPREAD
        val perchYaw = (hash(id, index, 3) * 360.0).toFloat()
        val heading = hash(id, index, 4) * 2.0 * Math.PI
        val dirX = cos(heading)
        val dirZ = sin(heading)
        crow.seconds = now + crow.offset

        fun along(t: Double): DoubleArray {
            val out = t * 4.5 + t * t * 0.5
            val up = t * 1.6 + t * t * 0.35
            val wobble = sin(t * 3.0 + index) * 0.25 * t
            return doubleArrayOf(perchX + dirX * out - dirZ * wobble, roost.y + up, perchZ + dirZ * out + dirX * wobble)
        }

        if (roost.away && since >= FLY_SECONDS) {
            crow.visible = false
            return
        }
        crow.visible = true
        val flightT = when {
            roost.away -> since + hash(id, index, 5) * 0.4
            since < LAND_SECONDS -> (LAND_SECONDS - since) * 1.6
            else -> -1.0
        }
        if (flightT <= 0.0) {
            crow.flying = false
            crow.x = perchX
            crow.y = roost.y
            crow.z = perchZ
            crow.yaw = perchYaw
            crow.pitch = 0f
            return
        }
        val here = along(flightT)
        val ahead = along(flightT + if (roost.away) 0.05 else -0.05)
        val dx = ahead[0] - here[0]
        val dy = ahead[1] - here[1]
        val dz = ahead[2] - here[2]
        crow.flying = true
        crow.x = here[0]
        crow.y = here[1]
        crow.z = here[2]
        crow.yaw = Math.toDegrees(atan2(-dx, -dz)).toFloat()
        crow.pitch = Math.toDegrees(atan2(dy, sqrt(dx * dx + dz * dz))).toFloat().coerceIn(-35f, 35f)
    }

    private fun draw(matrices: PoseStack, context: LevelRenderContext) {
        if (roosts.isEmpty()) return
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val camera = client.gameRenderer.mainCamera().position()
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val clock = (level.gameTime % CLOCK).toDouble() + partial
        val now = level.gameTime / 20.0 + partial / 20.0
        for ((id, roost) in roosts) {
            if (camera.distanceToSqr(roost.x, roost.y, roost.z) > SHOWN_WITHIN * SHOWN_WITHIN) continue
            val since = ((clock - roost.changedAt + CLOCK) % CLOCK) / 20.0
            roost.crows.forEachIndexed { index, crow ->
                place(roost, id, index, crow, since, now)
                if (!crow.visible) return@forEachIndexed
                matrices.pushPose()
                matrices.translate(crow.x - camera.x, crow.y - camera.y, crow.z - camera.z)
                matrices.mulPose(Axis.YP.rotationDegrees(crow.yaw))
                matrices.mulPose(Axis.XP.rotationDegrees(crow.pitch))
                renderer.performRenderPass(crow, crow, matrices, context.submitNodeCollector(), context.levelState().cameraRenderState, lightAt(level, crow), 0f)
                matrices.popPose()
            }
        }
    }

    private fun lightAt(level: ClientLevel, crow: Crow): Int {
        val at = BlockPos.containing(crow.x, crow.y + 0.3, crow.z)
        return LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, at), level.getBrightness(LightLayer.SKY, at))
    }

    private class CrowRenderer : GeoObjectRenderer<Crow, Crow, GeoRenderState.Impl>(CrowModel()) {

        override fun adjustRenderPose(pass: RenderPassInfo<GeoRenderState.Impl>) {
        }

        override fun addRenderData(animatable: Crow, relatedObject: Crow?, renderState: GeoRenderState.Impl, partialTick: Float) {
            renderState.addGeckolibData(DataTickets.TICK, animatable.seconds * 20.0)
        }
    }
}
