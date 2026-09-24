package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class ZonePickPayload(val x: Int, val z: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("zone_pick")

        val TYPE: CustomPacketPayload.Type<ZonePickPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, ZonePickPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZonePickPayload::x,
            ByteBufCodecs.VAR_INT, ZonePickPayload::z,
            ::ZonePickPayload,
        )
    }
}
