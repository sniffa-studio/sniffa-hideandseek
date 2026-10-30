package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class MomentPayload(val name: String, val detail: String, val colour: Int, val millis: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("moment")

        val TYPE: CustomPacketPayload.Type<MomentPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, MomentPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MomentPayload::name,
            ByteBufCodecs.STRING_UTF8, MomentPayload::detail,
            ByteBufCodecs.VAR_INT, MomentPayload::colour,
            ByteBufCodecs.VAR_INT, MomentPayload::millis,
            ::MomentPayload,
        )
    }
}
