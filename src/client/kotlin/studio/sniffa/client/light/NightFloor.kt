package studio.sniffa.client.light

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.state.LightmapRenderState
import net.minecraft.world.attribute.EnvironmentAttributes
import org.joml.Vector3f
import studio.sniffa.client.border.BorderZone
import kotlin.math.max

object NightFloor {

    private const val RED = 0.11f
    private const val GREEN = 0.12f
    private const val BLUE = 0.16f

    @JvmStatic
    fun lift(state: LightmapRenderState, partial: Float) {
        if (BorderZone.current() == null) return
        val client = Minecraft.getInstance()
        if (client.level == null) return
        val stars = client.gameRenderer.mainCamera().attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, partial) as Float
        val night = (stars * 2.2f).coerceIn(0f, 1f)
        if (night <= 0f) return
        val ambient = state.ambientColor
        state.ambientColor = Vector3f(max(ambient.x(), RED * night), max(ambient.y(), GREEN * night), max(ambient.z(), BLUE * night))
    }
}
