package studio.sniffa.client.map

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import studio.sniffa.net.payload.MapMetaPayload
import studio.sniffa.net.payload.MapNeedPayload
import java.util.zip.Inflater

object MapState {

    private val LOGGER = LoggerFactory.getLogger("hideandseek/map")

    class ReadyMap(
        val meta: MapMetaPayload,
        val pixels: ByteArray,
    )

    private val lock = Any()

    private val known = LinkedHashMap<Int, ReadyMap>()

    @Volatile
    private var armedZoneRadius: Int? = null

    fun armZone(radius: Int) {
        armedZoneRadius = if (radius > 0) radius else null
    }

    private var assembling: MapMetaPayload? = null
    private var slices: Array<ByteArray?> = emptyArray()
    private var receivedSlices = 0

    fun onMeta(meta: MapMetaPayload) {
        val ready = synchronized(lock) {
            known[meta.hash] ?: run {
                assembling = meta
                slices = arrayOfNulls(meta.sliceCount)
                receivedSlices = 0
                null
            }
        }

        if (ready != null) {
            open(ready)
        } else {
            ClientPlayNetworking.send(MapNeedPayload(meta.hash))
        }
    }

    fun onSlice(hash: Int, index: Int, bytes: ByteArray) {
        val complete = synchronized(lock) {
            val meta = assembling ?: return
            if (meta.hash != hash || index !in slices.indices || slices[index] != null) return

            slices[index] = bytes
            receivedSlices++
            if (receivedSlices == meta.sliceCount) meta else null
        } ?: return

        val compressed = ByteArray(complete.compressedLength)
        var at = 0
        synchronized(lock) {
            for (slice in slices) {
                slice!!.copyInto(compressed, at)
                at += slice.size
            }
            assembling = null
            slices = emptyArray()
        }

        val expected = complete.size * complete.size * 2
        val pixels = ByteArray(expected)
        val inflater = Inflater()
        inflater.setInput(compressed)
        val got = inflater.inflate(pixels)
        inflater.end()

        if (got != expected) {
            LOGGER.warn("Map inflated to {} bytes, expected {} - dropped.", got, expected)
            return
        }

        val ready = ReadyMap(complete, pixels)
        synchronized(lock) {
            known[complete.hash] = ready
            while (known.size > 2) known.remove(known.keys.first())
        }
        open(ready)
    }

    fun forget() {
        armedZoneRadius = null
        synchronized(lock) {
            assembling = null
            slices = emptyArray()
            receivedSlices = 0
        }
    }

    private fun open(ready: ReadyMap) {
        val zoneRadius = armedZoneRadius
        armedZoneRadius = null

        val minecraft = Minecraft.getInstance()
        minecraft.execute {
            if (minecraft.level != null) {
                minecraft.setScreen(MapScreen(ready, zoneRadius))
            }
        }
    }
}
