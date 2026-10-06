package studio.sniffa.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.sniffa.client.radar.CrewView;

@Mixin(Entity.class)
public abstract class CrewColourMixin {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideandseek$colourForTheCrew(CallbackInfoReturnable<Integer> answer) {
        Integer colour = CrewView.INSTANCE.colourOf((Entity) (Object) this);
        if (colour != null) {
            answer.setReturnValue(colour);
        }
    }
}
