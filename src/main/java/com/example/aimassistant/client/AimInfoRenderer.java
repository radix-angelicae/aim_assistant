package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "aim_assistant", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class AimInfoRenderer {
    @SubscribeEvent
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!AimConfig.isShowAimInfo() && !AimConfig.isShowAimPanel()) return;
        LivingEntity target = AimHandler.TARGET;
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MatrixStack matrixStack = event.getMatrixStack();
        FontRenderer font = mc.font;

        if (AimConfig.isShowAimInfo()) {
            String info = String.format("Target: %s  HP: %.0f  Dist: %.1f",
                    target.getName().getString(),
                    target.getHealth(),
                    mc.player.distanceTo(target));
            int x = (int)(mc.getWindow().getGuiScaledWidth() * AimConfig.getAimInfoXPercent() / 100.0);
            int y = (int)(mc.getWindow().getGuiScaledHeight() * AimConfig.getAimInfoYPercent() / 100.0);
            font.draw(matrixStack, info, x, y, 0xFFFF00);
        }

        if (AimConfig.isShowAimPanel()) {
            drawAimPanel(matrixStack, target, mc, font);
        }
    }

    private static void drawAimPanel(MatrixStack matrixStack, LivingEntity target, Minecraft mc, FontRenderer font) {
        int x = 10, y = 30;
        int color = 0xFFFFFF;
        if (AimConfig.isAimPanelShowName()) {
            font.draw(matrixStack, "Name: " + target.getName().getString(), x, y, color);
            y += 12;
        }
        if (AimConfig.isAimPanelShowHealth()) {
            font.draw(matrixStack, "Health: " + String.format("%.0f", target.getHealth()), x, y, color);
            y += 12;
        }
        if (target instanceof PlayerEntity && AimConfig.isAimPanelShowArmor()) {
            font.draw(matrixStack, "Armor: " + ((PlayerEntity)target).getArmorValue(), x, y, color);
            y += 12;
        }
        if (AimConfig.isAimPanelShowDistance()) {
            font.draw(matrixStack, "Dist: " + String.format("%.1f", mc.player.distanceTo(target)), x, y, color);
            y += 12;
        }
        if (AimConfig.isAimPanelShowType()) {
            font.draw(matrixStack, "Type: " + target.getType().getDescription().getString(), x, y, color);
        }
    }
}
