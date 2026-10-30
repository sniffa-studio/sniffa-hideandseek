package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

data class IntroPayload(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val lengthTicks: Int,
    val seeker: String,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("intro")

        val TYPE: CustomPacketPayload.Type<IntroPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, IntroPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeDouble(payload.x)
                buffer.writeDouble(payload.y)
                buffer.writeDouble(payload.z)
                buffer.writeFloat(payload.yaw)
                ByteBufCodecs.VAR_INT.encode(buffer, payload.lengthTicks)
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.seeker)
            },
            { buffer ->
                IntroPayload(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                )
            },
        )
    }
}
