package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.BlendFactor
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import studio.sniffa.Hideandseek
import studio.sniffa.client.mixin.RenderTypeAccess
import kotlin.math.cos
import kotlin.math.sin

object HeldLantern {

    private val MODEL = Hideandseek.id("held_lantern")
    private val HAND_MODEL = Hideandseek.id("held_lantern_hand")
    private var handStack: ItemStack? = null

    @JvmField val FIST_X = 0.64f
    @JvmField val FIST_Y = 0.05f
    @JvmField val FIST_Z = -1.05f
    @JvmField val LEAN = 30f
    @JvmField val INWARD = 50f
    @JvmField val HAND = 9f
    @JvmField val SIZE = 0.58f
    @JvmField val HANG = 0.7f
    @JvmField val TURN = 30f
    @JvmField val TOP = 19f / 16f - 0.5f
    @JvmField val BODY = 4.5f / 16f - 0.5f
    @JvmField val ARM_LIGHT = 13
    @JvmField val HALO_RADIUS = 0.34f
    @JvmField val HALO_BRIGHT = 0.4f

    private const val HALO_STEPS = 20
    private const val WARM_R = 1f
    private const val WARM_G = 0.76f
    private const val WARM_B = 0.44f

    private const val SPRING = 0.07f
    private const val DAMPING = 0.16f
    private const val TURN_PUSH = 0.0035f
    private const val SPEED_PUSH = 0.9f
    private const val WALK_SWAY = 0.175f
    private const val MOST = 0.35f

    private var sideAngle = 0f
    private var sideSpeed = 0f
    private var sideBefore = 0f
    private var tiltAngle = 0f
    private var tiltSpeed = 0f
    private var tiltBefore = 0f
    private var lastYaw = Float.NaN
    private var lastForward = 0f

    private val GLOW_PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/hand_glow"))
        .withVertexShader(Hideandseek.id("core/handglow"))
        .withFragmentShader(Hideandseek.id("core/handglow"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build()

    private val GLOW: RenderType by lazy {
        RenderTypeAccess.`hideandseek$create`("hideandseek_hand_glow", RenderSetup.builder(GLOW_PIPELINE).createRenderSetup())
    }

    fun install() {
        RenderPipelines.register(GLOW_PIPELINE)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    @JvmStatic
    fun glow(): RenderType = GLOW

    @JvmStatic
    fun matches(stack: ItemStack): Boolean = stack.get(DataComponents.ITEM_MODEL) == MODEL

    @JvmStatic
    fun hand(): ItemStack = handStack ?: ItemStack(Items.LANTERN).also {
        it.set(DataComponents.ITEM_MODEL, HAND_MODEL)
        handStack = it
    }

    @JvmStatic
    fun haloStrength(): Float {
        val seconds = (System.currentTimeMillis() % 3_600_000L) / 1000f
        return HALO_BRIGHT * (0.95f + 0.03f * sin(seconds * 1.7f) + 0.02f * sin(seconds * 2.9f + 1.3f))
    }

    @JvmStatic
    fun halo(pose: PoseStack.Pose, consumer: VertexConsumer, strength: Float) {
        val hole = HALO_RADIUS * 0.2f
        val inner = HALO_RADIUS * 0.42f
        for (i in 0 until HALO_STEPS) {
            val a0 = (i * 2.0 * Math.PI / HALO_STEPS).toFloat()
            val a1 = ((i + 1) * 2.0 * Math.PI / HALO_STEPS).toFloat()
            ring(pose, consumer, a0, a1, hole, inner, 0f, strength)
            ring(pose, consumer, a0, a1, inner, HALO_RADIUS, strength, 0f)
        }
    }

    private fun ring(pose: PoseStack.Pose, consumer: VertexConsumer, a0: Float, a1: Float, near: Float, far: Float, nearAlpha: Float, farAlpha: Float) {
        val corners = arrayOf(
            floatArrayOf(cos(a0) * near, sin(a0) * near, nearAlpha),
            floatArrayOf(cos(a0) * far, sin(a0) * far, farAlpha),
            floatArrayOf(cos(a1) * far, sin(a1) * far, farAlpha),
            floatArrayOf(cos(a1) * near, sin(a1) * near, nearAlpha),
        )
        for (c in corners) consumer.addVertex(pose, c[0], c[1], 0f).setColor(WARM_R, WARM_G, WARM_B, c[2])
    }

    @JvmStatic
    fun side(partial: Float): Float = Mth.lerp(partial, sideBefore, sideAngle)

    @JvmStatic
    fun tilt(partial: Float): Float = Mth.lerp(partial, tiltBefore, tiltAngle)

    private fun tick(client: Minecraft) {
        sideBefore = sideAngle
        tiltBefore = tiltAngle
        val player = client.player
        if (player == null) {
            lastYaw = Float.NaN
            sideAngle = 0f; sideSpeed = 0f; tiltAngle = 0f; tiltSpeed = 0f
            sideBefore = 0f; tiltBefore = 0f
            return
        }
        val yaw = player.yRot
        val turn = if (lastYaw.isNaN()) 0f else Mth.wrapDegrees(yaw - lastYaw)
        lastYaw = yaw
        val radians = Math.toRadians(yaw.toDouble())
        val motion = player.deltaMovement
        val forward = (-sin(radians) * motion.x + cos(radians) * motion.z).toFloat()
        val push = forward - lastForward
        lastForward = forward

        val walk = player.walkAnimation
        val sway = sin(walk.position() * 0.6662f) * walk.speed() * WALK_SWAY

        sideSpeed += -SPRING * (sideAngle - sway) - DAMPING * sideSpeed - turn * TURN_PUSH
        tiltSpeed += -SPRING * tiltAngle - DAMPING * tiltSpeed - push * SPEED_PUSH
        sideAngle = (sideAngle + sideSpeed).coerceIn(-MOST, MOST)
        tiltAngle = (tiltAngle + tiltSpeed).coerceIn(-MOST, MOST)
    }
}
