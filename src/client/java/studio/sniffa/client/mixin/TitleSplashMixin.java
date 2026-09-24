package studio.sniffa.client.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleSplashMixin {

    @Shadow
    private SplashRenderer splash;

    private static final int SHOW_BLUE = 0x3D9CFF;

    @Inject(method = "init", at = @At("TAIL"))
    private void hideandseek$showTheStream(CallbackInfo info) {
        this.splash = new SplashRenderer(
                Component.literal("twitch.tv/marguhl")
                        .withStyle(style -> style.withColor(SHOW_BLUE))
        );
    }
}
