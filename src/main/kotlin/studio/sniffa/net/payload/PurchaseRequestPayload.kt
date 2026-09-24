package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class PurchaseRequestPayload(val abilityId: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("buy")

        val TYPE: CustomPacketPayload.Type<PurchaseRequestPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, PurchaseRequestPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PurchaseRequestPayload::abilityId,
            ::PurchaseRequestPayload,
        )
    }
}
