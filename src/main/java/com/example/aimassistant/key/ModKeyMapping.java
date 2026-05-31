package com.example.aimassistant.key;

import com.example.aimassistant.AimAssistant;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AimAssistant.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyMapping {
    public static final KeyMapping AIM_BUTTON = new KeyMapping(
            "key.aim_assistant.aim_button",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyMapping SET_RENDERING_ON = new KeyMapping(
            "key.aim_assistant.persistent_render",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.aim_assistant.open_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyMapping TOGGLE_FILTER = new KeyMapping(
            "key.aim_assistant.toggle_filter",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyMapping TOGGLE_TARGET_SWITCH = new KeyMapping(
            "key.aim_assistant.toggle_target_switch",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyMapping NEXT_TARGET = new KeyMapping(
            "key.aim_assistant.next_target",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );
    public static final KeyMapping LOCK_TARGET = new KeyMapping(
            "key.aim_assistant.lock_target",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.aim_assistant.category"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(AIM_BUTTON);
        event.register(SET_RENDERING_ON);
        event.register(OPEN_CONFIG);
        event.register(TOGGLE_FILTER);
        event.register(TOGGLE_TARGET_SWITCH);
        event.register(NEXT_TARGET);
        event.register(LOCK_TARGET);
    }
}
