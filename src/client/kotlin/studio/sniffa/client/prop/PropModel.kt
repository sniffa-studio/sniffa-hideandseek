package studio.sniffa.client.prop

import com.geckolib.constant.dataticket.DataTicket
import com.geckolib.model.GeoModel
import com.geckolib.renderer.base.GeoRenderState
import net.minecraft.resources.Identifier
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
