package studio.sniffa.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.ui.title.WinterScene;

@Mixin(TitleScreen.class)
public abstract class TitleWinterMixin {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/TitleScreen;renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void hideandseek$winter(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        Screen screen = (Screen) (Object) this;
        WinterScene.back(graphics, screen.width, screen.height);
    }
}
