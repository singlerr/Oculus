package net.coderbot.iris.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin makes the effects of view bobbing and nausea apply to the model view matrix, not the projection matrix.
 * <p>
 * Applying these effects to the projection matrix causes severe issues with most shaderpacks. As it turns out, OptiFine
 * applies these effects to the modelview matrix. As such, we must do the same to properly run shaderpacks.
 * <p>
 * This mixin makes use of the matrix stack in order to make these changes without more invasive changes.
 */
@Mixin(GameRenderer.class)
public class MixinModelViewBobbing {
    @Unique
    private Matrix4f bobbingEffectsModel;

    @Unique
    private boolean areShadersOn;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void iris$saveShadersOn(float pGameRenderer0, long pLong1, PoseStack pPoseStack2, CallbackInfo ci) {
        areShadersOn = IrisApi.getInstance().isShaderPackInUse();
    }

    @ModifyArg(method = "renderLevel", index = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private PoseStack iris$separateViewBobbing(PoseStack stack) {
        if (!areShadersOn) return stack;

        stack.pushPose();
        stack.last().pose().setIdentity();

        return stack;
    }

    @Redirect(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;last()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"),
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V")))
    private PoseStack.Pose iris$saveBobbing(PoseStack stack) {
        if (!areShadersOn) return stack.last();

        bobbingEffectsModel = stack.last().pose().copy();

        stack.popPose();

        return stack.last();
    }

    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lcom/mojang/math/Matrix4f;)V"))
    private void iris$applyBobbingToModelView(float tickDelta, long limitTime, PoseStack matrix, CallbackInfo ci) {
        if (!areShadersOn) return;

        matrix.last().pose().multiply(bobbingEffectsModel);

        bobbingEffectsModel = null;
    }
}
