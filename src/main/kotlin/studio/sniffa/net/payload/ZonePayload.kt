package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class ZonePayload(
    val x: Int,
    val z: Int,
    val radius: Int,
    val secondsLeft: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("zone")

        val TYPE: CustomPacketPayload.Type<ZonePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, ZonePayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZonePayload::x,
            ByteBufCodecs.VAR_INT, ZonePayload::z,
            ByteBufCodecs.VAR_INT, ZonePayload::radius,
            ByteBufCodecs.VAR_INT, ZonePayload::secondsLeft,
            ::ZonePayload,
        )
    }
}
