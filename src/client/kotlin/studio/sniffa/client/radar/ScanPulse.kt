package studio.sniffa.client.radar

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuSampler
import java.util.OptionalDouble
import java.util.OptionalInt

object ScanPulse {

    private const val TRAVEL_MILLIS = 2600f

    private const val WAVES = 3
    private const val SPACING = 10.0f

    private const val THICKNESS = 1.3f

    private const val FLASH_STRENGTH = 1.4f
    private const val FLASH_MILLIS = 130f

    private val COLOUR = Vector3f(0.11f, 0.78f, 0.42f)

    @Volatile
    private var origin: Vec3? = null

    @Volatile
    private var range = 0.0f

    @Volatile
    private var startedAt = 0L

    private var quad: ScanQuad? = null

    private var depthSampler: GpuSampler? = null

    fun depthSampler(): GpuSampler = sampler()

    private fun sampler(): GpuSampler = depthSampler ?: RenderSystem.getDevice().createSampler(
        AddressMode.CLAMP_TO_EDGE,
        AddressMode.CLAMP_TO_EDGE,
        FilterMode.NEAREST,
        FilterMode.NEAREST,
        1,
        OptionalDouble.empty(),
    ).also { depthSampler = it }

    fun cast(x: Double, y: Double, z: Double, range: Int) {
        this.origin = Vec3(x, y, z)
        this.range = range.toFloat()
        this.startedAt = System.currentTimeMillis()
    }

    fun forget() {
        origin = null
        startedAt = 0L
    }

    fun install() {
        ScanPipeline.install()

        WorldRenderEvents.END_MAIN.register(WorldRenderEvents.EndMain {
            draw()
        })
    }

    fun inFlight(): Wave? {
        val centre = origin ?: return null

        val elapsed = (System.currentTimeMillis() - startedAt).toFloat()

        val life = TRAVEL_MILLIS * (1f + WAVES * SPACING / range.coerceAtLeast(1f))
        if (elapsed > life) {
            return null
        }

        val leading = elapsed / TRAVEL_MILLIS * range

        val fade = (1f - (elapsed / life - 0.5f) / 0.5f).coerceIn(0f, 1f)

        val flash = 1f + FLASH_STRENGTH * (1f - (elapsed / FLASH_MILLIS)).coerceIn(0f, 1f)

        return Wave(
            origin = centre,
            radius = leading,
            speed = range / (TRAVEL_MILLIS / 1000f),
            strength = fade * flash,
            seconds = elapsed / 1000f,
        )
    }

    data class Wave(
        val origin: Vec3,
        val radius: Float,
        val speed: Float,
        val strength: Float,
        val seconds: Float,
    )

    private fun draw() {
        val wave = inFlight() ?: run {
            origin = null
            return
        }

        val centre = wave.origin
        val leading = wave.radius
        val strength = wave.strength
        if (strength <= 0f) return

        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera.position()

        val target = client.mainRenderTarget
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return

        val current = quad ?: ScanQuad.build().also { quad = it }

        val viewOrigin = Vector3f(
            (centre.x - camera.x).toFloat(),
            (centre.y - camera.y).toFloat(),
            (centre.z - camera.z).toFloat(),
        ).mulPosition(RenderSystem.getModelViewMatrix())

        val parameters = Matrix4f()
            .m00(leading).m01(SPACING).m02(THICKNESS).m03(range)
            .m10(wave.seconds)

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            Matrix4f(),
            Vector4f(COLOUR.x, COLOUR.y, COLOUR.z, strength),
            viewOrigin,
            parameters,
        )

        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa sonar" },
            colour,
            OptionalInt.empty(),
        ).use { pass ->
            pass.setPipeline(ScanPipeline.PING)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.bindTexture("DepthSampler", depth, sampler())
            pass.setIndexBuffer(indices.getBuffer(current.indexCount), indices.type())
            pass.setVertexBuffer(0, current.vertices)
            pass.drawIndexed(0, 0, current.indexCount, 1)
        }
    }
}
