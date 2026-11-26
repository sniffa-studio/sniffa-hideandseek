package studio.sniffa.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.sniffa.client.snow.WinterSky;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class WinterFogMixin {

    @Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
    private void hideandseek$winterHorizon(ClientLevel level, Camera camera, int renderDistance, float partialTicks, CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(WinterSky.fogColor(camera, partialTicks, info.getReturnValueI()));
    }
}
