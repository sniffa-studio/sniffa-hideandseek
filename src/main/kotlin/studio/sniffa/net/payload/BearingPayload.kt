package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class BearingPayload(val hasTarget: Int, val degrees: Float) : CustomPacketPayload {

    val live: Boolean get() = hasTarget != 0

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("bearing")

        val TYPE: CustomPacketPayload.Type<BearingPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, BearingPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BearingPayload::hasTarget,
            ByteBufCodecs.FLOAT, BearingPayload::degrees,
            ::BearingPayload,
        )
    }
}
