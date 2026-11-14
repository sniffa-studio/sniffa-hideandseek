package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
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
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble

object FairyLights {

    val ANCHOR = Hideandseek.id("props/lichterkette")

    private const val SCAN_EVERY_TICKS = 10
    private const val SHOWN_WITHIN = 160.0
    private const val FADE_FROM = 120f
    private const val WIRE_WIDTH = 0.018f
    private const val BULB_SIZE = 0.5f
    private const val BULB_PUSH = 0.08f
    private const val BULB_GAIN = 1f
    private val WIRE_COLOUR = Vector4f(0.06f, 0.1f, 0.06f, 1f)

    private val lit = HashMap<Int, GarlandMesh>()
    private var ticks = 0

    private val WIRE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/garland_wire"))
        .withVertexShader(Hideandseek.id("core/garlandwire"))
        .withFragmentShader(Hideandseek.id("core/garlandwire"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    private val BULBS: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/garland_bulbs"))
        .withVertexShader(Hideandseek.id("core/garlandbulb"))
        .withFragmentShader(Hideandseek.id("core/garlandbulb"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(WIRE)
        RenderPipelines.register(BULBS)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun forget() {
        lit.values.forEach { it.close() }
        lit.clear()
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            if (lit.isNotEmpty()) forget()
            return
        }
        if (++ticks % SCAN_EVERY_TICKS == 0) scan(level)
    }

    private fun scan(level: ClientLevel) {
        val seen = HashSet<Int>()
        for (entity in level.entitiesForRendering()) {
            if (entity !is Display.ItemDisplay) continue
            val stack = entity.itemRenderState()?.itemStack() ?: continue
            if (stack.get(DataComponents.ITEM_MODEL) != ANCHOR) continue
            val garland = Garland.of(entity, stack) ?: continue
            seen += entity.id
            val known = lit[entity.id]
            if (known == null || known.garland != garland) {
                known?.close()
                lit[entity.id] = GarlandMesh.build(garland)
            }
        }
        val gone = lit.keys - seen
        for (id in gone) lit.remove(id)?.close()
    }

    private fun draw() {
        if (lit.isEmpty()) return
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return

        val wireValues = Matrix4f().zero().m00(WIRE_WIDTH)
        val bulbValues = Matrix4f().zero().m00(BULB_SIZE).m01(BULB_PUSH).m02(FADE_FROM).m03(SHOWN_WITHIN.toFloat())
        val bulbTint = Vector4f(1f, 1f, 1f, BULB_GAIN)
        for (mesh in lit.values) {
            val garland = mesh.garland
            if (!garland.within(camera.x, camera.y, camera.z, SHOWN_WITHIN)) continue
            val offset = offsetOf(garland, camera)
            pass(WIRE, mesh.wire, mesh.wireIndexCount, offset, WIRE_COLOUR, wireValues, colour, depth)
            mesh.bulbs?.let { pass(BULBS, it, mesh.bulbIndexCount, offset, bulbTint, bulbValues, colour, depth) }
        }
    }

    private fun offsetOf(garland: Garland, camera: Vec3): Vector3f =
        Vector3f((garland.x - camera.x).toFloat(), (garland.y - camera.y).toFloat(), (garland.z - camera.z).toFloat())

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
            { "sniffa fairy lights" },
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
