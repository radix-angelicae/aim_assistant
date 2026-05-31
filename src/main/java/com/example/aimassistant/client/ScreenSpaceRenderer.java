package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ScreenSpaceRenderer {

    private static final List<LabelData> pendingLabels = new ArrayList<>();

    private static class LabelData {
        Vec3 proj;
        double dist;
        LivingEntity entity;
        Matrix4f mvp;
        int screenWidth, screenHeight;

        LabelData(Vec3 proj, double dist, LivingEntity entity, Matrix4f mvp, int sw, int sh) {
            this.proj = proj;
            this.dist = dist;
            this.entity = entity;
            this.mvp = mvp;
            this.screenWidth = sw;
            this.screenHeight = sh;
        }
    }

    public static void renderAll(Minecraft mc, Camera camera, PoseStack poseStack,
                                 List<LivingEntity> entities, LivingEntity lockedTarget) {
        pendingLabels.clear();
        if (entities.isEmpty() && lockedTarget == null) return;

        RenderSystem.assertOnRenderThread();

        Vec3 camPos = camera.getPosition();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.mulPoseMatrix(poseStack.last().pose());
        modelViewStack.translate(-camPos.x, -camPos.y, -camPos.z);

        Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        Matrix4f mvpMatrix = new Matrix4f(projMatrix);
        mvpMatrix.mul(modelViewStack.last().pose());

        RenderSystem.backupProjectionMatrix();
        Matrix4f ortho = new Matrix4f().setOrtho(0, screenWidth, screenHeight, 0, -1000, 1000);
        RenderSystem.setProjectionMatrix(ortho, VertexSorting.DISTANCE_TO_ORIGIN);

        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float normalLineWidth = (float) AimConfig.getLineWidth();

        // 第一批次：普通实体和普通锁定框
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        GL11.glLineWidth(normalLineWidth);

        for (LivingEntity e : entities) {
            float[] lineColor = RenderHandler.getColorFor(e, true, AimConfig.getFilterMode());
            float[] boxColor  = RenderHandler.getColorFor(e, false, AimConfig.getFilterMode());

            if (AimConfig.isEnableLineRendering()) {
                drawLineToScreenCenter(mvpMatrix, screenWidth, screenHeight, e.position(), centerX, centerY, lineColor, buffer);
            }
            if (AimConfig.isEnableBoxRendering()) {
                drawBoxScreen(mvpMatrix, screenWidth, screenHeight, e, boxColor, buffer);
            }
        }

        // 普通锁定框（线宽 + 2）
        if (lockedTarget != null && AimConfig.isGlowTarget() && lockedTarget.isAlive() &&
            AimModeAdapter.isValidForFilter(lockedTarget) && !AimModeAdapter.isExcluded(lockedTarget)) {
            GL11.glLineWidth(normalLineWidth + 2.0f);
            drawBoxScreen(mvpMatrix, screenWidth, screenHeight, lockedTarget, RenderHandler.getGlowColor(), buffer);
            GL11.glLineWidth(normalLineWidth);
        }

        tesselator.end();

        // 第二批次：强锁框
        if (lockedTarget != null && AimHandler.forceLocked && lockedTarget == AimHandler.forceLockedEntity) {
            if (AimConfig.isGlowTarget() && lockedTarget.isAlive() &&
                AimModeAdapter.isValidForFilter(lockedTarget) && !AimModeAdapter.isExcluded(lockedTarget)) {
                buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                GL11.glLineWidth((float) AimConfig.getForceLockLineWidth());
                drawBoxScreen(mvpMatrix, screenWidth, screenHeight, lockedTarget, RenderHandler.getForceLockGlowColor(), buffer);
                tesselator.end();
                GL11.glLineWidth(normalLineWidth);
            }
        }

        // 构建待绘制的标签列表（延迟到 GUI 绘制）
        if (AimConfig.isEnableTextRendering() && !entities.isEmpty()) {
            pendingLabels.clear();
            for (LivingEntity e : entities) {
                if (AimConfig.isTextFollowFilter() && !AimModeAdapter.isValidForFilter(e)) continue;
                Vec3 pos = e.position().add(0, e.getBbHeight() + 0.5, 0);
                Vec3 proj = project(mvpMatrix, screenWidth, screenHeight, pos);
                if (proj == null || isBehindCamera(proj.z)) continue;
                double dist = mc.player != null ? mc.player.distanceTo(e) : 1.0;
                if (dist > AimConfig.getTextMaxDistance()) continue;
                pendingLabels.add(new LabelData(proj, dist, e, mvpMatrix, screenWidth, screenHeight));
            }
        }

        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();

        modelViewStack.popPose();
        RenderSystem.restoreProjectionMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderLabelsGUI(RenderGuiEvent.Post event) {
        if (!AimConfig.isEnableEntityRendering()) {
            pendingLabels.clear();
            return;
        }
        if (pendingLabels.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        RenderSystem.assertOnRenderThread();
        RenderSystem.backupProjectionMatrix();
        Matrix4f ortho = new Matrix4f().setOrtho(0, screenWidth, screenHeight, 0, -1000, 1000);
        RenderSystem.setProjectionMatrix(ortho, VertexSorting.DISTANCE_TO_ORIGIN);

        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        RenderSystem.applyModelViewMatrix();

        MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (LabelData data : pendingLabels) {
            drawLabelScreenGUI(data, buf, mc);
        }
        buf.endBatch();
        pendingLabels.clear(); // 绘制后清空，防止残留

        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void drawLabelScreenGUI(LabelData data, MultiBufferSource.BufferSource buf, Minecraft mc) {
        Vec3 proj = data.proj;
        double dist = data.dist;
        LivingEntity entity = data.entity;

        float scale = (float) Math.max(0.5, Math.min(1.5, 10.0 / dist));

        Font font = mc.font;
        Component text = EntityMarkerRenderer.buildText(entity, dist);
        float x = (float) proj.x;
        float y = (float) proj.y;

        PoseStack stack = new PoseStack();
        stack.pushPose();
        stack.translate(x, y, 0);
        stack.scale(scale, scale, 1.0f);
        stack.translate(-font.width(text) / 2f, 0, 0);

        int color = AimConfig.getTextColor();
        boolean shadow = AimConfig.isTextShadow();
        int bg = AimConfig.isTextBackground() ? 0x80000000 : 0;
        font.drawInBatch(text, 0, 0, color, shadow, stack.last().pose(), buf,
                Font.DisplayMode.NORMAL, bg, 15728880);
        stack.popPose();
    }

    private static Vec3 project(Matrix4f mvp, int width, int height, Vec3 worldPos) {
        Vector4f vec = new Vector4f((float)worldPos.x, (float)worldPos.y, (float)worldPos.z, 1.0f);
        vec.mul(mvp);
        if (vec.w == 0) return null;
        float ndcX = vec.x / vec.w;
        float ndcY = vec.y / vec.w;
        float ndcZ = vec.z / vec.w;
        float screenX = (ndcX + 1.0f) / 2.0f * width;
        float screenY = (1.0f - ndcY) / 2.0f * height;
        return new Vec3(screenX, screenY, ndcZ);
    }

    private static boolean isBehindCamera(double ndcZ) { return ndcZ < 0 || ndcZ > 1; }

    private static void drawLineToScreenCenter(Matrix4f mvp, int w, int h, Vec3 targetWorldPos,
                                               float centerX, float centerY,
                                               float[] color, BufferBuilder buffer) {
        Vec3 targetProj = project(mvp, w, h, targetWorldPos);
        if (targetProj == null || isBehindCamera(targetProj.z)) return;
        buffer.vertex(centerX, centerY, 0).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex((float)targetProj.x, (float)targetProj.y, 0).color(color[0], color[1], color[2], color[3]).endVertex();
    }

    private static void drawBoxScreen(Matrix4f mvp, int w, int h, LivingEntity entity,
                                      float[] color, BufferBuilder buffer) {
        AABB box = entity.getBoundingBox();
        double scale = AimConfig.getBoxScale();
        Vec3 center = new Vec3(
                (box.minX + box.maxX) / 2,
                (box.minY + box.maxY) / 2,
                (box.minZ + box.maxZ) / 2
        );
        Vec3[] corners = new Vec3[]{
                scaleVec(center, new Vec3(box.minX, box.minY, box.minZ), scale),
                scaleVec(center, new Vec3(box.maxX, box.minY, box.minZ), scale),
                scaleVec(center, new Vec3(box.maxX, box.minY, box.maxZ), scale),
                scaleVec(center, new Vec3(box.minX, box.minY, box.maxZ), scale),
                scaleVec(center, new Vec3(box.minX, box.maxY, box.minZ), scale),
                scaleVec(center, new Vec3(box.maxX, box.maxY, box.minZ), scale),
                scaleVec(center, new Vec3(box.maxX, box.maxY, box.maxZ), scale),
                scaleVec(center, new Vec3(box.minX, box.maxY, box.maxZ), scale)
        };

        Vec3[] proj = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            proj[i] = project(mvp, w, h, corners[i]);
            if (proj[i] == null) return;
        }

        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] edge : edges) {
            Vec3 a = proj[edge[0]];
            Vec3 b = proj[edge[1]];
            if (!isBehindCamera(a.z) && !isBehindCamera(b.z)) {
                buffer.vertex((float)a.x, (float)a.y, 0).color(color[0], color[1], color[2], color[3]).endVertex();
                buffer.vertex((float)b.x, (float)b.y, 0).color(color[0], color[1], color[2], color[3]).endVertex();
            }
        }
    }

    private static Vec3 scaleVec(Vec3 center, Vec3 point, double scale) {
        double dx = point.x - center.x;
        double dy = point.y - center.y;
        double dz = point.z - center.z;
        return new Vec3(center.x + dx * scale, center.y + dy * scale, center.z + dz * scale);
    }
}
