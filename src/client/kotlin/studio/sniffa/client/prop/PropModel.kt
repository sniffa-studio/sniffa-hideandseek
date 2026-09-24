package studio.sniffa.client.prop

import net.minecraft.resources.Identifier
import software.bernie.geckolib.constant.dataticket.DataTicket
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.base.GeoRenderState
import studio.sniffa.Hideandseek
import studio.sniffa.prop.PropKind

class PropModel : GeoModel<Prop>() {

    override fun addAdditionalStateData(prop: Prop, related: Any?, state: GeoRenderState) {
        state.addGeckolibData(KIND, prop.kind)
    }

    override fun getModelResource(state: GeoRenderState): Identifier =
        Hideandseek.id(kindOf(state).asset)

    override fun getTextureResource(state: GeoRenderState): Identifier =
        Hideandseek.id(kindOf(state).texture)

    override fun getAnimationResource(prop: Prop): Identifier =
        Hideandseek.id(prop.kind.asset)

    companion object {

        private val KIND: DataTicket<PropKind> = DataTicket.create("sniffa_prop_kind", PropKind::class.java)

        fun kindOf(state: GeoRenderState): PropKind =
            state.getOrDefaultGeckolibData(KIND, PropKind.RADAR) ?: PropKind.RADAR
    }
}
