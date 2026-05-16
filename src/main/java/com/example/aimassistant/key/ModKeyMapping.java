package com.example.aimassistant.key;

import com.example.aimassistant.AimAssistant;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = AimAssistant.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeyMapping {
    public static final KeyBinding AIM_BUTTON = new KeyBinding(
            "key.aim_assistant.aim_button",
            KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM,
            InputMappings.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyBinding SET_RENDERING_ON = new KeyBinding(
            "key.aim_assistant.persistent_render",
            KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM,
            InputMappings.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyBinding OPEN_CONFIG = new KeyBinding(
            "key.aim_assistant.open_config",
            KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM,
            InputMappings.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyBinding TOGGLE_FILTER = new KeyBinding(
            "key.aim_assistant.toggle_filter",
            KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM,
            InputMappings.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    // 新键位：开关目标切换（单一锁定）
    public static final KeyBinding TOGGLE_TARGET_SWITCH = new KeyBinding(
            "key.aim_assistant.toggle_target_switch",
            KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM,
            InputMappings.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );

    @SubscribeEvent
    public static void registerKeys(FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(AIM_BUTTON);
        ClientRegistry.registerKeyBinding(SET_RENDERING_ON);
        ClientRegistry.registerKeyBinding(OPEN_CONFIG);
        ClientRegistry.registerKeyBinding(TOGGLE_FILTER);
        ClientRegistry.registerKeyBinding(TOGGLE_TARGET_SWITCH);
    }
}