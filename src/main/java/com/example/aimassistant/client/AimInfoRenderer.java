package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = "aim_assistant", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class AimInfoRenderer {
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!AimConfig.isShowAimInfo() && !AimConfig.isShowAimPanel()) return;
        LivingEntity target = AimHandler.TARGET;
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuiGraphics gui = event.getGuiGraphics();

        if (AimConfig.isShowAimInfo()) {
            String info = String.format("Target: %s   HP: %.0f   Dist: %.1f",
                    target.getName().getString(),
                    target.getHealth(),
                    mc.player.distanceTo(target));
            int x = (int)(mc.getWindow().getGuiScaledWidth() * AimConfig.getAimInfoXPercent() / 100.0);
            int y = (int)(mc.getWindow().getGuiScaledHeight() * AimConfig.getAimInfoYPercent() / 100.0);
            gui.drawString(mc.font, info, x, y, 0xFFFF00);
        }

        if (AimConfig.isShowAimPanel()) {
            drawAimPanel(gui, target, mc);
        }
    }

    private static void drawAimPanel(GuiGraphics gui, LivingEntity target, Minecraft mc) {
        int x = 10, y = 30;
        int color = 0xFFFFFF;
        if (AimConfig.isAimPanelShowName()) {
            gui.drawString(mc.font, "Name: " + target.getName().getString(), x, y, color); y += 12;
        }
        if (AimConfig.isAimPanelShowHealth()) {
            gui.drawString(mc.font, "HP: " + String.format("%.0f", target.getHealth()), x, y, color); y += 12;
        }
        if (AimConfig.isAimPanelShowMaxHealth()) {
            gui.drawString(mc.font, "Max HP: " + String.format("%.0f", target.getMaxHealth()), x, y, color); y += 12;
        }
        if (target instanceof Player && AimConfig.isAimPanelShowArmor()) {
            gui.drawString(mc.font, "Armor: " + ((Player)target).getArmorValue(), x, y, color); y += 12;
        }
        if (AimConfig.isAimPanelShowDistance()) {
            gui.drawString(mc.font, "Dist: " + String.format("%.1f", mc.player.distanceTo(target)), x, y, color); y += 12;
        }
        if (AimConfig.isAimPanelShowType()) {
            gui.drawString(mc.font, "Type: " + target.getType().getDescription().getString(), x, y, color); y += 12;
        }
        if (AimConfig.isAimPanelShowEffects()) {
            Collection<MobEffectInstance> effects = target.getActiveEffects();
            if (!effects.isEmpty()) {
                StringBuilder sb = new StringBuilder("Effects: ");
                for (MobEffectInstance effect : effects) {
                    sb.append(effect.getEffect().getDisplayName().getString()).append(" ");
                }
                gui.drawString(mc.font, sb.toString().trim(), x, y, color); y += 12;
            }
        }
        if (AimConfig.isAimPanelShowHeldItem()) {
            var item = target.getMainHandItem();
            if (!item.isEmpty()) {
                gui.drawString(mc.font, "Holding: " + item.getHoverName().getString(), x, y, color); y += 12;
            }
        }
    }
}
