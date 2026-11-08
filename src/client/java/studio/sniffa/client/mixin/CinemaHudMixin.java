package studio.sniffa.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.cinema.Cinema;

@Mixin(Hud.class)
public abstract class CinemaHudMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void hideandseek$letterbox(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo info) {
        if (!Cinema.takesOver()) {
            return;
        }
        Cinema.drawBars(graphics);
        info.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hideandseek$letterboxOverTheHud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo info) {
        if (Cinema.showing() && !Cinema.takesOver()) {
            Cinema.drawBars(graphics);
        }
    }
}
