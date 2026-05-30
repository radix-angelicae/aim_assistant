package com.example.aimassistant;

import com.example.aimassistant.client.*;
import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.EntityCategoryConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import static com.example.aimassistant.key.ModKeyMapping.*;

@Mod(AimAssistant.MODID)
public class AimAssistant {
    public static final String MODID = "aim_assistant";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ModConfig CLIENT_CONFIG;

    public AimAssistant() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ModList.get().getModContainerById(MODID).ifPresent(container -> {
            CLIENT_CONFIG = new ModConfig(ModConfig.Type.CLIENT, AimConfig.SPEC, container, MODID + "-client.toml");
            container.addConfig(CLIENT_CONFIG);
        });
        MinecraftForge.EVENT_BUS.register(this);
        // Load entity categories
        EntityCategoryConfig.loadConfig(FMLPaths.CONFIGDIR.get().resolve(MODID + "/entity_categories.json"));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.debug("Aim Assistant initialization complete");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientRenderEvents {
        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            ScreenSpaceRenderer.renderLabelsGUI(event);
        }
        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            if (AimModeAdapter.useRenderingOn) {
                if (AimConfig.isEnableEntityRendering()) {
                    RenderHandler.onRenderWorld(event);
                    EntityMarkerRenderer.renderEntityMarkers(event);
                }
                if (ConfigScreen.showRangeHelper) {
                    RenderHandler.renderRangeHelper(event);
                }
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            // decrement attack cooldown

            while (SET_RENDERING_ON.consumeClick()) {
                AimModeAdapter.useRenderingOn = !AimModeAdapter.useRenderingOn;
                var player = Minecraft.getInstance().player;
                if (player != null) player.sendSystemMessage(AimModeAdapter.useRenderingOn ?
                        Component.translatable("message.aim_assistant.render_on") :
                        Component.translatable("message.aim_assistant.render_off"));
            }
            while (OPEN_CONFIG.consumeClick()) Minecraft.getInstance().setScreen(new ConfigScreen());

            while (TOGGLE_FILTER.consumeClick()) {
                AimConfig.FilterMode current = AimConfig.getFilterMode();
                AimConfig.FilterMode[] values = AimConfig.FilterMode.values();
                AimConfig.FilterMode next = values[(current.ordinal() + 1) % values.length];
                AimConfig.FILTER_MODE.set(next);
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    String msg = switch (next) {
                        case ALL -> "message.aim_assistant.filter.all";
                        case HOSTILE -> "message.aim_assistant.filter.hostile";
                        case ANIMAL -> "message.aim_assistant.filter.animal";
                        case SEARCH -> "message.aim_assistant.filter.search";
                    };
                    player.sendSystemMessage(Component.translatable(msg));
                }
            }

            while (TOGGLE_TARGET_SWITCH.consumeClick()) {
                boolean current = AimConfig.isAllowTargetSwitching();
                AimConfig.ALLOW_TARGET_SWITCHING.set(!current);
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.sendSystemMessage(Component.translatable(
                            "message.aim_assistant.target_switch." + (!current ? "on" : "off")));
                }
            }

            while (LOCK_TARGET.consumeClick()) {
                AimHandler.handleLockKeyPress();
            }
            while (NEXT_TARGET.consumeClick()) {
                if (AimConfig.isAllowTargetSwitching()) {
                    AimHandler.nextTarget();
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            dispatcher.register(Commands.literal("aim_reload")
                    .executes(ctx -> {
                        EntityCategoryConfig.loadConfig(FMLPaths.CONFIGDIR.get().resolve(MODID + "/entity_categories.json"));
                        var player = Minecraft.getInstance().player;
                        if (player != null) {
                            player.sendSystemMessage(Component.literal("§a[aim_assistant] Entity categories reloaded."));
                        }
                        return 1;
                    }));
        }
    }
}
