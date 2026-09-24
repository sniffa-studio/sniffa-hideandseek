package studio.sniffa.client.net

import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import org.slf4j.LoggerFactory
import studio.sniffa.Hideandseek
import studio.sniffa.net.payload.HandshakePayload
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
            sendIfPossible()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            state = State.DISCONNECTED
        }

        C2SPlayChannelEvents.REGISTER.register { _, _, _, channels ->
            if (channels.contains(HandshakePayload.ID)) sendIfPossible()
        }
    }

    private fun sendIfPossible() {
        if (!ClientPlayNetworking.canSend(HandshakePayload.TYPE)) return

        ClientPlayNetworking.send(HandshakePayload(Protocol.VERSION, Hideandseek.version))
        state = State.REGISTERED
        LOGGER.info("Announced as version {} (protocol {}).", Hideandseek.version, Protocol.VERSION)
    }
}
