package studio.sniffa.client.prop

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import org.slf4j.LoggerFactory
import software.bernie.geckolib.renderer.GeoObjectRenderer
import software.bernie.geckolib.renderer.base.GeoRenderState
import software.bernie.geckolib.renderer.base.RenderPassInfo
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
    }

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
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            draw(context.matrices(), context)
        }
    }

    private fun draw(
        matrices: PoseStack,
        context: net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext,
    ) {
        if (standing.isEmpty()) return

        standing.removeIf { !it.alive() }
        if (standing.isEmpty()) return

        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val camera = client.gameRenderer.mainCamera.position()

        for (prop in standing) {
            matrices.pushPose()

            matrices.translate(prop.x - camera.x, prop.y - camera.y, prop.z - camera.z)

            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-prop.yaw))
            matrices.scale(prop.kind.scale, prop.kind.scale, prop.kind.scale)

            renderer.performRenderPass(
                prop,
                prop,
                matrices,
                context.commandQueue(),
                context.worldState().cameraRenderState,
                lightAt(level, prop),
                OverlayTexture.NO_OVERLAY,
            )

            matrices.popPose()
        }
    }

    private fun lightAt(level: net.minecraft.world.level.Level, prop: Prop): Int {
        val at = BlockPos.containing(prop.x, prop.y + 0.5, prop.z)
        return LightTexture.pack(
            level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at),
            level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at),
        )
    }
}
