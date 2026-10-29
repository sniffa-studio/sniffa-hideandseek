package studio.sniffa.net.payload

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

data class CinemaPayload(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val untilFirstTicks: Int,
    val gapTicks: Int,
    val names: List<String>,
    val details: List<String>,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("cinema")

        val TYPE: CustomPacketPayload.Type<CinemaPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, CinemaPayload> = StreamCodec.of(
            { buffer, payload ->
                buffer.writeDouble(payload.x)
                buffer.writeDouble(payload.y)
                buffer.writeDouble(payload.z)
                buffer.writeFloat(payload.yaw)
                ByteBufCodecs.VAR_INT.encode(buffer, payload.untilFirstTicks)
                ByteBufCodecs.VAR_INT.encode(buffer, payload.gapTicks)
                ByteBufCodecs.VAR_INT.encode(buffer, payload.names.size)
                payload.names.forEachIndexed { index, name ->
                    ByteBufCodecs.STRING_UTF8.encode(buffer, name)
                    ByteBufCodecs.STRING_UTF8.encode(buffer, payload.details.getOrElse(index) { "" })
                }
            },
            { buffer ->
                val x = buffer.readDouble()
                val y = buffer.readDouble()
                val z = buffer.readDouble()
                val yaw = buffer.readFloat()
                val untilFirst = ByteBufCodecs.VAR_INT.decode(buffer)
                val gap = ByteBufCodecs.VAR_INT.decode(buffer)
                val count = ByteBufCodecs.VAR_INT.decode(buffer).coerceIn(0, MOST_PLACES)
                val names = ArrayList<String>(count)
                val details = ArrayList<String>(count)
                repeat(count) {
                    names += ByteBufCodecs.STRING_UTF8.decode(buffer)
                    details += ByteBufCodecs.STRING_UTF8.decode(buffer)
                }
                CinemaPayload(x, y, z, yaw, untilFirst, gap, names, details)
            },
        )

        private const val MOST_PLACES = 3
    }
}
