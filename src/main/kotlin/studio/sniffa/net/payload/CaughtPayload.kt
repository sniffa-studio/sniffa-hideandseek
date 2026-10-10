package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class CaughtPayload(val seeker: String, val place: Int, val total: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("caught")

        val TYPE: CustomPacketPayload.Type<CaughtPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, CaughtPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CaughtPayload::seeker,
            ByteBufCodecs.VAR_INT, CaughtPayload::place,
            ByteBufCodecs.VAR_INT, CaughtPayload::total,
            ::CaughtPayload,
        )
    }
}
