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

    enum class Card(val euros: Int, val model: Identifier, val glow: Int, val halo: Float, val sparkle: Float, val dust: Float, val orbiters: Int, val bright: Float) {
        FIVE(5, Hideandseek.id("props/gutschein_5"), 0x5AA8FF, 0.6f, 0.05f, 0.4f, 3, 0.8f),
        TEN(10, Hideandseek.id("props/gutschein_10"), 0x4F8DFF, 0.6f, 0.05f, 0.4f, 3, 0.8f),
        FIFTEEN(15, Hideandseek.id("props/gutschein_15"), 0xFF3B4E, 0.7f, 0.12f, 0.5f, 4, 1.1f),
    }

    private const val SCAN_EVERY_TICKS = 10
    private const val LIFT = 0.6
    private const val SHOWN_WITHIN = 32.0
    private const val SPARKLE_REACH = 0.3
    private const val FADE_FROM = 22f
    private const val FADE_TO = 36f
    private const val ORBIT_RADIUS = 0.34f
    private const val ORBIT_SIZE = 0.045f
    private const val ORBIT_SECONDS = 5.0
    private const val TWINKLE_EVERY = 2.6
    private const val TWINKLE_FOR = 0.55
    private const val TWINKLE_SIZE = 0.085f

    private val cards = HashMap<Int, Pair<Display.ItemDisplay, Card>>()

    private class Flash(val x: Double, val y: Double, val z: Double, val card: Card, val start: Long)

    private val flashes = ArrayList<Flash>()
    private const val FLASH_TICKS = 9f
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

    private val GLINT: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/voucher_glint"))
        .withVertexShader(Hideandseek.id("core/voucherglint"))
        .withFragmentShader(Hideandseek.id("core/voucherglint"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    private const val GLINT_WITHIN = 24.0
    private const val GLINT_STRENGTH = 0.32f
    private const val FACE_HALF_WIDTH = 0.5f
    private const val FACE_HALF_HEIGHT = 0.3125f
    private const val FACE_DEPTH = 0.0185f

    private val SPARKLES: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/voucher_sparkle"))
        .withVertexShader(Hideandseek.id("core/vouchersparkle"))
        .withFragmentShader(Hideandseek.id("core/vouchersparkle"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun flash(euros: Int, x: Double, y: Double, z: Double) {
        val card = Card.entries.firstOrNull { it.euros == euros } ?: return
        val level = Minecraft.getInstance().level ?: return
        flashes += Flash(x, y, z, card, level.gameTime)
    }

    fun cardOf(model: Identifier?): Card? = Card.entries.firstOrNull { it.model == model }

    fun install() {
        RenderPipelines.register(PIPELINE)
        RenderPipelines.register(SPARKLES)
        RenderPipelines.register(GLINT)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { cards.clear(); flashes.clear() } }
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
        if (cards.isEmpty()) return
        if (client.isPaused) return
        val camera = client.gameRenderer.mainCamera().position()
        val random = level.random
        for ((display, card) in cards.values) {
            val at = display.position()
            if (camera.distanceToSqr(at.x, at.y + LIFT, at.z) > SHOWN_WITHIN * SHOWN_WITHIN) continue
            if (random.nextFloat() >= card.sparkle) continue
            val angle = random.nextDouble() * Math.PI * 2
            val reach = SPARKLE_REACH * sqrt(random.nextDouble())
            val x = at.x + cos(angle) * reach
            val y = at.y + LIFT + (random.nextDouble() - 0.5) * 0.4
            val z = at.z + sin(angle) * reach
            level.addParticle(DustParticleOptions(card.glow, card.dust), x, y, z, 0.0, 0.01, 0.0)
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
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        drawFlashes(client, camera, partial)
        if (cards.isEmpty()) return
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
                    pass(client, PIPELINE, vertices, shown.size * 6)
                }
            }
        }
        drawSparkles(client, shown, camera, partial)
        drawGlints(client, shown, camera, partial)
    }

    private fun drawGlints(client: Minecraft, shown: List<Pair<Display.ItemDisplay, Card>>, camera: net.minecraft.world.phys.Vec3, partial: Float) {
        val near = shown.filter { (display, _) ->
            val at = display.getPosition(partial)
            camera.distanceToSqr(at.x, at.y, at.z) <= GLINT_WITHIN * GLINT_WITHIN
        }
        if (near.isEmpty()) return
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 8 * near.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            val corner = Vector3f()
            for ((display, _) in near) {
                val state = display.renderState() ?: continue
                val pose = Matrix4f(state.transformation().get(display.calculateInterpolationProgress(partial)).matrix)
                pose.rotateY(Math.PI.toFloat())
                val at = display.getPosition(partial)
                val phase = ((display.id * 0.3819660f) % 1f)
                for (side in floatArrayOf(FACE_DEPTH, -FACE_DEPTH)) {
                    for ((u, v) in arrayOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)) {
                        val mirrored = if (side > 0) u else 1f - u
                        pose.transformPosition(corner.set((mirrored * 2f - 1f) * FACE_HALF_WIDTH, (1f - v * 2f) * FACE_HALF_HEIGHT, side))
                        builder.addVertex((at.x - camera.x).toFloat() + corner.x, (at.y - camera.y).toFloat() + corner.y, (at.z - camera.z).toFloat() + corner.z)
                            .setUv(u, v).setColor(GLINT_STRENGTH, 0f, 0f, phase)
                    }
                }
            }
            val built = builder.build() ?: return
            built.use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa voucher glints" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(client, GLINT, vertices, near.size * 12)
                }
            }
        }
    }

    private fun drawFlashes(client: Minecraft, camera: net.minecraft.world.phys.Vec3, partial: Float) {
        if (flashes.isEmpty()) return
        val now = (client.level?.gameTime ?: return) + partial
        flashes.removeIf { now - it.start > FLASH_TICKS }
        if (flashes.isEmpty()) return
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * flashes.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (flash in flashes) {
                val life = ((now - flash.start) / FLASH_TICKS).coerceIn(0f, 1f)
                val size = flash.card.halo * (1.2f + 2.4f * life)
                val strength = (1f - life) * (1f - life) * 1.6f
                val r = ((flash.card.glow shr 16) and 0xFF) / 255f
                val g = ((flash.card.glow shr 8) and 0xFF) / 255f
                val b = (flash.card.glow and 0xFF) / 255f
                val x = (flash.x - camera.x).toFloat()
                val y = (flash.y - camera.y).toFloat()
                val z = (flash.z - camera.z).toFloat()
                quad(builder, x, y, z, size, (r * strength + 0.25f * strength).coerceAtMost(1f), (g * strength + 0.25f * strength).coerceAtMost(1f), (b * strength + 0.25f * strength).coerceAtMost(1f), 1f)
            }
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa voucher flashes" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(client, SPARKLES, vertices, flashes.size * 6)
                }
            }
        }
    }

    private fun drawSparkles(client: Minecraft, shown: List<Pair<Display.ItemDisplay, Card>>, camera: net.minecraft.world.phys.Vec3, partial: Float) {
        val seconds = ((client.level?.gameTime ?: 0L) + partial) / 20.0
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        val most = shown.sumOf { it.second.orbiters + 1 }
        var quads = 0
        ByteBufferBuilder(format.vertexSize * 4 * most).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for ((display, card) in shown) {
                val at = display.getPosition(partial)
                if (camera.distanceToSqr(at.x, at.y + LIFT, at.z) > SHOWN_WITHIN * SHOWN_WITHIN) continue
                val cx = (at.x - camera.x).toFloat()
                val cy = (at.y + LIFT - camera.y).toFloat()
                val cz = (at.z - camera.z).toFloat()
                val r = ((card.glow shr 16) and 0xFF) / 255f
                val g = ((card.glow shr 8) and 0xFF) / 255f
                val b = (card.glow and 0xFF) / 255f
                val own = display.id * 0.37
                for (i in 0 until card.orbiters) {
                    val angle = (seconds / ORBIT_SECONDS + i.toDouble() / card.orbiters + own) * Math.PI * 2
                    val x = cx + (cos(angle) * ORBIT_RADIUS).toFloat()
                    val y = cy + (sin(angle * 1.7 + i) * 0.1).toFloat()
                    val z = cz + (sin(angle) * ORBIT_RADIUS).toFloat()
                    val twinkle = (0.55 + 0.45 * sin(seconds * 5.3 + i * 2.1 + own)).toFloat() * card.bright
                    quad(builder, x, y, z, ORBIT_SIZE, 0.75f + 0.25f * r, 0.75f + 0.25f * g, 0.75f + 0.25f * b, twinkle)
                    quads++
                }
                if (card == Card.FIFTEEN) {
                    val cycle = seconds / TWINKLE_EVERY + own
                    val into = (cycle - kotlin.math.floor(cycle)) * TWINKLE_EVERY
                    if (into < TWINKLE_FOR) {
                        val seed = kotlin.math.floor(cycle).toInt() * 7919 + display.id
                        val spot = ((seed * 0.61803398875) % 1.0) * Math.PI * 2
                        val life = (into / TWINKLE_FOR).toFloat()
                        val grow = sin(life * Math.PI).toFloat()
                        val x = cx + (cos(spot) * 0.22).toFloat()
                        val y = cy + 0.12f + ((seed % 5) - 2) * 0.03f
                        val z = cz + (sin(spot) * 0.22).toFloat()
                        quad(builder, x, y, z, TWINKLE_SIZE * grow, 1f, 0.92f, 0.85f, 1.3f * grow)
                        quads++
                    }
                }
            }
            if (quads == 0) return
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa voucher sparkles" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(client, SPARKLES, vertices, quads * 6)
                }
            }
        }
    }

    private fun quad(builder: BufferBuilder, x: Float, y: Float, z: Float, size: Float, r: Float, g: Float, b: Float, strength: Float) {
        for (corner in CORNERS) {
            builder.addVertex(x, y, z).setUv(corner[0] * size, corner[1] * size).setColor(r, g, b, strength.coerceIn(0f, 1f))
        }
    }

    private fun pass(client: Minecraft, pipeline: RenderPipeline, vertices: GpuBuffer, indexCount: Int) {
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val values = Matrix4f().zero().m00(FADE_FROM).m01(FADE_TO)
        val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), Vector4f(1f), Vector3f(), values)
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa voucher halos" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
