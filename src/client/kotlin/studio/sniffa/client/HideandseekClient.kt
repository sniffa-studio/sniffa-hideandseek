package studio.sniffa.client

import net.fabricmc.api.ClientModInitializer
import studio.sniffa.client.border.BorderRenderer
import studio.sniffa.client.radar.Footprints
import studio.sniffa.client.cinema.Cinema
import studio.sniffa.client.compat.ShaderGuard
import studio.sniffa.client.input.Keybindings
import studio.sniffa.client.map.MapOverlay
import studio.sniffa.client.net.BorderSync
import studio.sniffa.client.net.Handshake
import studio.sniffa.client.net.RoundSync
import studio.sniffa.client.light.Spotlights
import studio.sniffa.client.prop.Props
import studio.sniffa.client.radar.CompassRose
import studio.sniffa.client.radar.HeatMap
import studio.sniffa.client.radar.HotCold
import studio.sniffa.client.radar.RangeBar
import studio.sniffa.client.radar.ScanPulse
import studio.sniffa.client.ui.hud.CaughtScreen
import studio.sniffa.client.ui.hud.RoleHud
import studio.sniffa.client.ui.hud.ScanReadout
import studio.sniffa.client.ui.hud.SpottedWarning
import studio.sniffa.client.ui.hud.Warp
import studio.sniffa.client.ui.hud.ZoneWarning
import studio.sniffa.client.zone.ZoneStorm
import studio.sniffa.client.snow.SnowClouds
import studio.sniffa.client.snow.SnowSplat
import studio.sniffa.client.ui.title.BackToEvent
import studio.sniffa.client.ui.title.TitleScreenSkin

object HideandseekClient : ClientModInitializer {
	override fun onInitializeClient() {
		Handshake.install()
		ShaderGuard.install()
		RoleHud.install()
		ScanPulse.install()
		HeatMap.install()
		HotCold.install()
		RangeBar.install()
		CompassRose.install()
		TitleScreenSkin.install()
		BackToEvent.install()
		ScanReadout.install()
		SpottedWarning.install()
		ZoneWarning.install()
		ZoneStorm.install()
		SnowSplat.install()
		SnowClouds.install()
		Warp.install()
		CaughtScreen.install()
		Keybindings.install()
		BorderSync.install()
		RoundSync.install()
		BorderRenderer.install()
		Footprints.install()
		MapOverlay.install()
		Props.install()
		Spotlights.install()
		Cinema.install()
	}
}
