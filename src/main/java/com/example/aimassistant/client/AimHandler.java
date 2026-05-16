package com.example.aimassistant.client;

import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static com.example.aimassistant.key.ModKeyMapping.AIM_BUTTON;

@Mod.EventBusSubscriber(modid = "aim_assistant", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class AimHandler {
    public static volatile LivingEntity TARGET = null;
    private static boolean wasPressed = false;
    private static boolean aimToggled = false;

    @SubscribeEvent
    public static void aimBot(RenderGameOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.level == null) return;

        KeyMode keyMode = AimConfig.getAimKeyMode();
        boolean keyDown = AIM_BUTTON.isDown();
        boolean activated = (keyMode == KeyMode.HOLD && keyDown) || (keyMode == KeyMode.TOGGLE && aimToggled);

        if (keyMode == KeyMode.TOGGLE) {
            if (keyDown && !wasPressed) aimToggled = !aimToggled;
            wasPressed = keyDown;
        }

        if (activated) {
            searchTarget(player);
            lockTarget(player, (float) AimConfig.getSmoothFactor());

            if (AimConfig.isAutoAttack() && TARGET != null) {
                if (player.getAttackStrengthScale(0.0f) >= 1.0f &&
                    player.distanceTo(TARGET) <= mc.gameMode.getPickRange()) {
                    mc.gameMode.attack(player, TARGET);
                    player.swing(Hand.MAIN_HAND);
                }
            }
        } else {
            TARGET = null;
        }
    }

    private static boolean isTargetValid(LivingEntity target, ClientPlayerEntity player) {
        if (target == null || !target.isAlive()) return false;
        double dist = player.distanceTo(target);
        return dist <= AimConfig.getMaxLockDistance();
    }

    private static void searchTarget(ClientPlayerEntity player) {
        // 目标无效时才强制重新搜索，否则保持当前锁定（稳定锁定，避免每帧切换）
        if (TARGET != null && !isTargetValid(TARGET, player)) {
            TARGET = null;
        }
        // 仅在无目标或允许切换且目标无效时才搜索新目标
        if (TARGET == null) {
            double range = AimConfig.getSearchRange();
            double maxDist = AimConfig.getMaxLockDistance();
            List<LivingEntity> entities = player.level.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(range),
                    e -> e != player && e.isAlive()
            );
            entities.removeIf(e -> !passCustomFilters(player, e));
            entities.removeIf(e -> !AimConfig.isAllowWallPenetration() && !canSeeEntity(player, e));
            entities.removeIf(e -> player.distanceTo(e) > maxDist);

            if (entities.isEmpty()) {
                TARGET = null;
                return;
            }

            Vector3d eyePos = player.getEyePosition(1.0f);
            Vector3d lookVec = player.getLookAngle();
            TargetPriority priority = AimConfig.getTargetPriority();
            Comparator<LivingEntity> comp;
            switch (priority) {
                case DISTANCE:
                    comp = Comparator.comparingDouble(e -> e.position().distanceTo(eyePos));
                    break;
                case HEALTH:
                    comp = Comparator.comparingDouble(LivingEntity::getHealth);
                    break;
                case ANGLE:
                    comp = Comparator.comparingDouble(e -> {
                        Vector3d to = e.getBoundingBox().getCenter().subtract(eyePos);
                        return Math.acos(lookVec.dot(to.normalize()));
                    });
                    break;
                case RANDOM:
                default:
                    comp = (a, b) -> new Random().nextInt(3) - 1;
                    break;
            }
            TARGET = entities.stream().min(comp).orElse(null);
        }
    }

    private static boolean passCustomFilters(ClientPlayerEntity player, LivingEntity entity) {
        if (AimConfig.isIgnoreInvisible() && entity.isInvisible()) return false;
        if (AimModeAdapter.isExcluded(entity)) return false;
        return AimModeAdapter.isValidForFilter(entity);
    }

    private static void lockTarget(ClientPlayerEntity player, float smooth) {
        if (TARGET == null || !isTargetValid(TARGET, player)) return;
        LockPart part = AimConfig.getLockOnPart();
        Vector3d targetPos;
        switch (part) {
            case HEAD:
                targetPos = new Vector3d(TARGET.getX(), TARGET.getEyeY(), TARGET.getZ());
                break;
            case BODY:
                targetPos = TARGET.getBoundingBox().getCenter();
                break;
            case FEET:
            default:
                targetPos = new Vector3d(TARGET.getX(), TARGET.getY(), TARGET.getZ());
                break;
        }
        Vector3d eyePos = player.getEyePosition(1.0f);
        Vector3d dir = targetPos.subtract(eyePos).normalize();
        double dxz = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, dxz));

        float curYaw = player.yRot;
        float curPitch = player.xRot;

        // 角度差规范化到 [-180, 180]
        float deltaYaw = targetYaw - curYaw;
        while (deltaYaw > 180) deltaYaw -= 360;
        while (deltaYaw < -180) deltaYaw += 360;
        float deltaPitch = Math.max(-90, Math.min(90, targetPitch - curPitch));

        // 平滑旋转（每帧只移动差值的一定比例）
        float newYaw = curYaw + deltaYaw * smooth;
        float newPitch = curPitch + deltaPitch * smooth;

        // 只更新当前朝向，让游戏自动保留上一帧的 yRotO 用于渲染插值
        player.yRot = newYaw;
        player.xRot = newPitch;
        // 注意：不要设置 yRotO / xRotO，否则平滑会失效！
    }

    private static boolean canSeeEntity(PlayerEntity player, LivingEntity target) {
        Vector3d pe = player.getEyePosition(1.0f);
        Vector3d te = new Vector3d(target.getX(), target.getEyeY(), target.getZ());
        RayTraceContext ctx = new RayTraceContext(pe, te, RayTraceContext.BlockMode.VISUAL, RayTraceContext.FluidMode.NONE, player);
        RayTraceResult hit = player.level.clip(ctx);
        return hit.getType() == RayTraceResult.Type.MISS;
    }
}