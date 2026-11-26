package studio.sniffa.client.snow

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.ARGB
import net.minecraft.world.attribute.EnvironmentAttributes
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import studio.sniffa.client.border.BorderZone
import java.util.Optional
import java.util.OptionalDouble
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object WinterSky {

    private const val ZENITH = 0xFF0A1329.toInt()
    private const val HORIZON = 0xFF2B2B44.toInt()
    private const val GLOW = 0xFF3C3C52.toInt()
    private const val CLOUD = 0xFF2C3250.toInt()

    private const val CLOUD_THINNING = 0.4f

    private const val NIGHT_GAIN = 2.2f

    private const val STAR_COUNT = 3400
    private const val STAR_DISTANCE = 100f
    private const val STAR_SEED = 0x5EA5DL

    private const val MOON_HEIGHT = 100f
    private const val HALO_SIZE = 72f

    private const val HAZE_RADIUS = 90f
    private const val HAZE_STEPS = 48
    private val HAZE_RINGS = floatArrayOf(-30f, -12f, -6f, -3f, -1f, 0f, 1f, 2.5f, 4.5f, 7f, 10f, 14f, 19f, 25f, 33f, 43f, 56f, 70f)

    private val STARS: RenderPipeline = pipeline("winter_stars", "core/winterstars", DefaultVertexFormat.POSITION_TEX_COLOR, BlendFunction.LIGHTNING, globals = true)
    private val HALO: RenderPipeline = pipeline("moon_halo", "core/moonhalo", DefaultVertexFormat.POSITION_TEX, BlendFunction.LIGHTNING, globals = false)
    private val HAZE: RenderPipeline = pipeline("sky_haze", "core/skyhaze", DefaultVertexFormat.POSITION_TEX, BlendFunction.TRANSLUCENT, globals = false)

    private var stars: GpuBuffer? = null
    private var starIndices = 0
    private var halo: GpuBuffer? = null
    private var haze: GpuBuffer? = null
    private var hazeIndices = 0

    @JvmStatic
    var strength = 0f
        private set

    private fun pipeline(name: String, shader: String, format: VertexFormat, blend: BlendFunction, globals: Boolean): RenderPipeline {
        val builder = RenderPipeline.builder()
            .withLocation(Hideandseek.id("pipeline/$name"))
            .withVertexShader(Hideandseek.id(shader))
            .withFragmentShader(Hideandseek.id(shader))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        if (globals) builder.withBindGroupLayout(BindGroupLayouts.GLOBALS)
        return builder
            .withVertexBinding(0, format)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState(blend))
            .withCull(false)
            .build()
    }

    fun install() {
        RenderPipelines.register(STARS)
        RenderPipelines.register(HALO)
        RenderPipelines.register(HAZE)
    }

    private fun night(camera: Camera, partial: Float): Float {
        if (BorderZone.current() == null) return 0f
        val stars = camera.attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, partial) as Float
        return (stars * NIGHT_GAIN).coerceIn(0f, 1f)
    }

    @JvmStatic
    fun extract(camera: Camera, partial: Float, overworld: Boolean, skyColor: Int): Int {
        strength = if (overworld) night(camera, partial) else 0f
        if (strength <= 0f) return skyColor
        return ARGB.srgbLerp(strength, skyColor, ZENITH)
    }

    @JvmStatic
    fun fogColor(camera: Camera, partial: Float, vanilla: Int): Int {
        val night = night(camera, partial)
        if (night <= 0f) return vanilla
        return ARGB.srgbLerp(night, vanilla, HORIZON)
    }

    @JvmStatic
    fun cloudColor(vanilla: Int): Int {
        if (strength <= 0f) return vanilla
        val alpha = ARGB.alpha(vanilla) * (1f - CLOUD_THINNING * strength)
        return ARGB.color(alpha.toInt(), ARGB.srgbLerp(strength, ARGB.opaque(vanilla), CLOUD))
    }

    @JvmStatic
    fun drawStars(poseStack: PoseStack, brightness: Float) {
        val buffer = stars ?: buildStars().also { stars = it }
        val level = (brightness * NIGHT_GAIN).coerceIn(0f, 1f)
        if (level <= 0f) return
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        modelView.mul(poseStack.last().pose())
        draw(STARS, "sniffa winter stars", buffer, starIndices, Matrix4f(modelView), Vector4f(1f, 1f, 1f, level), Matrix4f())
        modelView.popMatrix()
    }

    @JvmStatic
    fun drawMoonHalo(poseStack: PoseStack, rainBrightness: Float) {
        val fade = strength * rainBrightness
        if (fade <= 0f) return
        val buffer = halo ?: buildHalo().also { halo = it }
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushMatrix()
        modelView.mul(poseStack.last().pose())
        modelView.translate(0f, MOON_HEIGHT, 0f)
        modelView.scale(HALO_SIZE, 1f, HALO_SIZE)
        draw(HALO, "sniffa moon halo", buffer, 6, Matrix4f(modelView), Vector4f(0.8f, 0.86f, 1f, fade), Matrix4f())
        modelView.popMatrix()
    }

    @JvmStatic
    fun drawHaze() {
        if (strength <= 0f) return
        val buffer = haze ?: buildHaze().also { haze = it }
        val colours = Matrix4f().zero()
        colours.m00(ARGB.redFloat(HORIZON)).m01(ARGB.greenFloat(HORIZON)).m02(ARGB.blueFloat(HORIZON))
        colours.m10(ARGB.redFloat(GLOW)).m11(ARGB.greenFloat(GLOW)).m12(ARGB.blueFloat(GLOW))
        draw(HAZE, "sniffa sky haze", buffer, hazeIndices, RenderSystem.getModelViewMatrixCopy(), Vector4f(1f, 1f, 1f, strength), colours)
    }

    private fun draw(pipeline: RenderPipeline, label: String, vertices: GpuBuffer, indexCount: Int, modelView: Matrix4f, modulator: Vector4f, extras: Matrix4f) {
        val target = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val transform = RenderSystem.getDynamicUniforms().writeTransform(modelView, modulator, Vector3f(), extras)
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ label }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }

    private fun buildStars(): GpuBuffer {
        val random = Random(STAR_SEED)
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * STAR_COUNT).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            var placed = 0
            while (placed < STAR_COUNT) {
                val x = random.nextFloat() * 2f - 1f
                val y = random.nextFloat() * 2f - 1f
                val z = random.nextFloat() * 2f - 1f
                val length = x * x + y * y + z * z
                if (length <= 0.01f || length >= 1f) continue
                val centre = Vector3f(x, y, z).normalize()
                val bright = random.nextFloat() < 0.06f
                val size = if (bright) 0.26f + random.nextFloat() * 0.1f else 0.13f + random.nextFloat() * 0.08f
                val glow = if (bright) 0.85f + random.nextFloat() * 0.15f else 0.32f + 0.5f * random.nextFloat().let { it * it }
                val warmth = random.nextFloat()
                val red = glow * (if (warmth > 0.88f) 1f else 0.86f)
                val green = glow * (if (warmth > 0.88f) 0.94f else 0.91f)
                val blue = glow * (if (warmth > 0.88f) 0.86f else 1f)
                val phase = random.nextFloat()
                val pole = if (abs(centre.y) > 0.95f) Vector3f(1f, 0f, 0f) else Vector3f(0f, 1f, 0f)
                val right = Vector3f(centre).cross(pole).normalize().mul(size)
                val up = Vector3f(centre).cross(right).normalize().mul(size)
                centre.mul(STAR_DISTANCE)
                for ((u, v) in CORNERS) {
                    builder.addVertex(centre.x + right.x * u + up.x * v, centre.y + right.y * u + up.y * v, centre.z + right.z * u + up.z * v)
                        .setUv(u, v)
                        .setColor(red, green, blue, phase)
                }
                placed++
            }
            builder.buildOrThrow().use { mesh ->
                starIndices = mesh.drawState().indexCount()
                return RenderSystem.getDevice().createBuffer({ "sniffa winter stars" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
            }
        }
    }

    private val CORNERS = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)

    private fun buildHalo(): GpuBuffer {
        val format = DefaultVertexFormat.POSITION_TEX
        ByteBufferBuilder(format.vertexSize * 4).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for ((u, v) in CORNERS) builder.addVertex(u, 0f, v).setUv(u, v)
            builder.buildOrThrow().use { mesh ->
                return RenderSystem.getDevice().createBuffer({ "sniffa moon halo" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
            }
        }
    }

    private fun buildHaze(): GpuBuffer {
        val format = DefaultVertexFormat.POSITION_TEX
        val quads = HAZE_STEPS * (HAZE_RINGS.size - 1)
        ByteBufferBuilder(format.vertexSize * 4 * quads).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (step in 0 until HAZE_STEPS) {
                val a0 = step * 2.0 * PI / HAZE_STEPS
                val a1 = (step + 1) * 2.0 * PI / HAZE_STEPS
                for (ring in 0 until HAZE_RINGS.size - 1) {
                    val e0 = HAZE_RINGS[ring]
                    val e1 = HAZE_RINGS[ring + 1]
                    hazeVertex(builder, a0, e0)
                    hazeVertex(builder, a1, e0)
                    hazeVertex(builder, a1, e1)
                    hazeVertex(builder, a0, e1)
                }
            }
            builder.buildOrThrow().use { mesh ->
                hazeIndices = mesh.drawState().indexCount()
                return RenderSystem.getDevice().createBuffer({ "sniffa sky haze" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
            }
        }
    }

    private fun hazeVertex(builder: BufferBuilder, azimuth: Double, elevation: Float) {
        val tilt = Math.toRadians(elevation.toDouble())
        val flat = cos(tilt) * HAZE_RADIUS
        builder.addVertex((cos(azimuth) * flat).toFloat(), (sin(tilt) * HAZE_RADIUS).toFloat(), (sin(azimuth) * flat).toFloat())
            .setUv(elevation, 0f)
    }
}
