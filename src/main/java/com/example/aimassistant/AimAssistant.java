package com.example.aimassistant;

import com.example.aimassistant.client.*;
import com.example.aimassistant.config.AimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.example.aimassistant.key.ModKeyMapping.*;

@Mod(AimAssistant.MODID)
public class AimAssistant {
    public static final String MODID = "aim_assistant";
    public static final Logger LOGGER = LogManager.getLogger();
    public static ModConfig CLIENT_CONFIG;

    public AimAssistant() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModList.get().getModContainerById(MODID).ifPresent(container -> {
            CLIENT_CONFIG = new ModConfig(ModConfig.Type.CLIENT, AimConfig.SPEC, container, MODID + "-client.toml");
            container.addConfig(CLIENT_CONFIG);
        });

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("AimAssistant 1.16.5 port initialized");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {}

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientRenderEvents {
        @SubscribeEvent
        public static void onRenderWorld(RenderWorldLastEvent event) {
            if (AimModeAdapter.useRenderingOn && AimConfig.isEnableEntityRendering()) {
                RenderHandler.onRenderWorld(event);
                EntityMarkerRenderer.renderEntityMarkers(event);
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            while (SET_RENDERING_ON.consumeClick()) {
                AimModeAdapter.useRenderingOn = !AimModeAdapter.useRenderingOn;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendMessage(new TranslationTextComponent(AimModeAdapter.useRenderingOn ?
                            "message.aim_assistant.render_on" : "message.aim_assistant.render_off"), mc.player.getUUID());
                }
            }
            while (OPEN_CONFIG.consumeClick()) {
                Minecraft.getInstance().setScreen(new ConfigScreen());
            }
            while (TOGGLE_FILTER.consumeClick()) {
                AimConfig.FilterMode current = AimConfig.getFilterMode();
                AimConfig.FilterMode[] values = AimConfig.FilterMode.values();
                AimConfig.FilterMode next = values[(current.ordinal() + 1) % values.length];
                AimConfig.FILTER_MODE.set(next);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    String msg = "";
                    switch (next) {
                        case ALL: msg = "message.aim_assistant.filter.all"; break;
                        case HOSTILE: msg = "message.aim_assistant.filter.hostile"; break;
                        case ANIMAL: msg = "message.aim_assistant.filter.animal"; break;
                        case SEARCH: msg = "message.aim_assistant.filter.search"; break;
                    }
                    mc.player.sendMessage(new TranslationTextComponent(msg), mc.player.getUUID());
                }
            }
            // 新键位：切换“允许目标切换”开关
            while (TOGGLE_TARGET_SWITCH.consumeClick()) {
                AimConfig.ALLOW_TARGET_SWITCHING.set(!AimConfig.ALLOW_TARGET_SWITCHING.get());
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendMessage(new TranslationTextComponent(
                            AimConfig.ALLOW_TARGET_SWITCHING.get() ?
                                    "message.aim_assistant.target_switch_on" :
                                    "message.aim_assistant.target_switch_off"
                    ), mc.player.getUUID());
                }
            }
        }
    }
}