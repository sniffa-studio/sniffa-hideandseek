package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class PropPayload(
    val kind: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val seconds: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("prop")

        val TYPE: CustomPacketPayload.Type<PropPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, PropPayload> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PropPayload::kind,
            ByteBufCodecs.DOUBLE, PropPayload::x,
            ByteBufCodecs.DOUBLE, PropPayload::y,
            ByteBufCodecs.DOUBLE, PropPayload::z,
            ByteBufCodecs.FLOAT, PropPayload::yaw,
            ByteBufCodecs.VAR_INT, PropPayload::seconds,
            ::PropPayload,
        )
    }
}
