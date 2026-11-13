package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.world.phys.Vec3
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.BlendFactor
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.cos

object Spotlights {

    val HEAD = Hideandseek.id("props/scheinwerfer_kopf")

    private const val SCAN_EVERY_TICKS = 10
    private const val RAYS_PER_TICK = 2_500
    private const val RETRACE_MILLIS = 20_000L
    private const val REACH = 40.0
    private const val OUTER_DEGREES = 34.0
    private const val INNER_DEGREES = 16.0
    private const val TRACE_DEGREES = OUTER_DEGREES + 2.0
    private const val SHOWN_WITHIN = 192.0
    private const val FADE_FROM = 150f
    private const val WARMTH_RED = 1.0f
    private const val WARMTH_GREEN = 0.70f
    private const val WARMTH_BLUE = 0.36f
    private const val LIFT_GAIN = 5.5f
    private const val GLOW_GAIN = 0.6f
    private const val SHAFT_DEGREES = 15.0
    private const val SHAFT_GAIN = 0.35f
    private const val LENS_SIZE = 0.55f
    private const val LENS_PUSH = 0.18f

    private class Light(var beam: Beam) {
        var mesh: LitMesh? = null
        var shaft: ShaftMesh? = null
        var tracing: LitFaces? = null
        var retraceAt = 0L

        fun close() {
            mesh?.close()
            mesh = null
            shaft?.close()
            shaft = null
            tracing = null
        }
    }

    private val lights = HashMap<Int, Light>()
    private var ticks = 0

    private val LIFT: RenderPipeline = pipeline("spotlight_lift", BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE))
    private val ADDITIVE = BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)
    private val GLOW: RenderPipeline = pipeline("spotlight_glow", ADDITIVE)

    private val SHAFT: RenderPipeline = pipeline("spotlight_shaft", ADDITIVE, "core/spotbeam", DefaultVertexFormat.POSITION_TEX_COLOR)
    private val LENS: RenderPipeline = pipeline("spotlight_lens", ADDITIVE, "core/spotlens", DefaultVertexFormat.POSITION_TEX)

    private var lensQuad: GpuBuffer? = null

    private fun pipeline(
        name: String,
        blend: BlendFunction,
        shader: String = "core/spotlight",
        format: VertexFormat = DefaultVertexFormat.POSITION_COLOR,
    ): RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/$name"))
        .withVertexShader(Hideandseek.id(shader))
        .withFragmentShader(Hideandseek.id(shader))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, format)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(blend))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(LIFT)
        RenderPipelines.register(GLOW)
        RenderPipelines.register(SHAFT)
        RenderPipelines.register(LENS)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun forget() {
        lights.values.forEach { it.close() }
        lights.clear()
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            if (lights.isNotEmpty()) forget()
            return
        }
        if (++ticks % SCAN_EVERY_TICKS == 0) scan(level)

        val now = System.currentTimeMillis()
        val viewer = client.gameRenderer.mainCamera().position()
        var budget = RAYS_PER_TICK
        for (light in lights.values) {
            if (budget <= 0) break
            if (!light.beam.within(viewer.x, viewer.y, viewer.z, SHOWN_WITHIN)) continue
            val tracing = light.tracing ?: if (now >= light.retraceAt) {
                LitFaces(light.beam, REACH, cos(Math.toRadians(TRACE_DEGREES))).also { light.tracing = it }
            } else {
                continue
            }
            budget -= tracing.trace(level, budget)
            if (tracing.done) {
                val fresh = LitMesh.build(level, light.beam, tracing.faces)
                light.mesh?.close()
                light.mesh = fresh
                light.shaft?.close()
                light.shaft = ShaftMesh.build(light.beam, ShaftMesh.length(level, light.beam, REACH), SHAFT_DEGREES)
                light.tracing = null
                light.retraceAt = now + RETRACE_MILLIS
            }
        }
    }

    private fun scan(level: ClientLevel) {
        val seen = HashSet<Int>()
        for (entity in level.entitiesForRendering()) {
            if (entity !is Display.ItemDisplay) continue
            val stack = entity.itemRenderState()?.itemStack() ?: continue
            if (stack.get(DataComponents.ITEM_MODEL) != HEAD) continue
            val beam = Beam.of(entity) ?: continue
            seen += entity.id
            val light = lights[entity.id]
            if (light == null) {
                lights[entity.id] = Light(beam)
            } else if (beam.movedFrom(light.beam)) {
                light.beam = beam
                light.tracing = null
                light.retraceAt = 0L
            }
        }
        val gone = lights.keys - seen
        for (id in gone) lights.remove(id)?.close()
    }

    private fun draw() {
        if (lights.isEmpty()) return
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return

        for (light in lights.values) {
            if (!light.beam.within(camera.x, camera.y, camera.z, SHOWN_WITHIN)) continue
            light.mesh?.let { mesh ->
                val offset = offsetOf(mesh.beam, camera)
                pass(LIFT, mesh.vertices, mesh.indexCount, offset, WARM, surface(mesh.beam, LIFT_GAIN), colour, depth)
                pass(GLOW, mesh.vertices, mesh.indexCount, offset, WARM, surface(mesh.beam, GLOW_GAIN), colour, depth)
            }
            light.shaft?.let { shaft ->
                val values = Matrix4f().zero().m00(SHAFT_GAIN).m01(FADE_FROM)
                pass(SHAFT, shaft.vertices, shaft.indexCount, offsetOf(shaft.beam, camera), WARM, values, colour, depth)
            }
            lens(light.beam, camera, colour, depth)
        }
    }

    private val WARM = Vector4f(WARMTH_RED, WARMTH_GREEN, WARMTH_BLUE, 1f)

    private fun offsetOf(beam: Beam, camera: Vec3): Vector3f =
        Vector3f((beam.x - camera.x).toFloat(), (beam.y - camera.y).toFloat(), (beam.z - camera.z).toFloat())

    private fun surface(beam: Beam, gain: Float): Matrix4f {
        val axis = beam.axis
        val values = Matrix4f().zero()
        values.m00(axis.x)
        values.m01(axis.y)
        values.m02(axis.z)
        values.m03(REACH.toFloat())
        values.m10(cos(Math.toRadians(OUTER_DEGREES)).toFloat())
        values.m11(cos(Math.toRadians(INNER_DEGREES)).toFloat())
        values.m12(gain)
        values.m13(FADE_FROM)
        return values
    }

    private fun lens(beam: Beam, camera: Vec3, colour: GpuTextureView, depth: GpuTextureView) {
        val offset = offsetOf(beam, camera)
        val distance = offset.length()
        if (distance < 1.0e-3f) return
        val facing = -(offset.x * beam.axis.x + offset.y * beam.axis.y + offset.z * beam.axis.z) / distance
        val strength = facing.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
        if (strength <= 0.01f) return
        val quad = lensQuad ?: buildLensQuad().also { lensQuad = it }
        val values = Matrix4f().zero().m00(LENS_SIZE).m01(LENS_PUSH)
        pass(LENS, quad, 6, offset, Vector4f(WARMTH_RED, WARMTH_GREEN, WARMTH_BLUE, strength), values, colour, depth)
    }

    private fun buildLensQuad(): GpuBuffer {
        val format = DefaultVertexFormat.POSITION_TEX
        ByteBufferBuilder(format.vertexSize * 4).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            builder.addVertex(0f, 0f, 0f).setUv(-1f, -1f)
            builder.addVertex(0f, 0f, 0f).setUv(1f, -1f)
            builder.addVertex(0f, 0f, 0f).setUv(1f, 1f)
            builder.addVertex(0f, 0f, 0f).setUv(-1f, 1f)
            builder.buildOrThrow().use { mesh ->
                return RenderSystem.getDevice().createBuffer({ "sniffa spotlight lens" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
            }
        }
    }

    private fun pass(
        pipeline: RenderPipeline,
        vertices: GpuBuffer,
        indexCount: Int,
        offset: Vector3f,
        tint: Vector4f,
        values: Matrix4f,
        colour: GpuTextureView,
        depth: GpuTextureView,
    ) {
        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            tint,
            offset,
            values,
        )
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa spotlight" },
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
