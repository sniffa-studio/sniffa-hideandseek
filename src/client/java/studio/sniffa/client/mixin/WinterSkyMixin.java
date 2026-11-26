package studio.sniffa.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.snow.WinterSky;

@Mixin(SkyRenderer.class)
public abstract class WinterSkyMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hideandseek$winterNight(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo info) {
        state.skyColor = WinterSky.extract(camera, partialTicks, state.skybox == DimensionType.Skybox.OVERWORLD, state.skyColor);
    }

    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void hideandseek$winterStars(float starBrightness, PoseStack poseStack, CallbackInfo info) {
        if (WinterSky.getStrength() > 0.0F) {
            WinterSky.drawStars(poseStack, starBrightness);
            info.cancel();
        }
    }

    @Inject(method = "renderMoon", at = @At("TAIL"))
    private void hideandseek$moonHalo(MoonPhase moonPhase, float rainBrightness, PoseStack poseStack, CallbackInfo info) {
        WinterSky.drawMoonHalo(poseStack, rainBrightness);
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void hideandseek$horizonHaze(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase,
                                         float rainBrightness, float starBrightness, CallbackInfo info) {
        WinterSky.drawHaze();
    }
}
