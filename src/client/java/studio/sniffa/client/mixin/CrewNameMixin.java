package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.sniffa.client.radar.CrewView;

@Mixin(LivingEntityRenderer.class)
public abstract class CrewNameMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void hideandseek$namesForTheCrew(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> answer) {
        if (CrewView.INSTANCE.knows(entity)) {
            answer.setReturnValue(true);
        }
    }
}
