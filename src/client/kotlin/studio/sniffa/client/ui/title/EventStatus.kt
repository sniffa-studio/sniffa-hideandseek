package studio.sniffa.client.ui.title

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.ServerStatusPinger
import net.minecraft.network.chat.Component
import net.minecraft.server.network.EventLoopGroupHolder
import studio.sniffa.client.ui.theme.Palette

object EventStatus {

    private const val GREEN = Palette.BRAND and 0xFFFFFF
    private const val RED = Palette.ERROR and 0xFFFFFF
    private const val GREY = Palette.MUTED and 0xFFFFFF

    private const val PATIENCE_MILLIS = 4_000L

    private var pinger: ServerStatusPinger? = null
    private var server: ServerData? = null
    private var startedAt = 0L
    private var gaveUp = false

    fun refresh(address: String) {
        forget()

        val client = Minecraft.getInstance()
        val data = ServerData("Sniffa Event", address, ServerData.Type.OTHER)
        val ping = ServerStatusPinger()
        server = data
        pinger = ping
        startedAt = System.currentTimeMillis()
        gaveUp = false

        try {
            ping.pingServer(
                data,
                {},
                {},
                EventLoopGroupHolder.remote(client.options.useNativeTransport()),
            )
        } catch (unreachable: Exception) {
            gaveUp = true
        }
    }

    fun tick() {
        pinger?.tick()
    }

    fun forget() {
        pinger?.removeAll()
        pinger = null
        server = null
    }

    fun line(): Component? {
        val data = server ?: return null
        val players = data.players

        if (players == null) {
            if (!gaveUp && System.currentTimeMillis() - startedAt < PATIENCE_MILLIS) {
                return null
            }
            return Component.literal("● Event offline").withStyle { it.withColor(RED) }
        }

        return Component.literal("● ")
            .withStyle { it.withColor(GREEN) }
            .append(
                Component.literal("${players.online()} / ${players.max()} online")
                    .withStyle { it.withColor(GREY) }
            )
    }
}
