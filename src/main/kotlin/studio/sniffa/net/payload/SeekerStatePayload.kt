package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class SeekerStatePayload(val points: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("seeker")

        val TYPE: CustomPacketPayload.Type<SeekerStatePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, SeekerStatePayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SeekerStatePayload::points,
            ::SeekerStatePayload,
        )
    }
}
