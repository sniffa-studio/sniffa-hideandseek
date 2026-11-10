package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class CloudPayload(
    val x: Double,
    val y: Double,
    val z: Double,
    val radius: Float,
    val seconds: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("cloud")

        val TYPE: CustomPacketPayload.Type<CloudPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, CloudPayload> = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, CloudPayload::x,
            ByteBufCodecs.DOUBLE, CloudPayload::y,
            ByteBufCodecs.DOUBLE, CloudPayload::z,
            ByteBufCodecs.FLOAT, CloudPayload::radius,
            ByteBufCodecs.VAR_INT, CloudPayload::seconds,
            ::CloudPayload,
        )
    }
}
