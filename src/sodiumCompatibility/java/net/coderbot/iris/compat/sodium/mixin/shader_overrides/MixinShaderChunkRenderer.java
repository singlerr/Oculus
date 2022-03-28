package net.coderbot.iris.compat.sodium.mixin.shader_overrides;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.sodium.render.chunk.draw.ShaderChunkRenderer;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.coderbot.iris.Iris;
import net.coderbot.iris.compat.sodium.impl.shader_overrides.GlObjectExt;
import net.coderbot.iris.compat.sodium.impl.shader_overrides.ShaderChunkRendererExt;
import net.coderbot.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides;
import net.coderbot.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Overrides shaders in {@link ShaderChunkRenderer} with our own as needed.
 */
@Mixin(ShaderChunkRenderer.class)
public abstract class MixinShaderChunkRenderer implements ShaderChunkRendererExt {
    @Unique
    private IrisChunkProgramOverrides irisChunkProgramOverrides;

    @Shadow(remap = false)
	@Final
	protected TerrainVertexType vertexType;

	@Shadow
	@Final
	protected RenderDevice device;

	@Shadow
	protected abstract Program<ChunkShaderInterface> getProgram(ChunkRenderPass pass);

	@Shadow
	private static ShaderConstants getShaderConstants(ChunkRenderPass pass, TerrainVertexType vertexType) {
		return null;
	}

	@Shadow
	public abstract void delete();

	@Shadow
	protected abstract Pipeline<ChunkShaderInterface, ShaderChunkRenderer.BufferTarget> createPipeline(ChunkRenderPass pass);

	@Shadow
	@Final
	private Map<ChunkRenderPass, Program<ChunkShaderInterface>> programs;

	@Shadow
	@Final
	private Map<ChunkRenderPass, Pipeline<ChunkShaderInterface, ShaderChunkRenderer.BufferTarget>> pipelines;

	@Unique
	private final Map<ChunkRenderPass, Pipeline<ChunkShaderInterface, ShaderChunkRenderer.BufferTarget>> shadowPipelines = new Object2ObjectOpenHashMap();

	@Unique
	private final Map<ChunkRenderPass, Program<ChunkShaderInterface>> shadowPrograms = new Object2ObjectOpenHashMap();

	@Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iris$onInit(RenderDevice device, TerrainVertexType vertexType, CallbackInfo ci) {
        irisChunkProgramOverrides = new IrisChunkProgramOverrides();
    }

	@Redirect(method = "getProgram", at = @At(value = "FIELD", target = "Lnet/caffeinemc/sodium/render/chunk/draw/ShaderChunkRenderer;programs:Ljava/util/Map;"))
	private Map<ChunkRenderPass, Program<ChunkShaderInterface>> redirectShadowProgram(ShaderChunkRenderer instance) {
		return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? shadowPrograms : programs;
	}

	@Redirect(method = "getPipeline", at = @At(value = "FIELD", target = "Lnet/caffeinemc/sodium/render/chunk/draw/ShaderChunkRenderer;pipelines:Ljava/util/Map;"))
	private Map<ChunkRenderPass, Pipeline<ChunkShaderInterface, ShaderChunkRenderer.BufferTarget>> redirectShadowPipeline(ShaderChunkRenderer instance) {
		return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? shadowPipelines : pipelines;
	}

	@Inject(method = "getPipeline", at = @At("HEAD"), cancellable = true)
	protected void getPipeline(ChunkRenderPass pass, CallbackInfoReturnable<Pipeline<ChunkShaderInterface, ShaderChunkRenderer.BufferTarget>> cir) {

	}

	/**
	 * @author
	 */
	@Overwrite(remap = false)
	private Program<ChunkShaderInterface> createProgram(ChunkRenderPass pass) {
		if (IrisApi.getInstance().isShaderPackInUse()) {
			return irisChunkProgramOverrides.getProgramOverride(device, pass, vertexType);
		} else {
			ShaderConstants constants = getShaderConstants(pass, this.vertexType);
			String vertShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new ResourceLocation("sodium", "blocks/block_layer_opaque.vsh"), constants);
			String fragShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new ResourceLocation("sodium", "blocks/block_layer_opaque.fsh"), constants);
			ShaderDescription desc = ShaderDescription.builder().addShaderSource(ShaderType.VERTEX, vertShader).addShaderSource(ShaderType.FRAGMENT, fragShader).addAttributeBinding("a_Position", 1).addAttributeBinding("a_Color", 2).addAttributeBinding("a_TexCoord", 3).addAttributeBinding("a_LightCoord", 4).addFragmentBinding("fragColor", 0).build();
			return this.device.createProgram(desc, ChunkShaderInterface::new);
		}
	}

	@Redirect(method = "delete", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/gfx/api/device/RenderDevice;deleteProgram(Lnet/caffeinemc/gfx/api/shader/Program;)V"))
	private void checkHandleFirst(RenderDevice instance, Program program) {
		if (((GlObjectExt) program).isHandleValid()) {
			instance.deleteProgram(program);
		}
	}

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    private void iris$onDelete(CallbackInfo ci) {
		deletePrograms();
	}

	private void deletePrograms() {
		for (var pipeline : this.shadowPipelines.values()) {
			this.device.deletePipeline(pipeline);
		}

		this.shadowPipelines.clear();

		for (var program : this.shadowPrograms.values()) {
			if (((GlObjectExt) program).isHandleValid()) {
				this.device.deleteProgram(program);
			}
		}

		this.shadowPrograms.clear();

		irisChunkProgramOverrides.deleteShaders(this.device);
	}

	@Override
	public IrisChunkProgramOverrides iris$getOverrides() {
		return irisChunkProgramOverrides;
	}
}
