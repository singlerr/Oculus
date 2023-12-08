package net.coderbot.iris.mixin.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {

	@Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
	private void iris$disableVignetteRendering(GuiGraphics pGui0, Entity pEntity1, CallbackInfo ci) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null && !pipeline.shouldRenderVignette()) {
			// we need to set up the GUI render state ourselves if we cancel the vignette
			RenderSystem.enableDepthTest();
			RenderSystem.defaultBlendFunc();

			ci.cancel();
		}
	}
}
