package studio.sniffa.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.sniffa.client.radar.GlowSight;
import studio.sniffa.client.radar.ScanTags;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void hideandseek$outlineSonarContacts(CallbackInfoReturnable<Boolean> answer) {
        Entity self = (Entity) (Object) this;

        GlowSight glow = GlowSight.INSTANCE;
        if (glow.active() && glow.glowing(self)) {
            answer.setReturnValue(true);
            return;
        }

        ScanTags tags = ScanTags.INSTANCE;
        if (tags.any() && tags.ready() && tags.glowing(self)) {
            answer.setReturnValue(true);
        }
    }
}
