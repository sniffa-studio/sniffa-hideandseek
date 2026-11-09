package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class TracksPayload(val prints: List<Print>) : CustomPacketPayload {

    @JvmRecord
    data class Print(val x: Double, val y: Double, val z: Double, val yaw: Float, val foot: Int) {
        val left: Boolean get() = foot != 0

        companion object {
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, Print> = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Print::x,
                ByteBufCodecs.DOUBLE, Print::y,
                ByteBufCodecs.DOUBLE, Print::z,
                ByteBufCodecs.FLOAT, Print::yaw,
                ByteBufCodecs.VAR_INT, Print::foot,
                ::Print,
            )
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("tracks")

        val TYPE: CustomPacketPayload.Type<TracksPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, TracksPayload> = StreamCodec.composite(
            Print.CODEC.apply(ByteBufCodecs.list<RegistryFriendlyByteBuf, Print>()), TracksPayload::prints,
            ::TracksPayload,
        )
    }
}
