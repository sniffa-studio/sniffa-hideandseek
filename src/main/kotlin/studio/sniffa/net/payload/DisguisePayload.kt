package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class DisguisePayload(
    val entityId: Int,
    val kind: String,
    val seconds: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("disguise")

        val TYPE: CustomPacketPayload.Type<DisguisePayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, DisguisePayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DisguisePayload::entityId,
            ByteBufCodecs.STRING_UTF8, DisguisePayload::kind,
            ByteBufCodecs.VAR_INT, DisguisePayload::seconds,
            ::DisguisePayload,
        )
    }
}
