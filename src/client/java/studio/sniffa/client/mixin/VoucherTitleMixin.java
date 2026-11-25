package studio.sniffa.client.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.light.VoucherPop;

@Mixin(Hud.class)
public abstract class VoucherTitleMixin {

    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void hideandseek$ownVoucherTitle(Component title, CallbackInfo info) {
        if (VoucherPop.INSTANCE.drawsOwnTitle(title.getString())) {
            info.cancel();
        }
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"), cancellable = true)
    private void hideandseek$ownVoucherSubtitle(Component subtitle, CallbackInfo info) {
        if (VoucherPop.INSTANCE.drawsOwnTitle(subtitle.getString())) {
            info.cancel();
        }
    }
}
