package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class ClockPayload(
    val state: Int,
    val seconds: Int,
    val progress: Float,
    val hidersAtStart: Int,
    val seekers: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("clock")

        val TYPE: CustomPacketPayload.Type<ClockPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, ClockPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClockPayload::state,
            ByteBufCodecs.VAR_INT, ClockPayload::seconds,
            ByteBufCodecs.FLOAT, ClockPayload::progress,
            ByteBufCodecs.VAR_INT, ClockPayload::hidersAtStart,
            ByteBufCodecs.VAR_INT, ClockPayload::seekers,
            ::ClockPayload,
        )
    }
}
