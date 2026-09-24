package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class HotColdPayload(val hasTarget: Int, val scaled: Int) : CustomPacketPayload {

    val reading: Float get() = (scaled - OFFSET) / SCALE

    val live: Boolean get() = hasTarget != 0

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        private const val SCALE = 100f
        private const val OFFSET = 1000

        val ID: Identifier = Hideandseek.id("hotcold")

        val TYPE: CustomPacketPayload.Type<HotColdPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, HotColdPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HotColdPayload::hasTarget,
            ByteBufCodecs.VAR_INT, HotColdPayload::scaled,
            ::HotColdPayload,
        )
    }
}
