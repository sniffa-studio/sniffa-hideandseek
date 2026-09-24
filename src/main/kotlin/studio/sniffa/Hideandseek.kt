package studio.sniffa

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import studio.sniffa.net.payload.BearingPayload
import studio.sniffa.net.payload.BorderPayload
import studio.sniffa.net.payload.CooldownPayload
import studio.sniffa.net.payload.GlowPayload
import studio.sniffa.net.payload.HandshakePayload
import studio.sniffa.net.payload.MapMetaPayload
import studio.sniffa.net.payload.MapNeedPayload
import studio.sniffa.net.payload.MapSlicePayload
import studio.sniffa.net.payload.MapTeleportPayload
import studio.sniffa.net.payload.HeatPayload
import studio.sniffa.net.payload.HotColdPayload
import studio.sniffa.net.payload.PropPayload
import studio.sniffa.net.payload.PurchaseRequestPayload
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
import studio.sniffa.net.payload.ZonePickPayload
import studio.sniffa.protocol.Protocol
import studio.sniffa.sound.Cues

object Hideandseek : ModInitializer {
	const val MOD_ID: String = "hideandseek"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	val version: String by lazy {
		FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map { it.metadata.version.friendlyString }
			.orElse("unknown")
	}

	override fun onInitialize() {
		PayloadTypeRegistry.playC2S().register(HandshakePayload.TYPE, HandshakePayload.CODEC)
		PayloadTypeRegistry.playC2S().register(PurchaseRequestPayload.TYPE, PurchaseRequestPayload.CODEC)
		PayloadTypeRegistry.playC2S().register(MapNeedPayload.TYPE, MapNeedPayload.CODEC)
		PayloadTypeRegistry.playC2S().register(MapTeleportPayload.TYPE, MapTeleportPayload.CODEC)
		PayloadTypeRegistry.playC2S().register(ZonePickPayload.TYPE, ZonePickPayload.CODEC)

		PayloadTypeRegistry.playS2C().register(BorderPayload.TYPE, BorderPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(RoundStatePayload.TYPE, RoundStatePayload.CODEC)
		PayloadTypeRegistry.playS2C().register(SeekerStatePayload.TYPE, SeekerStatePayload.CODEC)
		PayloadTypeRegistry.playS2C().register(RadarPayload.TYPE, RadarPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(ScanTagPayload.TYPE, ScanTagPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(ScanWavePayload.TYPE, ScanWavePayload.CODEC)
		PayloadTypeRegistry.playS2C().register(HeatPayload.TYPE, HeatPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(HotColdPayload.TYPE, HotColdPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(RangePayload.TYPE, RangePayload.CODEC)
		PayloadTypeRegistry.playS2C().register(BearingPayload.TYPE, BearingPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(RolePayload.TYPE, RolePayload.CODEC)
		PayloadTypeRegistry.playS2C().register(SpottedPayload.TYPE, SpottedPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(GlowPayload.TYPE, GlowPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(PropPayload.TYPE, PropPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(CooldownPayload.TYPE, CooldownPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(MapMetaPayload.TYPE, MapMetaPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(MapSlicePayload.TYPE, MapSlicePayload.CODEC)
		PayloadTypeRegistry.playS2C().register(ZoneArmPayload.TYPE, ZoneArmPayload.CODEC)
		PayloadTypeRegistry.playS2C().register(ZonePayload.TYPE, ZonePayload.CODEC)

		Cues.install()

		LOGGER.info(
			"Hide & Seek {} loaded, protocol {}, channel {} registered.",
			version, Protocol.VERSION, HandshakePayload.ID,
		)
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
