package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.FilterMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.regex.Pattern;

public class AimModeAdapter {
    public static volatile boolean useRenderingOn = false;

    public static boolean isHostileToPlayer(LivingEntity entity) {
        return entity instanceof MonsterEntity;
    }

    public static boolean isExcluded(LivingEntity entity) {
        String id = ForgeRegistries.ENTITIES.getKey(entity.getType()).toString();
        for (String pattern : AimConfig.getExclusionList()) {
            if (wildcardMatch(pattern, id)) return true;
        }
        return false;
    }

    public static boolean isInSearchList(LivingEntity entity) {
        String id = ForgeRegistries.ENTITIES.getKey(entity.getType()).toString();
        for (String pattern : AimConfig.getSearchList()) {
            if (wildcardMatch(pattern, id)) return true;
        }
        return false;
    }

    private static boolean wildcardMatch(String pattern, String text) {
        String regex = Pattern.quote(pattern).replace("*", "\\E.*\\Q");
        return Pattern.matches(regex, text);
    }

    public static boolean isValidForFilter(LivingEntity entity) {
        FilterMode mode = AimConfig.getFilterMode();
        if (entity instanceof PlayerEntity && mode != FilterMode.SEARCH) {
            return false;
        }
        switch (mode) {
            case ALL: return !(entity instanceof PlayerEntity);
            case HOSTILE: return isHostileToPlayer(entity);
            case ANIMAL: return entity instanceof AnimalEntity && !isHostileToPlayer(entity);
            case SEARCH: return isInSearchList(entity);
            default: return false;
        }
    }
}
