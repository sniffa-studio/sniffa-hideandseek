package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.light.NightFloor;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class NightFloorMixin {

    @Inject(method = "extract", at = @At("TAIL"))
    private void hideandseek$liftTheNight(LightmapRenderState renderState, float partialTicks, CallbackInfo info) {
        if (renderState.needsUpdate) {
            NightFloor.lift(renderState, partialTicks);
        }
    }
}
