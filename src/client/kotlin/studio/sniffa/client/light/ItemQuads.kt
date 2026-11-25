package studio.sniffa.client.light

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.geometry.BakedQuad
import java.lang.reflect.Proxy

object ItemQuads {

    class Captured(val pose: PoseStack.Pose, val tints: IntArray, val quads: List<BakedQuad>, val foil: ItemStackRenderState.FoilType)

    fun capture(renderState: ItemStackRenderState, poseStack: PoseStack, light: Int): List<Captured>? {
        val out = ArrayList<Captured>()
        var unsupported = false
        val collector = Proxy.newProxyInstance(SubmitNodeCollector::class.java.classLoader, arrayOf(SubmitNodeCollector::class.java)) { proxy, method, args ->
            when {
                method.declaringClass == Any::class.java -> when (method.name) {
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.get(0)
                    else -> "DekoBatchCapture"
                }
                method.name == "submitItem" && args != null && args.size == 8 -> {
                    @Suppress("UNCHECKED_CAST")
                    out.add(Captured((args[0] as PoseStack).last().copy(), args[5] as IntArray, ArrayList(args[6] as List<BakedQuad>), args[7] as ItemStackRenderState.FoilType))
                    null
                }
                else -> {
                    unsupported = true
                    if (method.returnType.isInstance(proxy)) proxy else null
                }
            }
        } as SubmitNodeCollector
        renderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0)
        return if (unsupported) null else out
    }
}
