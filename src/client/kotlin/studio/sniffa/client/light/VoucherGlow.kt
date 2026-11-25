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
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Display
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object VoucherGlow {

    enum class Card(val model: Identifier, val glow: Int, val halo: Float, val sparkle: Float, val dust: Float) {
        FIVE(Hideandseek.id("props/gutschein_5"), 0x5AA8FF, 0.85f, 0.14f, 0.5f),
        TEN(Hideandseek.id("props/gutschein_10"), 0x4F8DFF, 0.85f, 0.14f, 0.5f),
        FIFTEEN(Hideandseek.id("props/gutschein_15"), 0xFF3B4E, 1.0f, 0.24f, 0.65f),
    }

    private const val SCAN_EVERY_TICKS = 10
    private const val LIFT = 0.75
    private const val SHOWN_WITHIN = 48.0
    private const val SPARKLE_REACH = 0.45
    private const val FADE_FROM = 32f
    private const val FADE_TO = 48f

    private val cards = HashMap<Int, Pair<Display.ItemDisplay, Card>>()
    private var ticks = 0

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/voucher_halo"))
        .withVertexShader(Hideandseek.id("core/voucherhalo"))
        .withFragmentShader(Hideandseek.id("core/voucherhalo"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun cardOf(model: Identifier?): Card? = Card.entries.firstOrNull { it.model == model }

    fun install() {
        RenderPipelines.register(PIPELINE)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { cards.clear() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            cards.clear()
            return
        }
        if (++ticks % SCAN_EVERY_TICKS == 0) scan(level)
        cards.values.removeIf { (display, _) -> display.isRemoved }
        if (client.isPaused || cards.isEmpty()) return
        val camera = client.gameRenderer.mainCamera().position()
        val random = level.random
        for ((display, card) in cards.values) {
            val at = display.position()
            if (camera.distanceToSqr(at.x, at.y + LIFT, at.z) > SHOWN_WITHIN * SHOWN_WITHIN) continue
            if (random.nextFloat() >= card.sparkle) continue
            val angle = random.nextDouble() * Math.PI * 2
            val reach = SPARKLE_REACH * sqrt(random.nextDouble())
            val x = at.x + cos(angle) * reach
            val y = at.y + LIFT + (random.nextDouble() - 0.5) * 0.6
            val z = at.z + sin(angle) * reach
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.WAX_OFF, x, y, z, 0.0, 0.02, 0.0)
            } else {
                level.addParticle(DustParticleOptions(card.glow, card.dust), x, y, z, 0.0, 0.01, 0.0)
            }
        }
    }

    private fun scan(level: ClientLevel) {
        val seen = HashSet<Int>()
        for (entity in level.entitiesForRendering()) {
            if (entity !is Display.ItemDisplay) continue
            val stack = entity.itemRenderState()?.itemStack() ?: continue
            val card = cardOf(stack.get(DataComponents.ITEM_MODEL)) ?: continue
            seen += entity.id
            cards[entity.id] = entity to card
        }
        cards.keys.retainAll(seen)
    }

    private val CORNERS = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))

    private fun draw() {
        if (cards.isEmpty()) return
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val shown = cards.values.filter { !it.first.isRemoved }
        if (shown.isEmpty()) return
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * shown.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for ((display, card) in shown) {
                val at = display.getPosition(partial)
                val x = (at.x - camera.x).toFloat()
                val y = (at.y + LIFT - camera.y).toFloat()
                val z = (at.z - camera.z).toFloat()
                val r = ((card.glow shr 16) and 0xFF) / 255f
                val g = ((card.glow shr 8) and 0xFF) / 255f
                val b = (card.glow and 0xFF) / 255f
                val phase = ((display.id * 0.6180339f) % 1f)
                for (corner in CORNERS) {
                    builder.addVertex(x, y, z).setUv(corner[0] * card.halo, corner[1] * card.halo).setColor(r, g, b, phase)
                }
            }
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa voucher halos" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(client, vertices, shown.size * 6)
                }
            }
        }
    }

    private fun pass(client: Minecraft, vertices: GpuBuffer, indexCount: Int) {
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val values = Matrix4f().zero().m00(FADE_FROM).m01(FADE_TO)
        val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), Vector4f(1f), Vector3f(), values)
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa voucher halos" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
