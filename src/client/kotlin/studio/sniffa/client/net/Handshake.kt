package studio.sniffa.client.net

import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import studio.sniffa.Hideandseek
import studio.sniffa.client.compat.ShaderGuard
import studio.sniffa.net.payload.HandshakePayload
import studio.sniffa.net.payload.ModsPayload
import studio.sniffa.protocol.Protocol

object Handshake {

    private val LOGGER = LoggerFactory.getLogger("${Hideandseek.MOD_ID}/handshake")

    @Volatile
    var state: State = State.DISCONNECTED
        private set

    enum class State {
        DISCONNECTED,

        SERVER_UNAWARE,

        REGISTERED,
    }

    fun install() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            state = State.SERVER_UNAWARE
            eventAddress = null
            sendIfPossible()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            state = State.DISCONNECTED
        }

        C2SPlayChannelEvents.REGISTER.register { _, _, _, channels ->
            if (channels.contains(HandshakePayload.ID)) sendIfPossible()
        }
    }

    @Volatile
    var eventAddress: String? = null
        private set

    private fun loadedMods(): List<ModsPayload.Entry> =
        FabricLoader.getInstance().allMods
            .filter { it.containingMod.isEmpty && it.metadata.type != "builtin" }
            .map { ModsPayload.Entry(it.metadata.id.take(MOST_CHARS), it.metadata.name.take(MOST_CHARS)) }

    private const val MOST_CHARS = 80

    private fun sendIfPossible() {
        if (!ClientPlayNetworking.canSend(HandshakePayload.TYPE)) return

        ClientPlayNetworking.send(HandshakePayload(Protocol.VERSION, Hideandseek.version))
        state = State.REGISTERED
        eventAddress = Minecraft.getInstance().currentServer?.ip ?: eventAddress
        if (ClientPlayNetworking.canSend(ModsPayload.TYPE)) {
            ClientPlayNetworking.send(ModsPayload(loadedMods()))
        }
        LOGGER.info("Announced as version {} (protocol {}).", Hideandseek.version, Protocol.VERSION)
        ShaderGuard.onEventJoined()
    }
}
