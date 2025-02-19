package net.coderbot.iris.gl.texture;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;


public enum DepthBufferFormat {
    DEPTH(false),
    DEPTH16(false),
    DEPTH24(false),
    DEPTH32(false),
    DEPTH32F(false),
    DEPTH_STENCIL(true),
    DEPTH24_STENCIL8(true),
    DEPTH32F_STENCIL8(true);

    private final boolean combinedStencil;

    DepthBufferFormat(boolean combinedStencil) {
        this.combinedStencil = combinedStencil;
    }

    @Nullable
    public static DepthBufferFormat fromGlEnum(int glenum) {
        switch (glenum) {
            case GL11.GL_DEPTH_COMPONENT:
                return DepthBufferFormat.DEPTH;
            case GL14.GL_DEPTH_COMPONENT16:
                return DepthBufferFormat.DEPTH16;
            case GL14.GL_DEPTH_COMPONENT24:
                return DepthBufferFormat.DEPTH24;
            case GL14.GL_DEPTH_COMPONENT32:
                return DepthBufferFormat.DEPTH32;
            case GL30.GL_DEPTH_COMPONENT32F:
                return DepthBufferFormat.DEPTH32F;
            case GL30.GL_DEPTH_STENCIL:
                return DepthBufferFormat.DEPTH_STENCIL;
            case GL30.GL_DEPTH24_STENCIL8:
                return DepthBufferFormat.DEPTH24_STENCIL8;
            case GL30.GL_DEPTH32F_STENCIL8:
                return DepthBufferFormat.DEPTH32F_STENCIL8;
            default:
                return null;
        }
    }

    public static DepthBufferFormat fromGlEnumOrDefault(int glenum) {
        DepthBufferFormat format = fromGlEnum(glenum);
        if (format == null) {
            // yolo, just assume it's GL_DEPTH_COMPONENT
            return DepthBufferFormat.DEPTH;
        }
        return format;
    }

    public int getGlInternalFormat() {
        switch (this) {
            case DEPTH:
                return GL11.GL_DEPTH_COMPONENT;
            case DEPTH16:
                return GL14.GL_DEPTH_COMPONENT16;
            case DEPTH24:
                return GL14.GL_DEPTH_COMPONENT24;
            case DEPTH32:
                return GL14.GL_DEPTH_COMPONENT32;
            case DEPTH32F:
                return GL30.GL_DEPTH_COMPONENT32F;
            case DEPTH_STENCIL:
                return GL30.GL_DEPTH_STENCIL;
            case DEPTH24_STENCIL8:
                return GL30.GL_DEPTH24_STENCIL8;
            case DEPTH32F_STENCIL8:
                return GL30.GL_DEPTH32F_STENCIL8;
        }

        throw new AssertionError("unreachable");
    }

    public int getGlType() {
        return isCombinedStencil() ? GL30.GL_DEPTH_STENCIL : GL11.GL_DEPTH_COMPONENT;
    }

    public int getGlFormat() {
        switch (this) {
            case DEPTH:
            case DEPTH16:
                return GL11.GL_UNSIGNED_SHORT;
            case DEPTH24:
            case DEPTH32:
                return GL11.GL_UNSIGNED_INT;
            case DEPTH32F:
                return GL11.GL_FLOAT;
            case DEPTH_STENCIL:
            case DEPTH24_STENCIL8:
                return GL30.GL_UNSIGNED_INT_24_8;
            case DEPTH32F_STENCIL8:
                return GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
        }

        throw new AssertionError("unreachable");
    }

    public boolean isCombinedStencil() {
        return combinedStencil;
    }
}
