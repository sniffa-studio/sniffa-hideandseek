package studio.sniffa.client.prop

import com.geckolib.constant.DataTickets
import com.geckolib.renderer.GeoObjectRenderer
import com.geckolib.renderer.base.GeoRenderer
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer
import net.minecraft.client.renderer.SubmitNodeCollector
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.util.LightCoordsUtil
import org.slf4j.LoggerFactory
import studio.sniffa.Hideandseek
import studio.sniffa.prop.PropKind
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

object Props {

    private val LOGGER = LoggerFactory.getLogger("${Hideandseek.MOD_ID}/props")

    private val standing = CopyOnWriteArrayList<Prop>()

    private val worn = ConcurrentHashMap<Pair<Int, String>, Prop>()

    private val LIGHT_SAMPLES = doubleArrayOf(0.5, 1.2, 1.9)

    private const val HOP_HEIGHT = 0.22

    private const val HOP_PACE = 0.55f

    private const val TRAIL_BACK = 1.1

    private const val TRAIL_UP = 1.25

    private const val TRAIL_SIDE = 0.45

    private const val TRAIL_SWAY = 0.25

    private const val TRAIL_BOB = 0.08

    private const val TRAIL_CATCH_UP = 5.0

    private val renderer: GeoObjectRenderer<Prop, Prop, GeoRenderState.Impl> = PropRenderer()

    private class PropRenderer : GeoObjectRenderer<Prop, Prop, GeoRenderState.Impl>(PropModel()) {

        init {
            withRenderLayer(Glow(this))
        }

        override fun adjustRenderPose(pass: RenderPassInfo<GeoRenderState.Impl>) {
        }

        override fun addRenderData(animatable: Prop, relatedObject: Prop?, renderState: GeoRenderState.Impl, partialTick: Float) {
            renderState.addGeckolibData(DataTickets.TICK, animatable.age * TICKS_PER_SECOND)
        }
    }

    private class Glow(renderer: GeoRenderer<Prop, Prop, GeoRenderState.Impl>) :
        AutoGlowingGeoLayer<Prop, Prop, GeoRenderState.Impl>(renderer) {

        override fun submitRenderTask(pass: RenderPassInfo<GeoRenderState.Impl>, collector: SubmitNodeCollector) {
            if (PropModel.kindOf(pass.renderState()).glows) super.submitRenderTask(pass, collector)
        }
    }

    private const val TICKS_PER_SECOND = 20.0

    fun accept(kind: String, x: Double, y: Double, z: Double, yaw: Float, seconds: Int) {
        val known = PropKind.byId(kind)
        if (known == null) {
            LOGGER.warn("Server placed a prop this jar does not know: {}. Ignored.", kind)
            return
        }

        standing.add(Prop(known, x, y, z, yaw, seconds))
    }

    fun wear(entityId: Int, kind: String, seconds: Int) {
        if (seconds <= 0) {
            worn[entityId to kind]?.endSoon()
            return
        }
        val known = PropKind.byId(kind)
        if (known == null) {
            LOGGER.warn("Server sent a disguise this jar does not know: {}. Ignored.", kind)
            return
        }
        worn[entityId to kind] = Prop(known, 0.0, 0.0, 0.0, 0f, seconds)
    }

    fun forget() {
        standing.clear()
        worn.clear()
    }

    fun install() {
        LevelRenderEvents.COLLECT_SUBMITS.register { context ->
            draw(context.poseStack(), context)
        }
    }

    private fun draw(
        matrices: PoseStack,
        context: LevelRenderContext,
    ) {
        if (standing.isEmpty() && worn.isEmpty()) return

        standing.removeIf { !it.alive() }
        worn.values.removeIf { !it.alive() }
        if (standing.isEmpty() && worn.isEmpty()) return

        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val view = client.gameRenderer.mainCamera()
        val camera = view.position()
        val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)

        val dressed = worn.mapNotNull { (key, prop) ->
            val wearer = level.getEntity(key.first) ?: return@mapNotNull null
            if (wearer.isRemoved) return@mapNotNull null
            if (prop.kind.trails) {
                trail(prop, wearer, partialTick)
                return@mapNotNull prop
            }
            if (wearer.isSpectator) return@mapNotNull null
            if (wearer === view.entity() && !view.isDetached) return@mapNotNull null
            follow(prop, wearer, partialTick)
            prop
        }

        for (prop in standing + dressed) {
            matrices.pushPose()

            matrices.translate(prop.x - camera.x, prop.y - camera.y, prop.z - camera.z)

            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-prop.yaw))
            matrices.scale(prop.kind.scale, prop.kind.scale, prop.kind.scale)

            renderer.performRenderPass(
                prop,
                prop,
                matrices,
                context.submitNodeCollector(),
                context.levelState().cameraRenderState,
                lightAt(level, prop),
                0f,
            )

            matrices.popPose()
        }
    }

    private fun follow(prop: Prop, wearer: Entity, partialTick: Float) {
        val at = wearer.getPosition(partialTick)
        prop.x = at.x
        prop.y = at.y + hop(wearer, partialTick)
        prop.z = at.z
        val body = if (wearer is LivingEntity) lerpDegrees(wearer.yBodyRotO, wearer.yBodyRot, partialTick) else wearer.getYRot(partialTick)
        prop.yaw = body + 180f
    }

    private fun trail(prop: Prop, wearer: Entity, partialTick: Float) {
        val at = wearer.getPosition(partialTick)
        val yaw = if (wearer is LivingEntity) lerpDegrees(wearer.yBodyRotO, wearer.yBodyRot, partialTick) else wearer.getYRot(partialTick)
        val radians = Math.toRadians(yaw.toDouble())
        val backX = sin(radians) * TRAIL_BACK
        val backZ = -cos(radians) * TRAIL_BACK
        val side = TRAIL_SIDE + sin(prop.age * 2.2) * TRAIL_SWAY
        val targetX = at.x + backX - cos(radians) * side
        val targetY = at.y + TRAIL_UP + sin(prop.age * 3.0) * TRAIL_BOB
        val targetZ = at.z + backZ - sin(radians) * side

        val now = System.nanoTime()
        if (!prop.placed) {
            prop.x = targetX
            prop.y = targetY
            prop.z = targetZ
            prop.placed = true
        } else {
            val seconds = ((now - prop.lastFrameNanos) / 1.0e9).coerceIn(0.0, 0.25)
            val share = 1.0 - exp(-seconds * TRAIL_CATCH_UP)
            prop.x += (targetX - prop.x) * share
            prop.y += (targetY - prop.y) * share
            prop.z += (targetZ - prop.z) * share
        }
        prop.lastFrameNanos = now
        prop.yaw = yaw + 180f
    }

    private fun hop(wearer: Entity, partialTick: Float): Double {
        if (wearer !is LivingEntity) return 0.0
        val speed = wearer.walkAnimation.speed(partialTick).coerceIn(0f, 1f)
        if (speed < 0.01f) return 0.0
        return abs(sin(wearer.walkAnimation.position(partialTick) * HOP_PACE)) * speed * HOP_HEIGHT
    }

    private fun lerpDegrees(from: Float, to: Float, t: Float): Float {
        var delta = (to - from) % 360f
        if (delta >= 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return from + delta * t
    }

    private fun lightAt(level: net.minecraft.world.level.Level, prop: Prop): Int {
        var block = 0
        var sky = 0
        for (lift in LIGHT_SAMPLES) {
            val at = BlockPos.containing(prop.x, prop.y + lift, prop.z)
            block = maxOf(block, level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at))
            sky = maxOf(sky, level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at))
        }
        return LightCoordsUtil.pack(block, sky)
    }
}
