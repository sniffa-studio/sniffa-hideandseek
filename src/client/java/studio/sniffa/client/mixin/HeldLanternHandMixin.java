package studio.sniffa.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.sniffa.client.light.HeldLantern;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldLanternHandMixin {

    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack poseStack, SubmitNodeCollector collector, int light);

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hideandseek$carryLantern(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float attack, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo info) {
        if (player.isScoping() || !HeldLantern.matches(stack)) {
            return;
        }
        info.cancel();
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = arm == HumanoidArm.RIGHT;
        float side = right ? 1.0F : -1.0F;
        AvatarRenderer<AbstractClientPlayer> renderer = this.entityRenderDispatcher.getPlayerRenderer(player);
        ModelPart part = renderer.getModel().getArm(arm);
        int armLight = LightCoordsUtil.withBlock(light, Math.max(LightCoordsUtil.block(light), HeldLantern.ARM_LIGHT));
        Matrix4f base = new Matrix4f(poseStack.last().pose());

        poseStack.pushPose();
        poseStack.translate(side * HeldLantern.FIST_X, HeldLantern.FIST_Y + equip * -0.6F, HeldLantern.FIST_Z);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * HeldLantern.INWARD));
        poseStack.mulPose(Axis.XP.rotationDegrees(-HeldLantern.LEAN));
        part.resetPose();
        part.zRot = right ? 0.1F : -0.1F;
        PoseStack grip = new PoseStack();
        part.translateAndRotate(grip);
        grip.translate(side * -1.0F / 16.0F, HeldLantern.HAND / 16.0F, 0.0F);
        Vector3f fist = grip.last().pose().transformPosition(new Vector3f());
        poseStack.translate(-fist.x, -fist.y, -fist.z);
        if (!player.isInvisible()) {
            Identifier skin = player.getSkin().body().texturePath();
            if (right) {
                renderer.renderRightHand(poseStack, collector, armLight, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
            } else {
                renderer.renderLeftHand(poseStack, collector, armLight, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
            }
        }
        poseStack.popPose();

        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * HeldLantern.HANG));
        poseStack.mulPose(Axis.ZP.rotation(HeldLantern.side(partialTick)));
        poseStack.mulPose(Axis.XP.rotation(HeldLantern.tilt(partialTick)));
        poseStack.scale(HeldLantern.SIZE, HeldLantern.SIZE, HeldLantern.SIZE);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * HeldLantern.TURN));
        poseStack.translate(0.0F, -HeldLantern.TOP, 0.0F);
        this.renderItem(player, HeldLantern.hand(), ItemDisplayContext.NONE, poseStack, collector, light);
        Vector3f body = poseStack.last().pose().transformPosition(new Vector3f(0.0F, HeldLantern.BODY, 0.0F));
        poseStack.popPose();

        Vector3f at = new Matrix4f(base).invert().transformPosition(body);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        float glow = HeldLantern.haloStrength();
        collector.submitCustomGeometry(poseStack, HeldLantern.glow(), (pose, consumer) -> HeldLantern.halo(pose, consumer, glow));
        poseStack.popPose();
    }
}
