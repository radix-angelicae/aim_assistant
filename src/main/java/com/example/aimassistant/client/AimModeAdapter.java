package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.FilterMode;
import com.example.aimassistant.config.EntityCategoryConfig;
import com.example.aimassistant.util.WildcardUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistries;

public class AimModeAdapter {
    public static volatile boolean useRenderingOn = false;

    /** Check if entity is hostile towards the player. */
    public static boolean isHostileToPlayer(LivingEntity entity) {
        // Prioritize external category config
        if (EntityCategoryConfig.isLoaded()) {
            String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            if (EntityCategoryConfig.isHostile(id)) return true;
            if (EntityCategoryConfig.isFriendly(id)) return false;
        }
        // Vanilla monster check
        if (entity instanceof Monster) return true;
        // Smart detection
        if (AimConfig.useSmartHostileDetection() && entity instanceof net.minecraft.world.entity.Mob mob) {
            return mob.getTarget() == net.minecraft.client.Minecraft.getInstance().player;
        }
        return false;
    }

    /** True if entity is excluded (supports wildcards and tags #). */
    public static boolean isExcluded(LivingEntity entity) {
        String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        for (String pattern : AimConfig.getExclusionList()) {
            if (matchesEntityPattern(pattern, id)) return true;
        }
        return false;
    }

    /** True if entity is in search list. */
    public static boolean isInSearchList(LivingEntity entity) {
        String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        for (String pattern : AimConfig.getSearchList()) {
            if (matchesEntityPattern(pattern, id)) return true;
        }
        return false;
    }

    private static boolean matchesEntityPattern(String pattern, String entityId) {
        if (pattern.startsWith("#")) {
            // Tag match
            ResourceLocation tagName = ResourceLocation.tryParse(pattern.substring(1));
            if (tagName != null) {
                TagKey<net.minecraft.world.entity.EntityType<?>> tag = TagKey.create(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), tagName);
                ResourceLocation rl = ResourceLocation.tryParse(entityId);
        if (rl != null) return ForgeRegistries.ENTITY_TYPES.getValue(rl).is(tag);
        return false;
            }
            return false;
        }
        // Wildcard match
        return WildcardUtil.matches(pattern, entityId);
    }

    /** Unified filter used for aim and rendering. */
    public static boolean isValidForFilter(LivingEntity entity) {
        FilterMode mode = AimConfig.getFilterMode();
        // Never auto‑include players unless in SEARCH mode and in search list
        if (entity instanceof Player && mode != FilterMode.SEARCH) {
            return false;
        }
        // Apply teammate / pet ignore
        if (entity instanceof Player player && AimConfig.isIgnoreTeammates()) {
            var localPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (localPlayer != null && localPlayer.getTeam() != null && player.getTeam() != null &&
                localPlayer.getTeam().equals(player.getTeam())) {
                return false;
            }
        }
        if (AimConfig.isIgnoreTamedPets() && entity instanceof net.minecraft.world.entity.OwnableEntity ownable) {
            var localPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (localPlayer != null && ownable.getOwnerUUID() != null && ownable.getOwnerUUID().equals(localPlayer.getUUID())) {
                return false;
            }
        }

        return switch (mode) {
            case ALL -> !(entity instanceof Player);
            case HOSTILE -> isHostileToPlayer(entity);
            case ANIMAL -> (entity instanceof Animal || EntityCategoryConfig.isFriendly(
                    ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString())) && !isHostileToPlayer(entity);
            case SEARCH -> isInSearchList(entity);
        };
    }
}
