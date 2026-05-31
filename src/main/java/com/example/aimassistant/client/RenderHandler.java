package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class RenderHandler {

    public static void onRenderWorld(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!AimConfig.isEnableEntityRendering()) return;

        Player player = mc.player;
        Level level = mc.level;
        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        double range = AimConfig.getSearchRange();
        List<LivingEntity> entities = getNearbyEntities(player, level, range);
        entities = filterEntities(entities);
        entities.removeIf(AimModeAdapter::isExcluded);

        // 已移除距离裁剪功能，不再根据 cullDistance 过滤实体

        int max = AimConfig.getRenderMaxEntities();
        if (entities.size() > max) {
            entities = entities.stream()
                    .sorted((a, b) -> Double.compare(a.distanceTo(player), b.distanceTo(player)))
                    .limit(max)
                    .toList();
        }

        LivingEntity locked = AimHandler.TARGET;

        if (AimConfig.isScreenRenderMode()) {
            ScreenSpaceRenderer.renderAll(mc, camera, poseStack, entities, locked);
            return;
        }

        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 viewVec = player.getViewVector(1.0f);
        Vec3 crosshairPos = eyePos.add(viewVec.scale(AimConfig.getCrosshairDistance()));

        if (entities.isEmpty() && locked == null) return;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();

        poseStack.pushPose();
        Vec3 cameraPos = camera.getPosition();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float normalLineWidth = (float) AimConfig.getLineWidth();

        // 第一批次：普通实体和普通锁定框
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        GL11.glLineWidth(normalLineWidth);

        FilterMode filterMode = AimConfig.getFilterMode();
        for (LivingEntity entity : entities) {
            float[] lineColor = getColorFor(entity, true, filterMode);
            float[] boxColor  = getColorFor(entity, false, filterMode);

            if (AimConfig.isEnableLineRendering()) {
                addLineVertices(buffer, poseStack, crosshairPos, entity.position(), lineColor);
            }
            if (AimConfig.isEnableBoxRendering()) {
                addBoxVertices(buffer, poseStack, entity, boxColor);
            }
        }

        // 普通锁定框（线宽 + 2）
        if (locked != null && AimConfig.isGlowTarget() && locked.isAlive() &&
            AimModeAdapter.isValidForFilter(locked) && !AimModeAdapter.isExcluded(locked)) {
            GL11.glLineWidth(normalLineWidth + 2.0f);
            addBoxVertices(buffer, poseStack, locked, getGlowColor());
            GL11.glLineWidth(normalLineWidth);
        }

        tessellator.end();

        // 第二批次：强锁框
        if (locked != null && AimHandler.forceLocked && locked == AimHandler.forceLockedEntity) {
            if (AimConfig.isGlowTarget() && locked.isAlive() &&
                AimModeAdapter.isValidForFilter(locked) && !AimModeAdapter.isExcluded(locked)) {
                buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                GL11.glLineWidth((float) AimConfig.getForceLockLineWidth());
                addBoxVertices(buffer, poseStack, locked, getForceLockGlowColor());
                tessellator.end();
                GL11.glLineWidth(normalLineWidth);
            }
        }

        poseStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderRangeHelper(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        double range = AimConfig.getSearchRange();
        Vec3 center = mc.player.position();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        GL11.glLineWidth(1.0f);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        int segments = 64;
        float alpha = 0.3f;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            double x = center.x + range * Math.cos(angle);
            double y = center.y + range * Math.sin(angle);
            buffer.vertex(poseStack.last().pose(), (float)x, (float)y, (float)center.z).color(0.2f, 0.8f, 1f, alpha).endVertex();
        }
        tessellator.end();
        buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            double x = center.x + range * Math.cos(angle);
            double z = center.z + range * Math.sin(angle);
            buffer.vertex(poseStack.last().pose(), (float)x, (float)center.y, (float)z).color(0.2f, 0.8f, 1f, alpha).endVertex();
        }
        tessellator.end();
        buffer.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            double y = center.y + range * Math.cos(angle);
            double z = center.z + range * Math.sin(angle);
            buffer.vertex(poseStack.last().pose(), (float)center.x, (float)y, (float)z).color(0.2f, 0.8f, 1f, alpha).endVertex();
        }
        tessellator.end();

        poseStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static float[] getColorFor(LivingEntity entity, boolean isLine, FilterMode filterMode) {
        if (filterMode == FilterMode.SEARCH) {
            int rgb = isLine ? AimConfig.getLineColorSearch() : AimConfig.getBoxColorSearch();
            return rgbToFloat(rgb, isLine);
        }

        int rgb;
        if (AimModeAdapter.isHostileToPlayer(entity)) {
            rgb = isLine ? AimConfig.getLineColorMonster() : AimConfig.getBoxColorMonster();
        } else if (entity instanceof Player) {
            rgb = isLine ? AimConfig.getLineColorPlayer() : AimConfig.getBoxColorPlayer();
        } else if (entity instanceof Animal) {
            rgb = isLine ? AimConfig.getLineColorAnimal() : AimConfig.getBoxColorAnimal();
        } else {
            rgb = isLine ? AimConfig.getLineColorOther() : AimConfig.getBoxColorOther();
        }
        return rgbToFloat(rgb, isLine);
    }

    public static float[] getGlowColor() {
        int rgb = AimConfig.getGlowColor();
        return rgbToFloat(rgb, true);
    }

    public static float[] getForceLockGlowColor() {
        int rgb = AimConfig.getForceLockGlowColor();
        return rgbToFloat(rgb, true);
    }

    private static float[] rgbToFloat(int rgb, boolean isLine) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float a = isLine ? 1.0f : (float) AimConfig.getBoxAlpha();
        return new float[]{r, g, b, a};
    }

    private static List<LivingEntity> filterEntities(List<LivingEntity> all) {
        List<LivingEntity> filtered = new ArrayList<>();
        for (LivingEntity e : all) {
            if (AimModeAdapter.isValidForFilter(e)) filtered.add(e);
        }
        return filtered;
    }

    private static void addLineVertices(BufferBuilder buffer, PoseStack poseStack,
                                        Vec3 start, Vec3 end, float[] color) {
        buffer.vertex(poseStack.last().pose(), (float)start.x, (float)start.y, (float)start.z)
                .color(color[0], color[1], color[2], color[3])
                .endVertex();
        buffer.vertex(poseStack.last().pose(), (float)end.x, (float)end.y, (float)end.z)
                .color(color[0], color[1], color[2], color[3])
                .endVertex();
    }

    private static void addBoxVertices(BufferBuilder buffer, PoseStack poseStack,
                                       LivingEntity entity, float[] color) {
        AABB original = entity.getBoundingBox();
        double scale = AimConfig.getBoxScale();
        Vec3 center = new Vec3(
                (original.minX + original.maxX) / 2,
                (original.minY + original.maxY) / 2,
                (original.minZ + original.maxZ) / 2
        );
        Vec3[] corners = new Vec3[]{
                scaleVec(center, new Vec3(original.minX, original.minY, original.minZ), scale),
                scaleVec(center, new Vec3(original.maxX, original.minY, original.minZ), scale),
                scaleVec(center, new Vec3(original.maxX, original.minY, original.maxZ), scale),
                scaleVec(center, new Vec3(original.minX, original.minY, original.maxZ), scale),
                scaleVec(center, new Vec3(original.minX, original.maxY, original.minZ), scale),
                scaleVec(center, new Vec3(original.maxX, original.maxY, original.minZ), scale),
                scaleVec(center, new Vec3(original.maxX, original.maxY, original.maxZ), scale),
                scaleVec(center, new Vec3(original.minX, original.maxY, original.maxZ), scale)
        };

        BoxMode mode = AimConfig.getBoxMode();
        switch (mode) {
            case FULL_3D -> {
                int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
                for (int[] e : edges) addLineVertices(buffer, poseStack, corners[e[0]], corners[e[1]], color);
            }
            case CORNERS -> {
                double len = 0.3;
                int[][] signs = {{-1,-1,-1},{1,-1,-1},{1,-1,1},{-1,-1,1},{-1,1,-1},{1,1,-1},{1,1,1},{-1,1,1}};
                for (int i = 0; i < 8; i++) {
                    Vec3 v = corners[i];
                    double dx = signs[i][0] * len;
                    double dy = signs[i][1] * len;
                    double dz = signs[i][2] * len;
                    addLineVertices(buffer, poseStack, v, v.add(dx, 0, 0), color);
                    addLineVertices(buffer, poseStack, v, v.add(0, dy, 0), color);
                    addLineVertices(buffer, poseStack, v, v.add(0, 0, dz), color);
                }
            }
            case BOTTOM -> {
                addLineVertices(buffer, poseStack, corners[0], corners[1], color);
                addLineVertices(buffer, poseStack, corners[1], corners[2], color);
                addLineVertices(buffer, poseStack, corners[2], corners[3], color);
                addLineVertices(buffer, poseStack, corners[3], corners[0], color);
            }
            case TOP -> {
                addLineVertices(buffer, poseStack, corners[4], corners[5], color);
                addLineVertices(buffer, poseStack, corners[5], corners[6], color);
                addLineVertices(buffer, poseStack, corners[6], corners[7], color);
                addLineVertices(buffer, poseStack, corners[7], corners[4], color);
            }
        }
    }

    private static Vec3 scaleVec(Vec3 center, Vec3 point, double scale) {
        double dx = point.x - center.x;
        double dy = point.y - center.y;
        double dz = point.z - center.z;
        return new Vec3(center.x + dx * scale, center.y + dy * scale, center.z + dz * scale);
    }

    private static List<LivingEntity> getNearbyEntities(Player player, Level level, double range) {
        Vec3 pos = player.position();
        AABB area = new AABB(pos.subtract(range, range, range), pos.add(range, range, range));
        return level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());
    }
}
