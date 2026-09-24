package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class RangePayload(val hasTarget: Int, val blocks: Int) : CustomPacketPayload {

    val live: Boolean get() = hasTarget != 0

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("range")

        val TYPE: CustomPacketPayload.Type<RangePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, RangePayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RangePayload::hasTarget,
            ByteBufCodecs.VAR_INT, RangePayload::blocks,
            ::RangePayload,
        )
    }
}
