package studio.sniffa.client.border

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import java.util.Optional
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.client.zone.DangerZone
import java.util.OptionalDouble

object BorderRenderer {

    private val roundBorder = Layer(dome = true)
    private val dangerZone = Layer(dome = false)

    private val roundSeam = BorderSeam()
    private val zoneSeam = BorderSeam()

    private const val ZONE_TINT = 0xFFFC301E.toInt()

    private class Layer(val dome: Boolean) {
        var mesh: BorderMesh? = null

        fun meshFor(ring: BorderZone.Ring): BorderMesh {
            mesh?.let { if (it.scaleFor(ring) != null) return it else it.close() }
            return BorderMesh.build(ring, dome).also { mesh = it }
        }

        fun drop() {
            mesh?.close()
            mesh = null
        }
    }

    fun install() {
        BorderPipeline.install()
        BorderEffects.install()

        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain {
            val camera = Minecraft.getInstance().gameRenderer.mainCamera().position()

            val ring = BorderZone.current()
            if (ring == null) {
                roundSeam.drop()
            } else {
                draw(roundBorder, BorderPipeline.RING, ring, BorderZone.tint(), camera) {
                    BorderEffects.uniforms(ring, it.columns, camera)
                }
                roundSeam.meshFor(ring, camera)?.let { seam ->
                    pass(BorderPipeline.SEAM, seam.vertices, seam.indexCount, ring, BorderZone.tint(), camera, roundSeam.uniforms(seam.reach, BorderZone.motion()), roundSeam.scaleFor(ring))
                }
            }

            val zone = DangerZone.current()
            if (zone == null) {
                dangerZone.drop()
                zoneSeam.drop()
            } else {
                val zoneRing = BorderZone.Ring(zone.centerX, zone.centerZ, zone.radius)
                draw(dangerZone, BorderPipeline.ZONE, zoneRing, ZONE_TINT, camera) {
                    zoneUniforms(it.columns)
                }
                zoneSeam.meshFor(zoneRing, camera)?.let { seam ->
                    pass(BorderPipeline.SEAM, seam.vertices, seam.indexCount, zoneRing, ZONE_TINT, camera, zoneSeam.uniforms(seam.reach, BorderZone.Motion.STATIONARY), zoneSeam.scaleFor(zoneRing))
                }
            }
        })
    }

    private fun draw(
        layer: Layer,
        pipeline: RenderPipeline,
        ring: BorderZone.Ring,
        tint: Int,
        camera: Vec3,
        extras: (BorderMesh) -> Matrix4f,
    ) {
        val current = layer.meshFor(ring)
        val values = extras(current)
        values.m23(if (current.dome) BorderMesh.DOME_BASE.toFloat() else 0f)
        pass(pipeline, current.vertices, current.indexCount, ring, tint, camera, values, current.scaleFor(ring) ?: 1.0)
    }

    private fun zoneUniforms(columns: Int): Matrix4f {
        val values = FloatArray(16)
        values[0] = DangerZone.secondsLeft().toFloat()
        values[12] = columns.toFloat()
        return Matrix4f().set(values)
    }

    private fun pass(
        pipeline: RenderPipeline,
        vertices: GpuBuffer,
        indexCount: Int,
        ring: BorderZone.Ring,
        tint: Int,
        camera: Vec3,
        extras: Matrix4f,
        scale: Double,
    ) {
        extras.m13(scale.toFloat())
        val client = Minecraft.getInstance()

        val weather = client.levelRenderer.weatherTarget()
        val target = weather ?: client.gameRenderer.mainRenderTarget()

        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return

        val offset = Vector3f(
            (ring.centerX - camera.x).toFloat(),
            (-camera.y).toFloat(),
            (ring.centerZ - camera.z).toFloat(),
        )

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            Vector4f(
                ARGB.red(tint) / 255f,
                ARGB.green(tint) / 255f,
                ARGB.blue(tint) / 255f,
                1f,
            ),
            offset,
            extras,
        )

        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa border" },
            colour,
            Optional.empty(),
            depth,
            OptionalDouble.empty(),
        ).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
