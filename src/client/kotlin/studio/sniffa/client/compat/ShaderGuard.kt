package studio.sniffa.client.compat

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory
import studio.sniffa.Hideandseek

object ShaderGuard {

    private val LOGGER = LoggerFactory.getLogger("${Hideandseek.MOD_ID}/shaders")

    private const val API = "net.irisshaders.iris.api.v0.IrisApi"
    private const val CONFIG = "net.irisshaders.iris.api.v0.IrisApiConfig"

    @Volatile
    private var turnedOff = false

    fun install() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { restore() } }
        ClientLifecycleEvents.CLIENT_STOPPING.register { restore() }
    }

    fun onEventJoined() {
        if (turnedOff || !FabricLoader.getInstance().isModLoaded("iris")) return
        val client = Minecraft.getInstance()
        client.execute {
            if (turnedOff || !shadersOn()) return@execute
            if (!switchShaders(false)) return@execute
            turnedOff = true
            LOGGER.info("Turned shaders off for the event.")
            client.player?.sendSystemMessage(
                Component.literal("Shader sind während des Events aus. Danach gehen sie wieder an.")
                    .withStyle(ChatFormatting.YELLOW),
            )
        }
    }

    private fun restore() {
        if (!turnedOff) return
        turnedOff = false
        if (switchShaders(true)) LOGGER.info("Turned shaders back on.")
    }

    private fun config(): Any? = try {
        val api = Class.forName(API)
        val instance = api.getMethod("getInstance").invoke(null)
        api.getMethod("getConfig").invoke(instance)
    } catch (failure: Throwable) {
        LOGGER.warn("Iris is loaded but its API could not be reached: {}", failure.toString())
        null
    }

    private fun shadersOn(): Boolean {
        val config = config() ?: return false
        return try {
            Class.forName(CONFIG).getMethod("areShadersEnabled").invoke(config) as Boolean
        } catch (failure: Throwable) {
            LOGGER.warn("Could not ask Iris whether shaders are on: {}", failure.toString())
            false
        }
    }

    private fun switchShaders(on: Boolean): Boolean {
        val config = config() ?: return false
        return try {
            Class.forName(CONFIG).getMethod("setShadersEnabledAndApply", Boolean::class.javaPrimitiveType)
                .invoke(config, on)
            true
        } catch (failure: Throwable) {
            LOGGER.warn("Could not switch Iris shaders {}: {}", if (on) "on" else "off", failure.toString())
            false
        }
    }
}
