package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class LanternPayload(val uuid: String, val millis: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("lantern")

        val TYPE: CustomPacketPayload.Type<LanternPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, LanternPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LanternPayload::uuid,
            ByteBufCodecs.VAR_INT, LanternPayload::millis,
            ::LanternPayload,
        )
    }
}
