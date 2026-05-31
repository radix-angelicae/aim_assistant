package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.List;

public class EntityMarkerRenderer {
    public static void renderEntityMarkers(RenderLevelStageEvent event) {
        if (AimConfig.isScreenRenderMode()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !AimConfig.isEnableTextRendering()) return;
        LivingEntity player = mc.player;
        Level level = mc.level;
        PoseStack stack = event.getPoseStack();
        float pt = event.getPartialTick();
        double range = AimConfig.getSearchRange();
        Vec3 ppos = player.position();
        AABB area = new AABB(ppos.subtract(range, range, range), ppos.add(range, range, range));
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());

        boolean depthWasEnabled = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 在开始绘制所有标签前设为白色，绘制完成后再恢复
        float[] oldColor = RenderSystem.getShaderColor();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        MultiBufferSource.BufferSource buf = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (LivingEntity e : entities) {
            if (AimConfig.isTextFollowFilter() && !AimModeAdapter.isValidForFilter(e)) continue;
            renderLabel(e, stack, pt, buf, mc);
        }
        buf.endBatch(); // 此时着色器颜色为白色，标签不受环境光影响

        RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);

        if (depthWasEnabled) RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderLabel(LivingEntity e, PoseStack stack, float pt, MultiBufferSource.BufferSource buf, Minecraft mc) {
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        Vec3 pos = e.position().add(0, e.getBbHeight() + 0.5, 0);
        double dist = camPos.distanceTo(pos);
        if (dist > AimConfig.getTextMaxDistance()) return;

        float scale = (float) Math.max(0.01f, Math.min(AimConfig.getTextScale() * (1 + dist * 0.05), 0.1f));
        stack.pushPose();
        stack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        stack.mulPose(cam.rotation());
        stack.scale(-scale, -scale, scale);

        Font font = mc.font;
        Component text = buildText(e, dist);
        int color = AimConfig.getTextColor();
        boolean shadow = AimConfig.isTextShadow();
        int bg = AimConfig.isTextBackground() ? 0x80000000 : 0;
        font.drawInBatch(text, -font.width(text)/2f, 0, color, shadow, stack.last().pose(), buf,
                Font.DisplayMode.SEE_THROUGH, bg, 15728880);

        stack.popPose();
    }

    public static Component buildText(LivingEntity e, double dist) {
        StringBuilder sb = new StringBuilder();
        if (AimConfig.isTextShowName()) sb.append(e.getName().getString()).append(" ");
        if (AimConfig.isTextShowHealth()) sb.append("❤").append((int)e.getHealth()).append(" ");
        if (AimConfig.isTextShowArmor()) {
            int armor = e.getArmorValue();
            if (armor > 0) sb.append("🛡").append(armor).append(" ");
        }
        if (AimConfig.isTextShowDistance()) sb.append(String.format("%.1fm ", dist));
        if (AimConfig.isTextShowEntityType()) sb.append("[").append(e.getType().getDescription().getString()).append("]");
        return Component.literal(sb.toString().trim());
    }
}
