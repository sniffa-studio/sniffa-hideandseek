package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class PricesPayload(val prices: Map<String, Price>) : CustomPacketPayload {

    @JvmRecord
    data class Price(val cost: Int, val restockCost: Int, val cooldownSeconds: Int) {
        companion object {
            val CODEC: StreamCodec<in RegistryFriendlyByteBuf, Price> = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Price::cost,
                ByteBufCodecs.VAR_INT, Price::restockCost,
                ByteBufCodecs.VAR_INT, Price::cooldownSeconds,
                ::Price,
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("prices")

        val TYPE: CustomPacketPayload.Type<PricesPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, PricesPayload> = StreamCodec.composite(
            ByteBufCodecs.map(
                { size -> LinkedHashMap<String, Price>(size) },
                ByteBufCodecs.STRING_UTF8,
                Price.CODEC,
            ),
            PricesPayload::prices,
            ::PricesPayload,
        )
    }
}
