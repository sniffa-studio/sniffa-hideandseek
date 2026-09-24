package studio.sniffa.client.radar

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.OptionalInt

object HeatMap {

    private const val SHOW_MILLIS = 6000f

    private const val RISE_MILLIS = 450f
    private const val FALL_MILLIS = 1200f

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/heat"))
        .withVertexShader(Hideandseek.id("core/heat"))
        .withFragmentShader(Hideandseek.id("core/heat"))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withBlend(BlendFunction.ADDITIVE)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build()

    @Volatile
    private var centre: Vec3? = null

    @Volatile
    private var radius = 0f

    @Volatile
    private var startedAt = 0L

    private var quad: ScanQuad? = null

    fun show(x: Double, y: Double, z: Double, radius: Int) {
        this.centre = Vec3(x, y, z)
        this.radius = radius.toFloat()
        this.startedAt = System.currentTimeMillis()
    }

    fun forget() {
        centre = null
        startedAt = 0L
    }

    fun install() {
        RenderPipelines.register(PIPELINE)

        WorldRenderEvents.END_MAIN.register(WorldRenderEvents.EndMain {
            draw()
        })
    }

    private const val BODY_SLOTS = 3

    private fun bodies(centre: Vec3, camera: Vec3): List<Vector3f> {
        val level = Minecraft.getInstance().level ?: return emptyList()
        val self = Minecraft.getInstance().player

        return level.players()
            .asSequence()
            .filter { it !== self }
            .filter { it.position().distanceTo(centre) <= radius }
            .sortedBy { it.position().distanceToSqr(centre) }
            .take(BODY_SLOTS)
            .map { player ->
                val at = player.position().add(0.0, player.bbHeight / 2.0, 0.0)
                Vector3f(
                    (at.x - camera.x).toFloat(),
                    (at.y - camera.y).toFloat(),
                    (at.z - camera.z).toFloat(),
                ).mulPosition(RenderSystem.getModelViewMatrix())
            }
            .toList()
    }

    private fun draw() {
        val at = centre ?: return

        val elapsed = (System.currentTimeMillis() - startedAt).toFloat()
        if (elapsed > SHOW_MILLIS) {
            centre = null
            return
        }

        val rising = (elapsed / RISE_MILLIS).coerceIn(0f, 1f)
        val falling = ((SHOW_MILLIS - elapsed) / FALL_MILLIS).coerceIn(0f, 1f)
        val strength = rising * falling
        if (strength <= 0f) return

        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera.position()

        val target = client.mainRenderTarget
        val colour = target.colorTextureView ?: return

        val current = quad ?: ScanQuad.build().also { quad = it }

        val warm = bodies(at, camera)
        if (warm.isEmpty()) return

        val seconds = elapsed / 1000f

        val parameters = Matrix4f()
            .m00(radius).m01(seconds)

        warm.forEachIndexed { slot, body ->
            when (slot) {
                0 -> parameters.m10(body.x).m11(body.y).m12(body.z).m13(1f)
                1 -> parameters.m20(body.x).m21(body.y).m22(body.z).m23(1f)
                2 -> parameters.m30(body.x).m31(body.y).m32(body.z).m33(1f)
            }
        }

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            Matrix4f(),
            Vector4f(1f, 1f, 1f, strength),
            Vector3f(),
            parameters,
        )

        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa heat" },
            colour,
            OptionalInt.empty(),
        ).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(current.indexCount), indices.type())
            pass.setVertexBuffer(0, current.vertices)
            pass.drawIndexed(0, 0, current.indexCount, 1)
        }
    }
}
