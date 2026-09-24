package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class ScanWavePayload(
    val x: Double,
    val y: Double,
    val z: Double,
    val range: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("scan")

        val TYPE: CustomPacketPayload.Type<ScanWavePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, ScanWavePayload> = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ScanWavePayload::x,
            ByteBufCodecs.DOUBLE, ScanWavePayload::y,
            ByteBufCodecs.DOUBLE, ScanWavePayload::z,
            ByteBufCodecs.VAR_INT, ScanWavePayload::range,
            ::ScanWavePayload,
        )
    }
}
