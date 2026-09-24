package studio.sniffa.client.net

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import studio.sniffa.client.state.AbilityCooldowns
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.map.MapState
import studio.sniffa.client.radar.GlowSight
import studio.sniffa.client.radar.HeatMap
import studio.sniffa.client.radar.CompassRose
import studio.sniffa.client.radar.HotCold
import studio.sniffa.client.radar.RangeBar
import studio.sniffa.client.radar.ScanPulse
import studio.sniffa.client.prop.Props
import studio.sniffa.client.radar.ScanTags
import studio.sniffa.client.ui.hud.ScanReadout
import studio.sniffa.client.ui.hud.SpottedWarning
import studio.sniffa.net.payload.CooldownPayload
import studio.sniffa.net.payload.GlowPayload
import studio.sniffa.net.payload.MapMetaPayload
import studio.sniffa.net.payload.MapSlicePayload
import studio.sniffa.net.payload.HeatPayload
import studio.sniffa.net.payload.BearingPayload
import studio.sniffa.net.payload.HotColdPayload
import studio.sniffa.net.payload.PropPayload
import studio.sniffa.net.payload.RadarPayload
import studio.sniffa.net.payload.RangePayload
import studio.sniffa.net.payload.RolePayload
import studio.sniffa.net.payload.RoundStatePayload
import studio.sniffa.net.payload.SpottedPayload
import studio.sniffa.net.payload.ScanTagPayload
import studio.sniffa.net.payload.ScanWavePayload
import studio.sniffa.net.payload.SeekerStatePayload
import studio.sniffa.net.payload.ZoneArmPayload
import studio.sniffa.net.payload.ZonePayload
import studio.sniffa.client.zone.DangerZone

object RoundSync {

    fun install() {
        ClientPlayNetworking.registerGlobalReceiver(RoundStatePayload.TYPE) { payload, _ ->
            RoundState.accept(payload.phase, payload.elapsedSeconds, payload.remainingHiders)
        }

        ClientPlayNetworking.registerGlobalReceiver(SeekerStatePayload.TYPE) { payload, _ ->
            RoundState.acceptPoints(payload.points)
        }

        ClientPlayNetworking.registerGlobalReceiver(RadarPayload.TYPE) { payload, _ ->
            ScanReadout.show(payload.count, payload.range)
        }

        ClientPlayNetworking.registerGlobalReceiver(SpottedPayload.TYPE) { payload, _ ->
            SpottedWarning.show(payload.range)
        }

        ClientPlayNetworking.registerGlobalReceiver(RolePayload.TYPE) { payload, _ ->
            RoundState.acceptRole(payload.ordinal)
        }

        ClientPlayNetworking.registerGlobalReceiver(ScanWavePayload.TYPE) { payload, _ ->
            ScanPulse.cast(payload.x, payload.y, payload.z, payload.range)
        }

        ClientPlayNetworking.registerGlobalReceiver(HeatPayload.TYPE) { payload, _ ->
            HeatMap.show(payload.x, payload.y, payload.z, payload.radius)
        }

        ClientPlayNetworking.registerGlobalReceiver(HotColdPayload.TYPE) { payload, _ ->
            HotCold.accept(payload.reading, payload.live)
        }

        ClientPlayNetworking.registerGlobalReceiver(RangePayload.TYPE) { payload, _ ->
            RangeBar.accept(payload.blocks, payload.live)
        }

        ClientPlayNetworking.registerGlobalReceiver(BearingPayload.TYPE) { payload, _ ->
            CompassRose.accept(payload.degrees, payload.live)
        }

        ClientPlayNetworking.registerGlobalReceiver(CooldownPayload.TYPE) { payload, _ ->
            AbilityCooldowns.accept(payload.secondsLeft)
        }

        ClientPlayNetworking.registerGlobalReceiver(PropPayload.TYPE) { payload, _ ->
            Props.accept(payload.kind, payload.x, payload.y, payload.z, payload.yaw, payload.seconds)
        }

        ClientPlayNetworking.registerGlobalReceiver(GlowPayload.TYPE) { payload, _ ->
            GlowSight.accept(payload.entityIds, payload.seconds)
        }

        ClientPlayNetworking.registerGlobalReceiver(MapMetaPayload.TYPE) { payload, _ ->
            MapState.onMeta(payload)
        }
        ClientPlayNetworking.registerGlobalReceiver(MapSlicePayload.TYPE) { payload, _ ->
            MapState.onSlice(payload.hash, payload.index, payload.bytes)
        }

        ClientPlayNetworking.registerGlobalReceiver(ScanTagPayload.TYPE) { payload, _ ->
            ScanTags.accept(payload.entityIds)
        }

        ClientPlayNetworking.registerGlobalReceiver(ZoneArmPayload.TYPE) { payload, _ ->
            MapState.armZone(payload.radius)
        }

        ClientPlayNetworking.registerGlobalReceiver(ZonePayload.TYPE) { payload, _ ->
            DangerZone.accept(payload.x, payload.z, payload.radius, payload.secondsLeft)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            RoundState.forget()
            ScanPulse.forget()
            HeatMap.forget()
            GlowSight.forget()
            MapState.forget()
            DangerZone.forget()
            HotCold.forget()
            RangeBar.forget()
            CompassRose.forget()
            ScanTags.forget()
            Props.forget()
            AbilityCooldowns.forget()
            ScanReadout.forget()
            SpottedWarning.forget()
        }
    }
}
