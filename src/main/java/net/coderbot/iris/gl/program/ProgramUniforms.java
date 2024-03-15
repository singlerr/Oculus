package net.coderbot.iris.gl.program;

import com.google.common.collect.ImmutableList;

import net.coderbot.iris.Iris;
import net.coderbot.iris.gl.IrisRenderSystem;
import net.coderbot.iris.gl.state.ValueUpdateNotifier;
import net.coderbot.iris.gl.uniform.*;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;


import java.nio.IntBuffer;
import java.util.*;

public class ProgramUniforms {
    private static ProgramUniforms active;
    private final ImmutableList<Uniform> perTick;
    private final ImmutableList<Uniform> perFrame;
    private final ImmutableList<Uniform> dynamic;
    private final ImmutableList<ValueUpdateNotifier> notifiersToReset;
    long lastTick = -1;
    int lastFrame = -1;
    private ImmutableList<Uniform> once;

    public ProgramUniforms(ImmutableList<Uniform> once, ImmutableList<Uniform> perTick, ImmutableList<Uniform> perFrame,
                           ImmutableList<Uniform> dynamic, ImmutableList<ValueUpdateNotifier> notifiersToReset) {
        this.once = once;
        this.perTick = perTick;
        this.perFrame = perFrame;
        this.dynamic = dynamic;
        this.notifiersToReset = notifiersToReset;
    }

    private static long getCurrentTick() {
        return Objects.requireNonNull(Minecraft.getMinecraft().world).getTotalWorldTime();
    }

    public static void clearActiveUniforms() {
        if (active != null) {
            active.removeListeners();
        }
    }

    public static Builder builder(String name, int program) {
        return new Builder(name, program);
    }

    private static String getTypeName(int type) {
        String typeName;

        if (type == GL11.GL_FLOAT) {
            typeName = "float";
        } else if (type == GL11.GL_INT) {
            typeName = "int";
        } else if (type == GL20.GL_FLOAT_MAT4) {
            typeName = "mat4";
        } else if (type == GL20.GL_FLOAT_VEC4) {
            typeName = "vec4";
        } else if (type == GL20.GL_FLOAT_MAT3) {
            typeName = "mat3";
        } else if (type == GL20.GL_FLOAT_VEC3) {
            typeName = "vec3";
        } else if (type == GL20.GL_FLOAT_MAT2) {
            typeName = "mat2";
        } else if (type == GL20.GL_FLOAT_VEC2) {
            typeName = "vec2";
        } else if (type == GL20.GL_INT_VEC2) {
            typeName = "ivec2";
        } else if (type == GL20.GL_INT_VEC4) {
            typeName = "ivec4";
        } else if (type == GL20.GL_SAMPLER_3D) {
            typeName = "sampler3D";
        } else if (type == GL20.GL_SAMPLER_2D) {
            typeName = "sampler2D";
        } else if (type == GL30.GL_UNSIGNED_INT_SAMPLER_2D) {
            typeName = "usampler2D";
        } else if (type == GL30.GL_UNSIGNED_INT_SAMPLER_3D) {
            typeName = "usampler3D";
        } else if (type == GL20.GL_SAMPLER_1D) {
            typeName = "sampler1D";
        } else if (type == GL20.GL_SAMPLER_2D_SHADOW) {
            typeName = "sampler2DShadow";
        } else if (type == GL20.GL_SAMPLER_1D_SHADOW) {
            typeName = "sampler1DShadow";
        } else if (type == ARBShaderImageLoadStore.GL_IMAGE_2D) {
            typeName = "image2D";
        } else if (type == ARBShaderImageLoadStore.GL_IMAGE_3D) {
            typeName = "image3D";
        } else {
            typeName = "(unknown:" + type + ")";
        }

        return typeName;
    }

    private static UniformType getExpectedType(int type) {
        if (type == GL11.GL_FLOAT) {
            return UniformType.FLOAT;
        } else if (type == GL11.GL_INT) {
            return UniformType.INT;
        } else if (type == GL20.GL_FLOAT_MAT4) {
            return UniformType.MAT4;
        } else if (type == GL20.GL_FLOAT_VEC4) {
            return UniformType.VEC4;
        } else if (type == GL20.GL_INT_VEC4) {
            return UniformType.VEC4I;
        } else if (type == GL20.GL_FLOAT_MAT3) {
            return null;
        } else if (type == GL20.GL_FLOAT_VEC3) {
            return UniformType.VEC3;
        } else if (type == GL20.GL_INT_VEC3) {
            return null;
        } else if (type == GL20.GL_FLOAT_MAT2) {
            return null;
        } else if (type == GL20.GL_FLOAT_VEC2) {
            return UniformType.VEC2;
        } else if (type == GL20.GL_INT_VEC2) {
            return UniformType.VEC2I;
        } else if (type == GL20.GL_SAMPLER_3D) {
            return UniformType.INT;
        } else if (type == GL20.GL_SAMPLER_2D) {
            return UniformType.INT;
        } else if (type == GL30.GL_UNSIGNED_INT_SAMPLER_2D) {
            return UniformType.INT;
        } else if (type == GL30.GL_UNSIGNED_INT_SAMPLER_3D) {
            return UniformType.INT;
        } else if (type == GL20.GL_SAMPLER_1D) {
            return UniformType.INT;
        } else if (type == GL20.GL_SAMPLER_2D_SHADOW) {
            return UniformType.INT;
        } else if (type == GL20.GL_SAMPLER_1D_SHADOW) {
            return UniformType.INT;
        } else {
            return null;
        }
    }

    private static boolean isSampler(int type) {
        return type == GL20.GL_SAMPLER_1D
                || type == GL20.GL_SAMPLER_2D
                || type == GL30.GL_UNSIGNED_INT_SAMPLER_2D
                || type == GL30.GL_UNSIGNED_INT_SAMPLER_3D
                || type == GL20.GL_SAMPLER_3D
                || type == GL20.GL_SAMPLER_1D_SHADOW
                || type == GL20.GL_SAMPLER_2D_SHADOW;
    }

    private static boolean isImage(int type) {
        return type == ARBShaderImageLoadStore.GL_IMAGE_1D
                || type == ARBShaderImageLoadStore.GL_IMAGE_2D
                || type == ARBShaderImageLoadStore.GL_UNSIGNED_INT_IMAGE_2D
                || type == ARBShaderImageLoadStore.GL_IMAGE_3D
                || type == ARBShaderImageLoadStore.GL_IMAGE_1D_ARRAY
                || type == ARBShaderImageLoadStore.GL_IMAGE_2D_ARRAY;
    }

    private void updateStage(ImmutableList<Uniform> uniforms) {
        for (Uniform uniform : uniforms) {
            uniform.update();
        }
    }

    public void update() {
        if (active != null) {
            active.removeListeners();
        }

        active = this;

        updateStage(dynamic);

        if (once != null) {
            updateStage(once);
            updateStage(perTick);
            updateStage(perFrame);
            lastTick = getCurrentTick();

            once = null;
            return;
        }

        long currentTick = getCurrentTick();

        if (lastTick != currentTick) {
            lastTick = currentTick;

            updateStage(perTick);
        }

        // TODO: Move the frame counter to a different place?
        int currentFrame = SystemTimeUniforms.COUNTER.getAsInt();

        if (lastFrame != currentFrame) {
            lastFrame = currentFrame;

            updateStage(perFrame);
        }
    }

    public void removeListeners() {
        active = null;

        for (ValueUpdateNotifier notifier : notifiersToReset) {
            notifier.setListener(null);
        }
    }

    public static class Builder implements DynamicLocationalUniformHolder {
        private final String name;
        private final int program;

        private final Map<Integer, String> locations;
        private final Map<String, Uniform> once;
        private final Map<String, Uniform> perTick;
        private final Map<String, Uniform> perFrame;
        private final Map<String, Uniform> dynamic;
        private final Map<String, UniformType> uniformNames;
        private final Map<String, UniformType> externalUniformNames;
        private final List<ValueUpdateNotifier> notifiersToReset;

        protected Builder(String name, int program) {
            this.name = name;
            this.program = program;

            locations = new HashMap<>();
            once = new HashMap<>();
            perTick = new HashMap<>();
            perFrame = new HashMap<>();
            dynamic = new HashMap<>();
            uniformNames = new HashMap<>();
            externalUniformNames = new HashMap<>();
            notifiersToReset = new ArrayList<>();
        }

        @Override
        public Builder addUniform(UniformUpdateFrequency updateFrequency, Uniform uniform) {
            Objects.requireNonNull(uniform);

            switch (updateFrequency) {
                case ONCE:
                    once.put(locations.get(uniform.getLocation()), uniform);
                    break;
                case PER_TICK:
                    perTick.put(locations.get(uniform.getLocation()), uniform);
                    break;
                case PER_FRAME:
                    perFrame.put(locations.get(uniform.getLocation()), uniform);
                    break;
            }

            return this;
        }

        @Override
        public OptionalInt location(String name, UniformType type) {
            int id = IrisRenderSystem.getUniformLocation(program, name);

            if (id == -1) {
                return OptionalInt.empty();
            }

            // TODO: Temporary hack until custom uniforms are merged.
            if ((!locations.containsKey(id) && !uniformNames.containsKey(name)) || name.equals("framemod8")) {
                locations.put(id, name);
                uniformNames.put(name, type);
            } else {
                Iris.logger.warn("[" + this.name + "] Duplicate uniform: " + type.toString().toLowerCase() + " " + name);

                return OptionalInt.empty();
            }

            return OptionalInt.of(id);
        }

        public ProgramUniforms buildUniforms() {
            // Check for any unsupported uniforms and warn about them so that we can easily figure out what uniforms we
            // need to add.
            int activeUniforms = OpenGlHelper.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
            IntBuffer sizeBuf = BufferUtils.createIntBuffer(1);
            IntBuffer typeBuf = BufferUtils.createIntBuffer(1);

            for (int index = 0; index < activeUniforms; index++) {
                String name = IrisRenderSystem.getActiveUniform(program, index, 128, sizeBuf, typeBuf);

                if (name.isEmpty()) {
                    // No further information available.
                    continue;
                }

                int size = sizeBuf.get(0);
                int type = typeBuf.get(0);

                UniformType provided = uniformNames.get(name);
                UniformType expected = getExpectedType(type);

                if (provided == null && !name.startsWith("gl_")) {
                    String typeName = getTypeName(type);

                    if (isSampler(type) || isImage(type)) {
                        // don't print a warning, samplers and images are managed elsewhere.
                        // TODO: Detect unsupported samplers/images?
                        continue;
                    }

                    UniformType externalProvided = externalUniformNames.get(name);

                    if (externalProvided != null) {
                        if (externalProvided != expected) {
                            String expectedName;

                            if (expected != null) {
                                expectedName = expected.toString();
                            } else {
                                expectedName = "(unsupported type: " + getTypeName(type) + ")";
                            }

                            Iris.logger.error("[" + this.name + "] Wrong uniform type for externally-managed uniform " + name + ": " + externalProvided + " is provided but the program expects " + expectedName + ".");
                        }

                        continue;
                    }

                    if (size == 1) {
                        Iris.logger.warn("[" + this.name + "] Unsupported uniform: " + typeName + " " + name);
                    } else {
                        Iris.logger.warn("[" + this.name + "] Unsupported uniform: " + name + " of size " + size + " and type " + typeName);
                    }

                    continue;
                }

                // TODO: This is an absolutely horrific hack, but is needed until custom uniforms work.
                if ("framemod8".equals(name) && expected == UniformType.FLOAT && provided == UniformType.INT) {
                    SystemTimeUniforms.addFloatFrameMod8Uniform(this);
                    provided = UniformType.FLOAT;
                }

                if (provided != null && provided != expected) {
                    String expectedName;

                    if (expected != null) {
                        expectedName = expected.toString();
                    } else {
                        expectedName = "(unsupported type: " + getTypeName(type) + ")";
                    }

                    Iris.logger.error("[" + this.name + "] Wrong uniform type for " + name + ": Iris is providing " + provided + " but the program expects " + expectedName + ". Disabling that uniform.");

                    once.remove(name);
                    perTick.remove(name);
                    perFrame.remove(name);
                    dynamic.remove(name);
                }
            }

            return new ProgramUniforms(ImmutableList.copyOf(once.values()), ImmutableList.copyOf(perTick.values()), ImmutableList.copyOf(perFrame.values()),
                    ImmutableList.copyOf(dynamic.values()), ImmutableList.copyOf(notifiersToReset));
        }

        @Override
        public Builder addDynamicUniform(Uniform uniform, ValueUpdateNotifier notifier) {
            Objects.requireNonNull(uniform);
            Objects.requireNonNull(notifier);

            dynamic.put(locations.get(uniform.getLocation()), uniform);
            notifiersToReset.add(notifier);

            return this;
        }

        @Override
        public UniformHolder externallyManagedUniform(String name, UniformType type) {
            externalUniformNames.put(name, type);

            return this;
        }
    }
}
