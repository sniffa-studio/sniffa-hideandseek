package studio.sniffa.client

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.util.Properties

object Settings {

    private const val PING_EVENT_SERVER = "ping_event_server"

    private val file = FabricLoader.getInstance().configDir.resolve("hideandseek.properties")

    val pingEventServer: Boolean by lazy {
        val properties = Properties()
        runCatching { Files.newBufferedReader(file).use(properties::load) }

        if (!properties.containsKey(PING_EVENT_SERVER)) {
            properties.setProperty(PING_EVENT_SERVER, "true")
            runCatching {
                Files.newBufferedWriter(file).use {
                    properties.store(it, "$PING_EVENT_SERVER: show on the title screen whether the event is online (pings event.sniffa.studio)")
                }
            }
        }

        properties.getProperty(PING_EVENT_SERVER).toBoolean()
    }
}
