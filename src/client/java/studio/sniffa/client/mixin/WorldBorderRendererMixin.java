package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.border.BorderZone;

@Mixin(WorldBorderRenderer.class)
public class WorldBorderRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sniffa$hideBehindOurs(
        WorldBorderRenderState state,
        Vec3 camera,
        double farPlane,
        double nearPlane,
        CallbackInfo info
    ) {
        if (BorderZone.INSTANCE.current() != null) {
            info.cancel();
        }
    }
}
