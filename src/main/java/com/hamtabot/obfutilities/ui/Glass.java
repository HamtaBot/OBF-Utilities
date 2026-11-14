package com.hamtabot.obfutilities.ui;

import com.hamtabot.obfutilities.OBFUtilities;
import com.hamtabot.obfutilities.mixin.DrawContextInvoker;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Smooth rounded shapes drawn by a small shader: one quad per shape whatever its size, with the edge
 * anti-aliased at the real resolution of the screen. Quads share one buffer, so a whole HUD is a single draw.
 */
public final class Glass {

    private Glass() {}

    private static ShaderProgram program, blurProgram;
    private static SimpleFramebuffer blurA, blurB;
    /** True once the screen behind has been blurred for the shapes about to be drawn. */
    private static boolean backdrop;
    private static boolean blurBroken;

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(ctx -> {
            try {
                ctx.register(new Identifier(OBFUtilities.MOD_ID, "glass"),
                        VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> program = p);
            } catch (Exception e) {
                // the UI falls back to plain rectangles rather than taking the game down
                program = null;
                OBFUtilities.LOGGER.error("[OBF] glass shader unavailable: " + e.getMessage());
            }
            try {
                ctx.register(new Identifier(OBFUtilities.MOD_ID, "blur"), VertexFormats.POSITION_TEXTURE, p -> blurProgram = p);
            } catch (Exception e) {
                blurProgram = null;
                OBFUtilities.LOGGER.error("[OBF] blur shader unavailable: " + e.getMessage());
            }
        });
    }

    public static boolean ready() { return program != null; }

    public static boolean hasBackdrop() { return backdrop; }

    /**
     * Blurs what is on screen right now, for the glass panels drawn next. The copy is a quarter of the
     * resolution, so the whole thing is one blit and four small passes: a fraction of a millisecond.
     */
    public static void capture(DrawContext ctx) {
        backdrop = false;
        if (!Theme.GLASS || !OBFUtilities.config.glassBlur || blurBroken || program == null || blurProgram == null) return;
        try {
            ctx.draw();
            MinecraftClient client = MinecraftClient.getInstance();
            Framebuffer main = client.getFramebuffer();
            int w = Math.max(1, main.textureWidth / 4), h = Math.max(1, main.textureHeight / 4);
            if (blurA == null) {
                blurA = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
                blurB = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
                blurA.setTexFilter(GL11.GL_LINEAR);
                blurB.setTexFilter(GL11.GL_LINEAR);
            } else if (blurA.textureWidth != w || blurA.textureHeight != h) {
                blurA.resize(w, h, MinecraftClient.IS_SYSTEM_MAC);
                blurB.resize(w, h, MinecraftClient.IS_SYSTEM_MAC);
                blurA.setTexFilter(GL11.GL_LINEAR);
                blurB.setTexFilter(GL11.GL_LINEAR);
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, blurA.fbo);
            GlStateManager._glBlitFrameBuffer(0, 0, main.textureWidth, main.textureHeight, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            for (int i = 1; i <= 2; i++) {
                blurPass(blurA, blurB, i / (float) w, 0f);
                blurPass(blurB, blurA, 0f, i / (float) h);
            }
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            main.beginWrite(true);
            backdrop = true;
        } catch (Throwable t) {
            // never retried: the panels simply stay see-through without blur
            blurBroken = true;
            OBFUtilities.LOGGER.error("[OBF] blur disabled: " + t);
            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
        }
    }

    private static void blurPass(Framebuffer from, Framebuffer to, float dx, float dy) {
        to.beginWrite(true);
        RenderSystem.setShader(() -> blurProgram);
        RenderSystem.setShaderTexture(0, from.getColorAttachment());
        GlUniform dir = blurProgram.getUniform("Dir");
        if (dir != null) dir.set(dx, dy);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bb.vertex(-1, -1, 0).texture(0, 0).next();
        bb.vertex(1, -1, 0).texture(1, 0).next();
        bb.vertex(1, 1, 0).texture(1, 1).next();
        bb.vertex(-1, 1, 0).texture(0, 1).next();
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    private static final class Layer extends RenderLayer {
        Layer() {
            super("obf_glass", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
                    4096, false, false, () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableDepthTest();
                        RenderSystem.depthMask(false);
                        RenderSystem.disableCull();
                        if (blurA != null) RenderSystem.setShaderTexture(0, blurA.getColorAttachment());
                        RenderSystem.setShader(() -> program);
                    }, () -> {
                        RenderSystem.enableCull();
                        RenderSystem.depthMask(true);
                        RenderSystem.enableDepthTest();
                        RenderSystem.disableBlend();
                    });
        }
    }

    private static final RenderLayer LAYER = new Layer();

    /**
     * Draws the part {@code qx0..qx1, qy0..qy1} of the rounded rectangle {@code x, y, w, h}.
     * {@code stroke > 0} keeps only a ring of that thickness, {@code soft > 0} draws a shadow of that spread instead.
     * Colours are ARGB, one per corner of the drawn part: top-left, top-right, bottom-right, bottom-left.
     */
    public static void quad(DrawContext ctx, float x, float y, float w, float h, float radius, float stroke, float soft,
                            float qx0, float qy0, float qx1, float qy1, int tl, int tr, int br, int bl) {
        quad(ctx, x, y, w, h, radius, stroke, soft, 0f, qx0, qy0, qx1, qy1, tl, tr, br, bl);
    }

    private static void quad(DrawContext ctx, float x, float y, float w, float h, float radius, float stroke, float soft, float frost,
                            float qx0, float qy0, float qx1, float qy1, int tl, int tr, int br, int bl) {
        float cx = x + w / 2f, cy = y + h / 2f;
        int hu = Math.round(w * 4f), hv = Math.round(h * 4f);
        int ru = Math.round(radius * 16f), sv = Math.round(stroke * 16f);
        float n = Math.min(1f, soft / 31.75f);
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        VertexConsumer vc = ctx.getVertexConsumers().getBuffer(LAYER);
        vc.vertex(m, qx0, qy0, 0).color(tl).texture(qx0 - cx, qy0 - cy).overlay(hu, hv).light(ru, sv).normal(n, frost, 0).next();
        vc.vertex(m, qx0, qy1, 0).color(bl).texture(qx0 - cx, qy1 - cy).overlay(hu, hv).light(ru, sv).normal(n, frost, 0).next();
        vc.vertex(m, qx1, qy1, 0).color(br).texture(qx1 - cx, qy1 - cy).overlay(hu, hv).light(ru, sv).normal(n, frost, 0).next();
        vc.vertex(m, qx1, qy0, 0).color(tr).texture(qx1 - cx, qy0 - cy).overlay(hu, hv).light(ru, sv).normal(n, frost, 0).next();
        ((DrawContextInvoker) ctx).obf$tryDraw();
    }

    /** The whole shape, with a vertical gradient from {@code top} to {@code bottom}. */
    public static void shape(DrawContext ctx, float x, float y, float w, float h, float radius, float stroke, int top, int bottom) {
        if (w <= 0 || h <= 0 || ((top | bottom) >>> 24) == 0) return;
        // half a unit of margin leaves room for the anti-aliased edge
        float e = radius > 0 || stroke > 0 ? 0.5f : 0f;
        quad(ctx, x, y, w, h, radius, stroke, 0f, x - e, y - e, x + w + e, y + h + e, top, top, bottom, bottom);
    }

    /** A filled shape showing the blurred screen behind it, tinted by its colours. Needs {@link #hasBackdrop}. */
    public static void frosted(DrawContext ctx, float x, float y, float w, float h, float radius, int top, int bottom) {
        if (w <= 0 || h <= 0 || ((top | bottom) >>> 24) == 0) return;
        quad(ctx, x, y, w, h, radius, 0f, 0f, 1f, x - 0.5f, y - 0.5f, x + w + 0.5f, y + h + 0.5f, top, top, bottom, bottom);
    }

    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float radius, float spread, int color) {
        quad(ctx, x, y, w, h, radius, 0f, spread, x - spread, y - spread, x + w + spread, y + h + spread, color, color, color, color);
    }
}
