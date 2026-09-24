package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class RadarPayload(val count: Int, val range: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("radar")

        val TYPE: CustomPacketPayload.Type<RadarPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, RadarPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RadarPayload::count,
            ByteBufCodecs.VAR_INT, RadarPayload::range,
            ::RadarPayload,
        )
    }
}
