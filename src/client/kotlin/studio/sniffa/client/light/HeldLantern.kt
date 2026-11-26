package studio.sniffa.client.light

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import studio.sniffa.Hideandseek
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

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    @JvmStatic
    fun matches(stack: ItemStack): Boolean = stack.get(DataComponents.ITEM_MODEL) == MODEL

    @JvmStatic
    fun hand(): ItemStack = handStack ?: ItemStack(Items.LANTERN).also {
        it.set(DataComponents.ITEM_MODEL, HAND_MODEL)
        handStack = it
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
