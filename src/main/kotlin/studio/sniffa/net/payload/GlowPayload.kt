package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class GlowPayload(val seconds: Int, val entityIds: List<Int>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("glow")

        val TYPE: CustomPacketPayload.Type<GlowPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, GlowPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GlowPayload::seconds,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), GlowPayload::entityIds,
            ::GlowPayload,
        )
    }
}
