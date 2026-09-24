package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class RoundStatePayload(
    val phase: Int,
    val elapsedSeconds: Int,
    val remainingHiders: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("round")

        val TYPE: CustomPacketPayload.Type<RoundStatePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, RoundStatePayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RoundStatePayload::phase,
            ByteBufCodecs.VAR_INT, RoundStatePayload::elapsedSeconds,
            ByteBufCodecs.VAR_INT, RoundStatePayload::remainingHiders,
            ::RoundStatePayload,
        )
    }
}
