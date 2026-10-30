package studio.sniffa.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.cinema.Cinema;

@Mixin(Camera.class)
public abstract class CinemaCameraMixin {

    @Shadow
    private Vec3 position;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void hideandseek$film(Level level, Entity entity, boolean detached, boolean mirror, float partialTick,
                                  CallbackInfo info) {
        double[] frame = Cinema.frame(this.position.x, this.position.y, this.position.z, this.yRot, this.xRot);
        if (frame == null) {
            return;
        }
        this.detached = true;
        this.setRotation((float) frame[3], (float) frame[4]);
        this.setPosition(new Vec3(frame[0], frame[1], frame[2]));
    }
}
