package studio.sniffa.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.cinema.Cinema;

@Mixin(Gui.class)
public abstract class CinemaHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideandseek$letterbox(GuiGraphics graphics, DeltaTracker delta, CallbackInfo info) {
        if (!Cinema.showing()) {
            return;
        }
        Cinema.drawBars(graphics);
        info.cancel();
    }
}
