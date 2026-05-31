package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static com.example.aimassistant.key.ModKeyMapping.AIM_BUTTON;

@Mod.EventBusSubscriber(modid = "aim_assistant", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class AimHandler {
    public static volatile LivingEntity TARGET = null;
    private static boolean wasPressed = false;
    private static boolean aimToggled = false;
    private static LivingEntity lastLockedEntity = null;

    // 强制锁死目标
    public static boolean forceLocked = false;
    public static LivingEntity forceLockedEntity = null;

    @SubscribeEvent
    public static void aimBot(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (AimConfig.isConditionalActivation()) {
            if (!isHoldingActivationItem(player)) return;
        }

        KeyMode keyMode = AimConfig.getAimKeyMode();
        boolean keyDown = AIM_BUTTON.isDown();
        boolean activated = (keyMode == KeyMode.HOLD && keyDown) || (keyMode == KeyMode.TOGGLE && aimToggled);

        if (keyMode == KeyMode.TOGGLE) {
            if (keyDown && !wasPressed) aimToggled = !aimToggled;
            wasPressed = keyDown;
        }

        if (activated) {
            // 先验证当前目标是否有效（普通检查）
            if (TARGET != null && !isTargetValid(player, TARGET)) {
                TARGET = null;
            }

            // 强锁目标使用宽松检查（忽略穿墙）
            if (forceLocked && forceLockedEntity != null && isTargetValidForForceLock(player, forceLockedEntity)) {
                TARGET = forceLockedEntity;
            } else {
                if (forceLocked) {
                    forceLocked = false;
                    forceLockedEntity = null;
                }
                if (TARGET == null) {
                    searchTarget(player);
                }
            }

            if (TARGET != null) {
                if (TARGET != lastLockedEntity && AimConfig.isLockSoundEnabled()) {
                    player.playSound(SoundEvents.NOTE_BLOCK_PLING.get(), (float) AimConfig.getLockSoundVolume(), 1.0f);
                }
                lastLockedEntity = TARGET;
                lockTarget(player, (float) AimConfig.getSmoothFactor());

                if (AimConfig.isAutoAttack()) {
                    if (player.getAttackStrengthScale(0.0f) >= 1.0f &&
                        player.distanceTo(TARGET) <= player.getEntityReach()) {
                        if (mc.gameMode != null) {
                            mc.gameMode.attack(player, TARGET);
                            player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                }
            }
        } else {
            TARGET = null;
            lastLockedEntity = null;
        }
    }

    private static void searchTarget(LocalPlayer player) {
        if (TARGET != null && !AimConfig.isAllowTargetSwitching()) return;

        double range = AimConfig.getSearchRange();
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive()
        );
        entities.removeIf(e -> !passCustomFilters(player, e));
        entities.removeIf(e -> !AimConfig.isAllowWallPenetration() && !canSeeEntity(player, e));
        double lockDist = AimConfig.getLockDistance();
        entities.removeIf(e -> e.distanceTo(player) > lockDist);

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        double halfFov = AimConfig.getFovAngle() / 2.0;
        entities.removeIf(e -> {
            Vec3 toEntity = e.getEyePosition(1.0f).subtract(eyePos);
            double len = toEntity.length();
            if (len == 0) return false;
            double cos = lookVec.dot(toEntity.normalize());
            return Math.toDegrees(Math.acos(cos)) > halfFov;
        });

        if (entities.isEmpty()) {
            TARGET = null;
            return;
        }

        TargetPriority priority = AimConfig.getTargetPriority();
        if (priority == TargetPriority.RANDOM) {
            Collections.shuffle(entities, new Random());
            TARGET = entities.get(0);
            return;
        }

        Comparator<LivingEntity> comp = switch (priority) {
            case DISTANCE -> Comparator.comparingDouble(e -> e.position().distanceTo(eyePos));
            case HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth);
            case ANGLE -> Comparator.comparingDouble(e -> {
                Vec3 to = e.getBoundingBox().getCenter().subtract(eyePos);
                return Math.acos(lookVec.dot(to.normalize()));
            });
            default -> Comparator.comparingDouble(e -> e.position().distanceTo(eyePos));
        };
        TARGET = entities.stream().min(comp).orElse(null);
    }

    /** 处理锁定按键：切换强制锁死状态，并发送消息 */
    public static void handleLockKeyPress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (TARGET == null) {
            mc.player.sendSystemMessage(Component.translatable("message.aim_assistant.force_lock.no_target"));
            return;
        }
        if (forceLocked && forceLockedEntity == TARGET) {
            forceLocked = false;
            forceLockedEntity = null;
            mc.player.sendSystemMessage(Component.translatable("message.aim_assistant.force_lock.unlocked"));
        } else {
            forceLocked = true;
            forceLockedEntity = TARGET;
            mc.player.sendSystemMessage(Component.translatable("message.aim_assistant.force_lock.locked", TARGET.getName().getString()));
        }
    }

    public static void nextTarget() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || TARGET == null || !AimConfig.isAllowTargetSwitching()) return;

        double range = AimConfig.getSearchRange();
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive()
        );
        entities.removeIf(e -> !passCustomFilters(player, e));
        entities.removeIf(e -> !AimConfig.isAllowWallPenetration() && !canSeeEntity(player, e));
        entities.removeIf(e -> e.distanceTo(player) > AimConfig.getLockDistance());
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getLookAngle();
        double halfFov = AimConfig.getFovAngle() / 2.0;
        entities.removeIf(e -> {
            Vec3 toEntity = e.getEyePosition(1.0f).subtract(eyePos);
            double len = toEntity.length();
            if (len == 0) return false;
            double cos = lookVec.dot(toEntity.normalize());
            return Math.toDegrees(Math.acos(cos)) > halfFov;
        });
        if (entities.size() <= 1) return;

        TargetPriority priority = AimConfig.getTargetPriority();
        if (priority != TargetPriority.RANDOM) {
            Comparator<LivingEntity> comp = switch (priority) {
                case DISTANCE -> Comparator.comparingDouble(e -> e.position().distanceTo(eyePos));
                case HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth);
                case ANGLE -> Comparator.comparingDouble(e -> {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(eyePos);
                    return Math.acos(lookVec.dot(to.normalize()));
                });
                default -> Comparator.comparingDouble(e -> e.position().distanceTo(eyePos));
            };
            entities.sort(comp);
        }
        int idx = entities.indexOf(TARGET);
        int nextIdx = (idx + 1) % entities.size();
        TARGET = entities.get(nextIdx);
    }

    private static boolean passCustomFilters(LocalPlayer player, LivingEntity entity) {
        if (AimConfig.isIgnoreInvisible() && entity.isInvisible()) return false;
        if (AimModeAdapter.isExcluded(entity)) return false;
        return AimModeAdapter.isValidForFilter(entity);
    }

    private static boolean isTargetValid(LocalPlayer player, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (target.distanceTo(player) > AimConfig.getLockDistance()) return false;
        if (!AimModeAdapter.isValidForFilter(target)) return false;
        if (AimModeAdapter.isExcluded(target)) return false;
        if (AimConfig.isIgnoreInvisible() && target.isInvisible()) return false;
        if (!AimConfig.isAllowWallPenetration() && !canSeeEntity(player, target)) return false;
        return true;
    }

    /** 强锁目标宽松检查：忽略穿墙限制，只要目标存活、在距离内且未被过滤即可 */
    private static boolean isTargetValidForForceLock(LocalPlayer player, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (target.distanceTo(player) > AimConfig.getLockDistance()) return false;
        if (!AimModeAdapter.isValidForFilter(target)) return false;
        if (AimModeAdapter.isExcluded(target)) return false;
        if (AimConfig.isIgnoreInvisible() && target.isInvisible()) return false;
        // 不强检查穿墙
        return true;
    }

    private static void lockTarget(LocalPlayer player, float smooth) {
        if (TARGET != null) {
            LockPart part = AimConfig.getLockOnPart();
            Vec3 targetPos = switch (part) {
                case HEAD -> new Vec3(TARGET.getX(), TARGET.getEyeY(), TARGET.getZ());
                case BODY -> TARGET.getBoundingBox().getCenter();
                case FEET -> new Vec3(TARGET.getX(), TARGET.getY(), TARGET.getZ());
            };
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 dir = targetPos.subtract(eyePos).normalize();
            double dxz = Math.hypot(dir.x, dir.z);
            float targetYaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
            float targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, dxz));
            float curYaw = player.getYRot();
            while (targetYaw - curYaw > 180) targetYaw -= 360;
            while (targetYaw - curYaw < -180) targetYaw += 360;
            player.setYRot(curYaw + (targetYaw - curYaw) * smooth);
            player.setXRot(player.getXRot() + (targetPitch - player.getXRot()) * smooth);
        }
    }

    private static boolean canSeeEntity(Player player, LivingEntity target) {
        Vec3 pe = player.getEyePosition(1.0f);
        Vec3 te = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        HitResult hit = player.level().clip(new ClipContext(pe, te, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static boolean isHoldingActivationItem(LocalPlayer player) {
        String itemSetting = AimConfig.getActivationItem().trim();
        if (itemSetting.isEmpty()) return true;
        if (itemSetting.startsWith("#")) {
            ResourceLocation tagName = ResourceLocation.tryParse(itemSetting.substring(1));
            if (tagName != null) {
                TagKey<net.minecraft.world.item.Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagName);
                return player.getMainHandItem().is(tag);
            }
            return false;
        } else {
            ResourceLocation itemId = ResourceLocation.tryParse(itemSetting);
            if (itemId != null) {
                return player.getMainHandItem().is(ForgeRegistries.ITEMS.getValue(itemId));
            }
            return false;
        }
    }
}
