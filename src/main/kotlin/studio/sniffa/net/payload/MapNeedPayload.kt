package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class MapNeedPayload(val hash: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("map_need")

        val TYPE: CustomPacketPayload.Type<MapNeedPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, MapNeedPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapNeedPayload::hash,
            ::MapNeedPayload,
        )
    }
}
