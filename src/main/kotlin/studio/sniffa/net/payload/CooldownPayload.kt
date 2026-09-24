package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class CooldownPayload(val secondsLeft: Map<String, Int>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("cooldown")

        val TYPE: CustomPacketPayload.Type<CooldownPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, CooldownPayload> = StreamCodec.composite(
            ByteBufCodecs.map(
                { size -> LinkedHashMap<String, Int>(size) },
                ByteBufCodecs.STRING_UTF8,
                ByteBufCodecs.VAR_INT,
            ),
            CooldownPayload::secondsLeft,
            ::CooldownPayload,
        )
    }
}
