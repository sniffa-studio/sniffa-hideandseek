package studio.sniffa.client

import net.fabricmc.api.ClientModInitializer
import studio.sniffa.client.border.BorderRenderer
import studio.sniffa.client.input.Keybindings
import studio.sniffa.client.map.MapOverlay
import studio.sniffa.client.prop.Props
import studio.sniffa.client.net.BorderSync
import studio.sniffa.client.net.Handshake
import studio.sniffa.client.net.RoundSync
import studio.sniffa.client.radar.CompassRose
import studio.sniffa.client.radar.HeatMap
import studio.sniffa.client.radar.HotCold
import studio.sniffa.client.radar.RangeBar
import studio.sniffa.client.ui.title.TitleScreenSkin
import studio.sniffa.client.radar.ScanPulse
import studio.sniffa.client.ui.hud.ScanReadout
import studio.sniffa.client.ui.hud.SpottedWarning
import studio.sniffa.client.ui.hud.RoundHud
import studio.sniffa.client.ui.hud.StatusHud
import studio.sniffa.client.ui.hud.ZoneWarning

object HideandseekClient : ClientModInitializer {
	override fun onInitializeClient() {
		Handshake.install()
		StatusHud.install()
		RoundHud.install()
		ScanPulse.install()
		HeatMap.install()
		HotCold.install()
		RangeBar.install()
		CompassRose.install()
		TitleScreenSkin.install()
		ScanReadout.install()
		SpottedWarning.install()
		ZoneWarning.install()
		Keybindings.install()
		BorderSync.install()
		RoundSync.install()
		BorderRenderer.install()
		MapOverlay.install()
		Props.install()
	}
}
