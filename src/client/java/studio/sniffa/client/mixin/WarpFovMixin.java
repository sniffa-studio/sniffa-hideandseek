package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.sniffa.client.ui.hud.Warp;

@Mixin(GameRenderer.class)
public class WarpFovMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void sniffa$pullDuringWarp(CallbackInfoReturnable<Object> fov) {
        float scale = Warp.INSTANCE.fovScale();
        if (scale != 1f && fov.getReturnValue() instanceof Float current) {
            fov.setReturnValue(current * scale);
        }
    }
}
