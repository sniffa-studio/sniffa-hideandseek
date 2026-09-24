package studio.sniffa.client.net

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import studio.sniffa.client.border.BorderZone
import studio.sniffa.net.payload.BorderPayload

object BorderSync {

    fun install() {
        ClientPlayNetworking.registerGlobalReceiver(BorderPayload.TYPE) { payload, _ ->
            BorderZone.accept(
                payload.centerX,
                payload.centerZ,
                payload.radius,
                payload.targetRadius,
                payload.durationMillis,
            )
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> BorderZone.forget() }
    }
}
