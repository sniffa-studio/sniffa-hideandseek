package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class BeamPayload(
    val id: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val argb: Int,
    val on: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("beam")

        val TYPE: CustomPacketPayload.Type<BeamPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, BeamPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BeamPayload::id,
            ByteBufCodecs.DOUBLE, BeamPayload::x,
            ByteBufCodecs.DOUBLE, BeamPayload::y,
            ByteBufCodecs.DOUBLE, BeamPayload::z,
            ByteBufCodecs.VAR_INT, BeamPayload::argb,
            ByteBufCodecs.VAR_INT, BeamPayload::on,
            ::BeamPayload,
        )
    }
}
