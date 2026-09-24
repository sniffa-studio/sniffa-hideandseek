package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class MapSlicePayload(val hash: Int, val index: Int, val bytes: ByteArray) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    override fun equals(other: Any?): Boolean =
        other is MapSlicePayload && hash == other.hash && index == other.index && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = 31 * (31 * hash + index) + bytes.contentHashCode()

    companion object {
        val ID: Identifier = Hideandseek.id("map_data")

        val TYPE: CustomPacketPayload.Type<MapSlicePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, MapSlicePayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapSlicePayload::hash,
            ByteBufCodecs.VAR_INT, MapSlicePayload::index,
            ByteBufCodecs.BYTE_ARRAY, MapSlicePayload::bytes,
            ::MapSlicePayload,
        )
    }
}
