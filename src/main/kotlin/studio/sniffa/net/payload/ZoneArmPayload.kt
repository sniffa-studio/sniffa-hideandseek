package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class ZoneArmPayload(val radius: Int) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("zone_arm")

        val TYPE: CustomPacketPayload.Type<ZoneArmPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, ZoneArmPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZoneArmPayload::radius,
            ::ZoneArmPayload,
        )
    }
}
