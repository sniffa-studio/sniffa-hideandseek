package studio.sniffa.client.mixin;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.zone.DangerZone;

@Mixin(CloudRenderer.class)
public abstract class StormCloudsMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideandseek$noCloudsInTheStorm(int colour, CloudStatus status, float height, int range, Vec3 camera,
                                               long gameTime, float partialTick, CallbackInfo info) {
        if (DangerZone.INSTANCE.current() != null) {
            info.cancel();
        }
    }
}
