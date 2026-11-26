package studio.sniffa.client.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.ui.text.ChristmasTitle;

@Mixin(Hud.class)
public abstract class ChristmasTitleMixin {

    @Shadow
    private Component subtitle;

    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void hideandseek$christmasTitle(Component title, CallbackInfo info) {
        if (!ChristmasTitle.claims(title)) {
            return;
        }
        ChristmasTitle.show(title, subtitle);
        subtitle = null;
        info.cancel();
    }
}
