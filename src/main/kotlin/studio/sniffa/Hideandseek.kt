package studio.sniffa

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import studio.sniffa.net.payload.BearingPayload
import studio.sniffa.net.payload.TracksPayload
import studio.sniffa.net.payload.BorderPayload
import studio.sniffa.net.payload.ClockPayload
import studio.sniffa.net.payload.CooldownPayload
import studio.sniffa.net.payload.PricesPayload
import studio.sniffa.net.payload.CrewPayload
import studio.sniffa.net.payload.GlowPayload
import studio.sniffa.net.payload.HandshakePayload
import studio.sniffa.net.payload.ModsPayload
import studio.sniffa.net.payload.MapMetaPayload
import studio.sniffa.net.payload.MapNeedPayload
import studio.sniffa.net.payload.MapSlicePayload
import studio.sniffa.net.payload.MapTeleportPayload
import studio.sniffa.net.payload.HeatPayload
import studio.sniffa.net.payload.HotColdPayload
import studio.sniffa.net.payload.PropPayload
import studio.sniffa.net.payload.DisguisePayload
import studio.sniffa.net.payload.SplatPayload
import studio.sniffa.net.payload.CloudPayload
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
import studio.sniffa.net.payload.CaughtPayload
import studio.sniffa.net.payload.ZonePayload
import studio.sniffa.net.payload.CinemaPayload
import studio.sniffa.net.payload.IntroPayload
import studio.sniffa.net.payload.MomentPayload
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
		PayloadTypeRegistry.serverboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(ModsPayload.TYPE, ModsPayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(PurchaseRequestPayload.TYPE, PurchaseRequestPayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(MapNeedPayload.TYPE, MapNeedPayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(MapTeleportPayload.TYPE, MapTeleportPayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(ZonePickPayload.TYPE, ZonePickPayload.CODEC)

		PayloadTypeRegistry.clientboundPlay().register(BorderPayload.TYPE, BorderPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(RoundStatePayload.TYPE, RoundStatePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(ClockPayload.TYPE, ClockPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(SeekerStatePayload.TYPE, SeekerStatePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(RadarPayload.TYPE, RadarPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(ScanTagPayload.TYPE, ScanTagPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(ScanWavePayload.TYPE, ScanWavePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(HeatPayload.TYPE, HeatPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(HotColdPayload.TYPE, HotColdPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(RangePayload.TYPE, RangePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(BearingPayload.TYPE, BearingPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(TracksPayload.TYPE, TracksPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(RolePayload.TYPE, RolePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(SpottedPayload.TYPE, SpottedPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(GlowPayload.TYPE, GlowPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(CrewPayload.TYPE, CrewPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(PropPayload.TYPE, PropPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(DisguisePayload.TYPE, DisguisePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(SplatPayload.TYPE, SplatPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(CloudPayload.TYPE, CloudPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(CooldownPayload.TYPE, CooldownPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(PricesPayload.TYPE, PricesPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(MapMetaPayload.TYPE, MapMetaPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(MapSlicePayload.TYPE, MapSlicePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(ZoneArmPayload.TYPE, ZoneArmPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(CaughtPayload.TYPE, CaughtPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(ZonePayload.TYPE, ZonePayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(CinemaPayload.TYPE, CinemaPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(IntroPayload.TYPE, IntroPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(MomentPayload.TYPE, MomentPayload.CODEC)

		Cues.install()

		LOGGER.info(
			"Hide & Seek {} loaded, protocol {}, channel {} registered.",
			version, Protocol.VERSION, HandshakePayload.ID,
		)
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
