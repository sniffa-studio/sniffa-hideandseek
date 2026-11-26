package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import studio.sniffa.client.snow.WinterSky;

@Mixin(CloudRenderer.class)
public abstract class WinterCloudsMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int hideandseek$moonlitClouds(int colour) {
        return WinterSky.cloudColor(colour);
    }
}
