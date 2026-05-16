package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.List;

public class RenderHandler {

    public static void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        PlayerEntity player = mc.player;
        World level = mc.level;
        MatrixStack poseStack = event.getMatrixStack();
        float partialTick = event.getPartialTicks();

        Vector3d eyePos = player.getEyePosition(partialTick);
        Vector3d viewVec = player.getViewVector(1.0F);
        Vector3d crosshairPos = eyePos.add(viewVec.scale(AimConfig.getCrosshairDistance()));

        double range = AimConfig.getSearchRange();
        List<LivingEntity> entities = getNearbyEntities(player, level, range);
        entities = filterEntities(entities);
        entities.removeIf(AimModeAdapter::isExcluded);

        if (AimConfig.isDistanceCulling()) {
            double cullDist = AimConfig.getCullDistance();
            entities.removeIf(e -> e.distanceTo(player) > cullDist);
        }
        int max = AimConfig.getRenderMaxEntities();
        if (entities.size() > max) {
            entities = entities.stream()
                    .sorted(Comparator.comparingDouble(e -> e.distanceTo(player)))
                    .limit(max)
                    .collect(Collectors.toList());
        }

        if (entities.isEmpty() && AimHandler.TARGET == null) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth((float) AimConfig.getLineWidth());

        poseStack.pushPose();
        Vector3d camPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

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

        LivingEntity locked = AimHandler.TARGET;
        if (locked != null && AimConfig.isGlowTarget() && locked.isAlive()) {
            float[] glowColor = getGlowColor(locked);
            RenderSystem.lineWidth((float) (AimConfig.getLineWidth() + 2.0f));
            addBoxVertices(buffer, poseStack, locked, glowColor);
            RenderSystem.lineWidth((float) AimConfig.getLineWidth());
        }

        tessellator.end();
        poseStack.popPose();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static float[] getGlowColor(LivingEntity entity) {
        int rgb = AimConfig.getGlowColor();
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        return new float[]{r, g, b, 1.0f};
    }

    private static float[] getColorFor(LivingEntity entity, boolean isLine, FilterMode filterMode) {
        if (filterMode == FilterMode.SEARCH) {
            int rgb = isLine ? AimConfig.getLineColorSearch() : AimConfig.getBoxColorSearch();
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;
            float a = isLine ? 1.0f : (float)AimConfig.getBoxAlpha();
            return new float[]{r, g, b, a};
        }

        int rgb;
        if (AimModeAdapter.isHostileToPlayer(entity)) {
            rgb = isLine ? AimConfig.getLineColorMonster() : AimConfig.getBoxColorMonster();
        } else if (entity instanceof PlayerEntity) {
            rgb = isLine ? AimConfig.getLineColorPlayer() : AimConfig.getBoxColorPlayer();
        } else if (entity instanceof AnimalEntity) {
            rgb = isLine ? AimConfig.getLineColorAnimal() : AimConfig.getBoxColorAnimal();
        } else {
            rgb = isLine ? AimConfig.getLineColorOther() : AimConfig.getBoxColorOther();
        }
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float a = isLine ? 1.0f : (float)AimConfig.getBoxAlpha();
        return new float[]{r, g, b, a};
    }

    private static List<LivingEntity> filterEntities(List<LivingEntity> all) {
        List<LivingEntity> filtered = new ArrayList<>();
        for (LivingEntity e : all) {
            if (AimModeAdapter.isValidForFilter(e)) filtered.add(e);
        }
        return filtered;
    }

    private static void addLineVertices(BufferBuilder buffer, MatrixStack poseStack, Vector3d start, Vector3d end, float[] color) {
        Matrix4f mat = poseStack.last().pose();
        buffer.vertex(mat, (float)start.x, (float)start.y, (float)start.z).color(color[0], color[1], color[2], color[3]).endVertex();
        buffer.vertex(mat, (float)end.x, (float)end.y, (float)end.z).color(color[0], color[1], color[2], color[3]).endVertex();
    }

    private static void addBoxVertices(BufferBuilder buffer, MatrixStack poseStack, LivingEntity entity, float[] color) {
        AxisAlignedBB original = entity.getBoundingBox();
        double scale = AimConfig.getBoxScale();
        Vector3d center = new Vector3d(
                (original.minX + original.maxX) / 2,
                (original.minY + original.maxY) / 2,
                (original.minZ + original.maxZ) / 2
        );
        Vector3d[] corners = new Vector3d[]{
                scaleVec(center, new Vector3d(original.minX, original.minY, original.minZ), scale),
                scaleVec(center, new Vector3d(original.maxX, original.minY, original.minZ), scale),
                scaleVec(center, new Vector3d(original.maxX, original.minY, original.maxZ), scale),
                scaleVec(center, new Vector3d(original.minX, original.minY, original.maxZ), scale),
                scaleVec(center, new Vector3d(original.minX, original.maxY, original.minZ), scale),
                scaleVec(center, new Vector3d(original.maxX, original.maxY, original.minZ), scale),
                scaleVec(center, new Vector3d(original.maxX, original.maxY, original.maxZ), scale),
                scaleVec(center, new Vector3d(original.minX, original.maxY, original.maxZ), scale)
        };

        BoxMode mode = AimConfig.getBoxMode();
        switch (mode) {
            case FULL_3D: {
                int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
                for (int[] e : edges) addLineVertices(buffer, poseStack, corners[e[0]], corners[e[1]], color);
                break;
            }
            case CORNERS: {
                double len = 0.3;
                int[][] signs = {{-1,-1,-1},{1,-1,-1},{1,-1,1},{-1,-1,1},{-1,1,-1},{1,1,-1},{1,1,1},{-1,1,1}};
                for (int i = 0; i < 8; i++) {
                    Vector3d v = corners[i];
                    double dx = signs[i][0] * len;
                    double dy = signs[i][1] * len;
                    double dz = signs[i][2] * len;
                    addLineVertices(buffer, poseStack, v, v.add(dx, 0, 0), color);
                    addLineVertices(buffer, poseStack, v, v.add(0, dy, 0), color);
                    addLineVertices(buffer, poseStack, v, v.add(0, 0, dz), color);
                }
                break;
            }
            case BOTTOM: {
                addLineVertices(buffer, poseStack, corners[0], corners[1], color);
                addLineVertices(buffer, poseStack, corners[1], corners[2], color);
                addLineVertices(buffer, poseStack, corners[2], corners[3], color);
                addLineVertices(buffer, poseStack, corners[3], corners[0], color);
                break;
            }
            case TOP: {
                addLineVertices(buffer, poseStack, corners[4], corners[5], color);
                addLineVertices(buffer, poseStack, corners[5], corners[6], color);
                addLineVertices(buffer, poseStack, corners[6], corners[7], color);
                addLineVertices(buffer, poseStack, corners[7], corners[4], color);
                break;
            }
        }
    }

    private static Vector3d scaleVec(Vector3d center, Vector3d point, double scale) {
        double dx = point.x - center.x;
        double dy = point.y - center.y;
        double dz = point.z - center.z;
        return new Vector3d(center.x + dx * scale, center.y + dy * scale, center.z + dz * scale);
    }

    private static List<LivingEntity> getNearbyEntities(PlayerEntity player, World level, double range) {
        Vector3d pos = player.position();
        AxisAlignedBB area = new AxisAlignedBB(pos.x - range, pos.y - range, pos.z - range, pos.x + range, pos.y + range, pos.z + range);
        return level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
    }
}
