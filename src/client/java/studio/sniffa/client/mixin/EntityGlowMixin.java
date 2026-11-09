package studio.sniffa.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.sniffa.client.radar.CrewView;
import studio.sniffa.client.radar.GlowSight;
import studio.sniffa.client.radar.ScanTags;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Unique
    private static final int NOSE_RED = 0xFF3B3B;

    @Unique
    private static final int NOSE_GOLD = 0xFFD54A;

    @Unique
    private static final long FLICKER_MILLIS = 250L;

    @Unique
    private static final int FROST_BLUE = 0x8FD3FF;

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void hideandseek$noseColour(CallbackInfoReturnable<Integer> answer) {
        Entity self = (Entity) (Object) this;
        if (CrewView.INSTANCE.knows(self)) {
            return;
        }
        GlowSight glow = GlowSight.INSTANCE;
        if (glow.active() && glow.glowing(self)) {
            answer.setReturnValue(FROST_BLUE);
            return;
        }
        ScanTags tags = ScanTags.INSTANCE;
        if (!tags.any() || !tags.ready() || !tags.glowing(self)) {
            return;
        }
        boolean gold = (System.currentTimeMillis() / FLICKER_MILLIS) % 2 == 0;
        answer.setReturnValue(gold ? NOSE_GOLD : NOSE_RED);
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void hideandseek$outlineSonarContacts(CallbackInfoReturnable<Boolean> answer) {
        Entity self = (Entity) (Object) this;

        if (CrewView.INSTANCE.knows(self)) {
            answer.setReturnValue(true);
            return;
        }

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
