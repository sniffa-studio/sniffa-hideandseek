package studio.sniffa.client.prop

import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.GeoRenderState
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

class CrowModel : GeoModel<Crow>() {

    override fun getModelResource(state: GeoRenderState): Identifier = Hideandseek.id("crow")

    override fun getTextureResource(state: GeoRenderState): Identifier = Hideandseek.id("textures/prop/crow.png")

    override fun getAnimationResource(crow: Crow): Identifier = Hideandseek.id("crow")
}
