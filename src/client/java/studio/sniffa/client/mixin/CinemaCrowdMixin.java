package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.sniffa.client.cinema.Cinema;

@Mixin(EntityRenderDispatcher.class)
public abstract class CinemaCrowdMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void hideandseek$clearTheStage(E entity, Frustum frustum, double x, double y, double z,
                                                            CallbackInfoReturnable<Boolean> info) {
        if (entity instanceof Player && Cinema.showing()) {
            info.setReturnValue(false);
        }
    }
}
