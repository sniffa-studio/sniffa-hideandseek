package studio.sniffa.client.prop

import com.geckolib.constant.DataTickets
import com.geckolib.renderer.GeoObjectRenderer
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
import java.util.concurrent.CopyOnWriteArrayList

object Props {

    private val LOGGER = LoggerFactory.getLogger("${Hideandseek.MOD_ID}/props")

    private val standing = CopyOnWriteArrayList<Prop>()

    private val renderer: GeoObjectRenderer<Prop, Prop, GeoRenderState.Impl> = PropRenderer()

    private class PropRenderer : GeoObjectRenderer<Prop, Prop, GeoRenderState.Impl>(PropModel()) {

        override fun adjustRenderPose(pass: RenderPassInfo<GeoRenderState.Impl>) {
        }

        override fun addRenderData(animatable: Prop, relatedObject: Prop?, renderState: GeoRenderState.Impl, partialTick: Float) {
            renderState.addGeckolibData(DataTickets.TICK, animatable.age * TICKS_PER_SECOND)
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

    fun forget() {
        standing.clear()
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
        if (standing.isEmpty()) return

        standing.removeIf { !it.alive() }
        if (standing.isEmpty()) return

        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val camera = client.gameRenderer.mainCamera().position()

        for (prop in standing) {
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

    private fun lightAt(level: net.minecraft.world.level.Level, prop: Prop): Int {
        val at = BlockPos.containing(prop.x, prop.y + 0.5, prop.z)
        return LightCoordsUtil.pack(
            level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at),
            level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at),
        )
    }
}
