package studio.sniffa.client.border

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.client.zone.DangerZone
import java.util.OptionalDouble
import java.util.OptionalInt

object BorderRenderer {

    private val roundBorder = Layer()
    private val dangerZone = Layer()

    private const val ZONE_TINT = 0xFFFC301E.toInt()

    private class Layer {
        var mesh: BorderMesh? = null

        fun meshFor(ring: BorderZone.Ring): BorderMesh {
            mesh?.let { if (it.matches(ring)) return it else it.close() }
            return BorderMesh.build(ring).also { mesh = it }
        }

        fun drop() {
            mesh?.close()
            mesh = null
        }
    }

    fun install() {
        BorderPipeline.install()

        WorldRenderEvents.END_MAIN.register(WorldRenderEvents.EndMain {
            val camera = Minecraft.getInstance().gameRenderer.mainCamera.position()

            BorderZone.current()?.let {
                draw(roundBorder, BorderPipeline.RING, it, BorderZone.tint(), camera)
            }

            val zone = DangerZone.current()
            if (zone == null) {
                dangerZone.drop()
            } else {
                draw(
                    dangerZone,
                    BorderPipeline.ZONE,
                    BorderZone.Ring(zone.centerX, zone.centerZ, zone.radius),
                    ZONE_TINT,
                    camera,
                )
            }
        })
    }

    private fun draw(
        layer: Layer,
        pipeline: com.mojang.blaze3d.pipeline.RenderPipeline,
        ring: BorderZone.Ring,
        tint: Int,
        camera: Vec3,
    ) {
        val current = layer.meshFor(ring)

        val client = Minecraft.getInstance()

        val weather = client.levelRenderer.weatherTarget
        val target = weather ?: client.mainRenderTarget

        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return

        val offset = Vector3f(
            (ring.centerX - camera.x).toFloat(),
            (-camera.y).toFloat(),
            (ring.centerZ - camera.z).toFloat(),
        )

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(),
            Vector4f(
                ARGB.red(tint) / 255f,
                ARGB.green(tint) / 255f,
                ARGB.blue(tint) / 255f,
                1f,
            ),
            offset,
            Matrix4f(),
        )

        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa border" },
            colour,
            OptionalInt.empty(),
            depth,
            OptionalDouble.empty(),
        ).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(current.indexCount), indices.type())
            pass.setVertexBuffer(0, current.vertices)
            pass.drawIndexed(0, 0, current.indexCount, 1)
        }
    }
}
