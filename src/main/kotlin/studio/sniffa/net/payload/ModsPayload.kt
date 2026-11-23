package studio.sniffa.net.payload

import io.netty.buffer.ByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek

@JvmRecord
data class ModsPayload(val mods: List<Entry>) : CustomPacketPayload {

    @JvmRecord
    data class Entry(val id: String, val name: String)

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Hideandseek.id("mods")

        val TYPE: CustomPacketPayload.Type<ModsPayload> = CustomPacketPayload.Type(ID)

        private val ENTRY: StreamCodec<ByteBuf, Entry> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Entry::id,
            ByteBufCodecs.STRING_UTF8, Entry::name,
            ::Entry,
        )

        val CODEC: StreamCodec<in RegistryFriendlyByteBuf, ModsPayload> = StreamCodec.composite(
            ENTRY.apply(ByteBufCodecs.list()), ModsPayload::mods,
            ::ModsPayload,
        )
    }
}
