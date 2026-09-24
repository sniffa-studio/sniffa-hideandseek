package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class ScanTagPayload(val entityIds: List<Int>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("tag")

        val TYPE: CustomPacketPayload.Type<ScanTagPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, ScanTagPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ScanTagPayload::entityIds,
            ::ScanTagPayload,
        )
    }
}
