package studio.sniffa.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.ui.hud.RoleHud;

@Mixin(Hud.class)
public abstract class EffectsBelowHudMixin {

    @Inject(method = "extractEffects", at = @At("HEAD"))
    private void hideandseek$moveDown(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo info) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, (float) RoleHud.INSTANCE.effectsOffset());
    }

    @Inject(method = "extractEffects", at = @At("RETURN"))
    private void hideandseek$moveBack(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo info) {
        graphics.pose().popMatrix();
    }
}
