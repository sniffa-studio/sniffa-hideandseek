package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class HandshakePayload(val protocolVersion: Int, val modVersion: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("handshake")

        val TYPE: CustomPacketPayload.Type<HandshakePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, HandshakePayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HandshakePayload::protocolVersion,
            ByteBufCodecs.STRING_UTF8, HandshakePayload::modVersion,
            ::HandshakePayload,
        )
    }
}
