package studio.sniffa.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.ui.title.WinterScene;

@Mixin(TitleScreen.class)
public abstract class TitleWinterMixin {

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void hideandseek$winter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
        Screen screen = (Screen) (Object) this;
        WinterScene.back(graphics, screen.width, screen.height);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"
            )
    )
    private void hideandseek$ourLogo(LogoRenderer logo, GuiGraphicsExtractor graphics, int width, float alpha) {
        Screen screen = (Screen) (Object) this;
        WinterScene.logo(graphics, width, screen.height, alpha);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"
            )
    )
    private void hideandseek$splashBesideLogo(SplashRenderer splash, GuiGraphicsExtractor graphics, int width, Font font, float alpha) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(WinterScene.SPLASH_SHIFT_X, WinterScene.SPLASH_SHIFT_Y);
        splash.extractRenderState(graphics, width, font, alpha);
        graphics.pose().popMatrix();
    }
}
