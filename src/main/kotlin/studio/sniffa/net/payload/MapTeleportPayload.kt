package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class MapTeleportPayload(val x: Int, val z: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("tp")

        val TYPE: CustomPacketPayload.Type<MapTeleportPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, MapTeleportPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapTeleportPayload::x,
            ByteBufCodecs.VAR_INT, MapTeleportPayload::z,
            ::MapTeleportPayload,
        )
    }
}
