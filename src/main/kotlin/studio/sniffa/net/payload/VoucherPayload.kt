package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class VoucherPayload(val euros: Int, val finder: Int, val x: Double, val y: Double, val z: Double) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("voucher")

        val TYPE: CustomPacketPayload.Type<VoucherPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, VoucherPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoucherPayload::euros,
            ByteBufCodecs.VAR_INT, VoucherPayload::finder,
            ByteBufCodecs.DOUBLE, VoucherPayload::x,
            ByteBufCodecs.DOUBLE, VoucherPayload::y,
            ByteBufCodecs.DOUBLE, VoucherPayload::z,
            ::VoucherPayload,
        )
    }
}
