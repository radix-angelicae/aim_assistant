package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import java.util.List;

public class EntityMarkerRenderer {
    public static void renderEntityMarkers(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !AimConfig.isEnableTextRendering()) return;
        LivingEntity player = mc.player;
        World level = mc.level;
        MatrixStack stack = event.getMatrixStack();
        float pt = event.getPartialTicks();
        double range = AimConfig.getSearchRange();
        Vector3d ppos = player.position();
        net.minecraft.util.math.AxisAlignedBB area = new net.minecraft.util.math.AxisAlignedBB(
                ppos.x - range, ppos.y - range, ppos.z - range,
                ppos.x + range, ppos.y + range, ppos.z + range);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());

        // 透视渲染
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        IRenderTypeBuffer.Impl buf = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
        for (LivingEntity e : entities) {
            if (AimConfig.isTextFollowFilter() && !AimModeAdapter.isValidForFilter(e)) continue;
            renderLabel(e, stack, pt, buf, mc);
        }
        buf.endBatch();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderLabel(LivingEntity e, MatrixStack stack, float pt, IRenderTypeBuffer.Impl buf, Minecraft mc) {
        net.minecraft.client.renderer.ActiveRenderInfo cam = mc.gameRenderer.getMainCamera();
        Vector3d camPos = cam.getPosition();
        Vector3d pos = e.position().add(0, e.getBbHeight() + 0.5, 0);
        double dist = camPos.distanceTo(pos);
        if (dist > AimConfig.getTextMaxDistance()) return;

        float scale = (float) Math.max(0.01f, Math.min(AimConfig.getTextScale() * (1 + dist * 0.05), 0.1f));
        stack.pushPose();
        stack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        stack.mulPose(cam.rotation());
        stack.scale(-scale, -scale, scale);

        FontRenderer font = mc.font;
        ITextComponent text = buildText(e, dist);
        int color = AimConfig.getTextColor();
        boolean shadow = AimConfig.isTextShadow();
        int bg = AimConfig.isTextBackground() ? 0x80000000 : 0;
        font.drawInBatch(text, -font.width(text)/2f, 0, color, shadow, stack.last().pose(), buf, true, bg, 15728880);

        stack.popPose();
    }

    private static ITextComponent buildText(LivingEntity e, double dist) {
        StringBuilder sb = new StringBuilder();
        if (AimConfig.isTextShowName()) sb.append(e.getName().getString()).append(" ");
        if (AimConfig.isTextShowHealth()) sb.append("❤").append((int)e.getHealth()).append(" ");
        if (AimConfig.isTextShowArmor()) {
            int armor = e.getArmorValue();
            if (armor > 0) sb.append("🛡").append(armor).append(" ");
        }
        if (AimConfig.isTextShowDistance()) sb.append(String.format("%.1fm ", dist));
        if (AimConfig.isTextShowEntityType()) sb.append("[").append(e.getType().getDescription().getString()).append("]");
        return new StringTextComponent(sb.toString().trim());
    }
}