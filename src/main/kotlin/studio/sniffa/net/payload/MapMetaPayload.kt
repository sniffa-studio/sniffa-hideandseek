package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class MapMetaPayload(
    val hash: Int,
    val originX: Int,
    val originZ: Int,
    val size: Int,
    val sliceCount: Int,
    val compressedLength: Int,
    val palette: List<String>,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("map_meta")

        val TYPE: CustomPacketPayload.Type<MapMetaPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MapMetaPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, MapMetaPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): MapMetaPayload {
                    val hash = buf.readVarInt()
                    val originX = buf.readVarInt()
                    val originZ = buf.readVarInt()
                    val size = buf.readVarInt()
                    val sliceCount = buf.readVarInt()
                    val compressedLength = buf.readVarInt()
                    val palette = List(buf.readVarInt()) { buf.readUtf() }
                    return MapMetaPayload(hash, originX, originZ, size, sliceCount, compressedLength, palette)
                }

                override fun encode(buf: RegistryFriendlyByteBuf, value: MapMetaPayload) {
                    buf.writeVarInt(value.hash)
                    buf.writeVarInt(value.originX)
                    buf.writeVarInt(value.originZ)
                    buf.writeVarInt(value.size)
                    buf.writeVarInt(value.sliceCount)
                    buf.writeVarInt(value.compressedLength)
                    buf.writeVarInt(value.palette.size)
                    value.palette.forEach(buf::writeUtf)
                }
            }
    }
}
