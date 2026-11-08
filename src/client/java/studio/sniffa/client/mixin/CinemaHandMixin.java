package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.cinema.Cinema;

@Mixin(GameRenderer.class)
public abstract class CinemaHandMixin {

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void hideandseek$noHandOnCamera(CameraRenderState cameraState, float partialTick, Matrix4fc modelView, CallbackInfo info) {
        if (Cinema.takesOver()) {
            info.cancel();
        }
    }
}
