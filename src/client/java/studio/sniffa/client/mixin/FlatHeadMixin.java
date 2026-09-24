package studio.sniffa.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.ui.inventory.FlatHeads;

@Mixin(AbstractContainerScreen.class)
public abstract class FlatHeadMixin {

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void hideandseek$flattenHead(GuiGraphics graphics, Slot slot, int mouseX, int mouseY,
                                         CallbackInfo info) {
        FlatHeads.INSTANCE.overdraw(graphics, slot);
    }
}
