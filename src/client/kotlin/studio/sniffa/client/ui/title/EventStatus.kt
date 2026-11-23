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

    private const val REFRESH_MILLIS = 20_000L

    @Volatile
    private var pinger: ServerStatusPinger? = null
    private var server: ServerData? = null
    private var startedAt = 0L
    @Volatile
    private var gaveUp = false
    private var address: String? = null
    private var lastLine: Component? = null

    fun refresh(address: String) {
        forget()
        this.address = address

        val client = Minecraft.getInstance()
        val data = ServerData("Sniffa Event", address, ServerData.Type.OTHER)
        val ping = ServerStatusPinger()
        server = data
        pinger = ping
        startedAt = System.currentTimeMillis()
        gaveUp = false

        val transport = EventLoopGroupHolder.remote(client.options.useNativeTransport())
        val worker = Thread({
            try {
                ping.pingServer(data, {}, {}, transport)
            } catch (unreachable: Exception) {
                gaveUp = true
            }
            if (pinger !== ping) {
                ping.removeAll()
            }
        }, "sniffa event ping")
        worker.contextClassLoader = EventStatus::class.java.classLoader
        worker.isDaemon = true
        worker.start()
    }

    fun tick() {
        pinger?.tick()
        val again = address ?: return
        if (System.currentTimeMillis() - startedAt >= REFRESH_MILLIS) {
            val keep = lastLine
            refresh(again)
            lastLine = keep
        }
    }

    fun forget() {
        pinger?.removeAll()
        pinger = null
        server = null
        lastLine = null
    }

    fun stop() {
        forget()
        address = null
    }

    fun line(): Component? {
        val fresh = freshLine()
        if (fresh != null) lastLine = fresh
        return fresh ?: lastLine
    }

    private fun freshLine(): Component? {
        val data = server ?: return null
        val players = data.players

        if (players == null) {
            if (!gaveUp && System.currentTimeMillis() - startedAt < PATIENCE_MILLIS) {
                return null
            }
            return Component.literal("● Event offline").withStyle { it.withColor(RED) }
        }

        val status = statusOf(data.motd.string)
        return Component.literal("● ")
            .withStyle { it.withColor(dotFor(status)) }
            .append(
                Component.literal(status ?: "${players.online()} / ${players.max()} online")
                    .withStyle { it.withColor(GREY) }
            )
    }

    fun statusOf(motd: String): String? =
        motd.lines().getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

    private fun dotFor(status: String?): Int = when {
        status == null || status.startsWith("Offen") -> GREEN
        status.startsWith("Voll") -> RED
        else -> GREY
    }
}
