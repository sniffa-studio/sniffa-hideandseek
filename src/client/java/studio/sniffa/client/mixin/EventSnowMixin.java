package studio.sniffa.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import studio.sniffa.client.border.BorderZone;

@Mixin(WeatherEffectRenderer.class)
public abstract class EventSnowMixin {

    @Unique
    private static final float SNOWFALL = 0.7F;

    @Redirect(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F")
    )
    private float hideandseek$alwaysSnowing(ClientLevel level, float partialTicks) {
        float real = level.getRainLevel(partialTicks);
        return BorderZone.INSTANCE.current() != null ? Math.max(real, SNOWFALL) : real;
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;")
    )
    private Biome.Precipitation hideandseek$snowEverywhere(ClientLevel level, BlockPos pos) {
        return BorderZone.INSTANCE.current() != null ? Biome.Precipitation.SNOW : level.getPrecipitationAt(pos);
    }
}
