package com.hamtabot.obfutilities.waypoint;

import com.hamtabot.obfutilities.OBFUtilities;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public class WaypointRenderer {

    private static final int MAX_DISTANCE = 1000;

    public static void register() {
        WorldRenderEvents.LAST.register(WaypointRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        if (!OBFUtilities.config.waypointsEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Camera cam = ctx.camera();
        Vec3d camPos = cam.getPos();
        MatrixStack matrices = ctx.matrixStack();
        TextRenderer tr = client.textRenderer;

        List<Waypoint> wps = WaypointManager.getForCurrentDimension();
        if (wps.isEmpty()) return;

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        for (Waypoint wp : wps) {
            if (!wp.enabled) continue;

            double dx = (wp.x + 0.5) - camPos.x;
            double dy = (wp.y + 0.5) - camPos.y;
            double dz = (wp.z + 0.5) - camPos.z;
            double dist = Math.sqrt(dx*dx + dz*dz);
            if (dist > MAX_DISTANCE) continue;

            matrices.push();
            matrices.translate(dx, dy, dz);
            matrices.multiply(cam.getRotation());

            float scale = (float) Math.max(0.04, Math.min(0.25, 0.015 + dist * 0.0015));
            matrices.scale(-scale, -scale, scale);

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            String label = wp.name + " (" + (int)dist + "m)";
            int textColor = wp.color | 0xFF000000;
            int bgColor   = 0x60000000;
            int w = tr.getWidth(label);

            tr.draw(label, -w / 2f, -10, textColor, false, matrix,
                    immediate, TextRenderer.TextLayerType.SEE_THROUGH, bgColor, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            tr.draw("◆", -3, 0, textColor, false, matrix,
                    immediate, TextRenderer.TextLayerType.SEE_THROUGH, bgColor, LightmapTextureManager.MAX_LIGHT_COORDINATE);

            matrices.pop();
        }

        immediate.draw();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }
}