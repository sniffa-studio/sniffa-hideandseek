package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class HeatPayload(
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("heat")

        val TYPE: CustomPacketPayload.Type<HeatPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, HeatPayload> = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, HeatPayload::x,
            ByteBufCodecs.DOUBLE, HeatPayload::y,
            ByteBufCodecs.DOUBLE, HeatPayload::z,
            ByteBufCodecs.VAR_INT, HeatPayload::radius,
            ::HeatPayload,
        )
    }
}
