package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class BorderPayload(
    val centerX: Double,
    val centerZ: Double,
    val radius: Double,
    val targetRadius: Double,
    val durationMillis: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("border")

        val TYPE: CustomPacketPayload.Type<BorderPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, BorderPayload> = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, BorderPayload::centerX,
            ByteBufCodecs.DOUBLE, BorderPayload::centerZ,
            ByteBufCodecs.DOUBLE, BorderPayload::radius,
            ByteBufCodecs.DOUBLE, BorderPayload::targetRadius,
            ByteBufCodecs.VAR_INT, BorderPayload::durationMillis,
            ::BorderPayload,
        )
    }
}
