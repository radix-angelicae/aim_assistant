package com.example.aimassistant.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Collections;
import java.util.List;

public final class AimConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 性能
    public static final ForgeConfigSpec.IntValue SEARCH_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue DISTANCE_CULLING;
    public static final ForgeConfigSpec.DoubleValue CULL_DISTANCE;

    // 常规
    public static final ForgeConfigSpec.IntValue SEARCH_RANGE;
    public static final ForgeConfigSpec.DoubleValue FOV_ANGLE;
    public static final ForgeConfigSpec.DoubleValue DISTANCE_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue SMOOTH_FACTOR;
    public static final ForgeConfigSpec.BooleanValue ALLOW_WALL_PENETRATION;
    public static final ForgeConfigSpec.BooleanValue ALLOW_TARGET_SWITCHING;
    public static final ForgeConfigSpec.EnumValue<TargetPriority> TARGET_PRIORITY;
    public static final ForgeConfigSpec.EnumValue<LockPart> LOCK_ON_PART;
    public static final ForgeConfigSpec.EnumValue<KeyMode> AIM_KEY_MODE;
    public static final ForgeConfigSpec.BooleanValue AUTO_ATTACK;
    public static final ForgeConfigSpec.IntValue AUTO_ATTACK_DELAY;
    public static final ForgeConfigSpec.BooleanValue IGNORE_INVISIBLE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUSION_LIST;
    public static final ForgeConfigSpec.BooleanValue LOCK_SOUND_ENABLED;
    public static final ForgeConfigSpec.DoubleValue LOCK_SOUND_VOLUME;
    public static final ForgeConfigSpec.DoubleValue MAX_LOCK_DISTANCE;   // 新增

    public static final ForgeConfigSpec.EnumValue<FilterMode> FILTER_MODE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SEARCH_LIST;

    // 渲染
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENTITY_RENDERING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LINE_RENDERING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BOX_RENDERING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TEXT_RENDERING;
    public static final ForgeConfigSpec.IntValue LINE_COLOR_PLAYER;
    public static final ForgeConfigSpec.IntValue LINE_COLOR_MONSTER;
    public static final ForgeConfigSpec.IntValue LINE_COLOR_ANIMAL;
    public static final ForgeConfigSpec.IntValue LINE_COLOR_OTHER;
    public static final ForgeConfigSpec.IntValue LINE_COLOR_SEARCH;
    public static final ForgeConfigSpec.DoubleValue LINE_WIDTH;
    public static final ForgeConfigSpec.DoubleValue CROSSHAIR_DISTANCE;
    public static final ForgeConfigSpec.IntValue BOX_COLOR_PLAYER;
    public static final ForgeConfigSpec.IntValue BOX_COLOR_MONSTER;
    public static final ForgeConfigSpec.IntValue BOX_COLOR_ANIMAL;
    public static final ForgeConfigSpec.IntValue BOX_COLOR_OTHER;
    public static final ForgeConfigSpec.IntValue BOX_COLOR_SEARCH;
    public static final ForgeConfigSpec.DoubleValue BOX_ALPHA;
    public static final ForgeConfigSpec.EnumValue<BoxMode> BOX_MODE;
    public static final ForgeConfigSpec.DoubleValue BOX_SCALE;
    public static final ForgeConfigSpec.BooleanValue TEXT_SHOW_NAME;
    public static final ForgeConfigSpec.BooleanValue TEXT_SHOW_HEALTH;
    public static final ForgeConfigSpec.BooleanValue TEXT_SHOW_ARMOR;
    public static final ForgeConfigSpec.BooleanValue TEXT_SHOW_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue TEXT_SHOW_ENTITY_TYPE;
    public static final ForgeConfigSpec.IntValue TEXT_COLOR;
    public static final ForgeConfigSpec.DoubleValue TEXT_SCALE;
    public static final ForgeConfigSpec.BooleanValue TEXT_BACKGROUND;
    public static final ForgeConfigSpec.BooleanValue TEXT_SHADOW;
    public static final ForgeConfigSpec.DoubleValue TEXT_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue RENDER_MAX_ENTITIES;
    public static final ForgeConfigSpec.BooleanValue TEXT_FOLLOW_FILTER;

    public static final ForgeConfigSpec.BooleanValue GLOW_TARGET;
    public static final ForgeConfigSpec.IntValue GLOW_COLOR;

    public static final ForgeConfigSpec.BooleanValue SHOW_AIM_INFO;
    public static final ForgeConfigSpec.BooleanValue SHOW_AIM_PANEL;
    public static final ForgeConfigSpec.BooleanValue AIM_PANEL_SHOW_NAME;
    public static final ForgeConfigSpec.BooleanValue AIM_PANEL_SHOW_HEALTH;
    public static final ForgeConfigSpec.BooleanValue AIM_PANEL_SHOW_ARMOR;
    public static final ForgeConfigSpec.BooleanValue AIM_PANEL_SHOW_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue AIM_PANEL_SHOW_TYPE;
    public static final ForgeConfigSpec.DoubleValue AIM_INFO_X_PERCENT;
    public static final ForgeConfigSpec.DoubleValue AIM_INFO_Y_PERCENT;

    public enum TargetPriority { DISTANCE, ANGLE, HEALTH, RANDOM }
    public enum LockPart { HEAD, BODY, FEET }
    public enum KeyMode { HOLD, TOGGLE }
    public enum BoxMode { FULL_3D, CORNERS, BOTTOM, TOP }
    public enum FilterMode { ALL, HOSTILE, ANIMAL, SEARCH }

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.comment("Aim Assistant Config");

        BUILDER.push("performance");
        SEARCH_INTERVAL = BUILDER.comment("搜索间隔(刻)").defineInRange("searchInterval", 1, 1, 20);
        DISTANCE_CULLING = BUILDER.comment("距离裁剪").define("distanceCulling", true);
        CULL_DISTANCE = BUILDER.comment("裁剪距离").defineInRange("cullDistance", 64.0, 16.0, 256.0);
        BUILDER.pop();

        BUILDER.push("general");
        SEARCH_RANGE = BUILDER.defineInRange("searchRange", 32, 1, 256);
        FOV_ANGLE = BUILDER.defineInRange("fovAngle", 60.0, 0.0, 180.0);
        DISTANCE_WEIGHT = BUILDER.defineInRange("distanceWeight", 0.2, 0.0, 1.0);
        SMOOTH_FACTOR = BUILDER.defineInRange("smoothFactor", 0.3, 0.01, 1.0);
        ALLOW_WALL_PENETRATION = BUILDER.define("allowWallPenetration", false);
        ALLOW_TARGET_SWITCHING = BUILDER.define("allowTargetSwitching", true);
        TARGET_PRIORITY = BUILDER.defineEnum("targetPriority", TargetPriority.ANGLE);
        LOCK_ON_PART = BUILDER.defineEnum("lockOnPart", LockPart.BODY);
        AIM_KEY_MODE = BUILDER.defineEnum("aimKeyMode", KeyMode.HOLD);
        AUTO_ATTACK = BUILDER.define("autoAttack", false);
        AUTO_ATTACK_DELAY = BUILDER.defineInRange("autoAttackDelay", 10, 1, 40);
        IGNORE_INVISIBLE = BUILDER.define("ignoreInvisible", false);
        EXCLUSION_LIST = BUILDER.comment("排除列表(通配符*)").defineList("exclusionList", Collections.emptyList(), o -> o instanceof String);
        LOCK_SOUND_ENABLED = BUILDER.define("lockSoundEnabled", false);
        LOCK_SOUND_VOLUME = BUILDER.defineInRange("lockSoundVolume", 1.0, 0.0, 1.0);
        MAX_LOCK_DISTANCE = BUILDER.comment("最远锁定距离(格)").defineInRange("maxLockDistance", 64.0, 1.0, 256.0);
        BUILDER.pop();

        BUILDER.push("filter");
        FILTER_MODE = BUILDER.defineEnum("filterMode", FilterMode.HOSTILE);
        SEARCH_LIST = BUILDER.comment("检索列表(通配符*)").defineList("searchList", Collections.emptyList(), o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("rendering");
        ENABLE_ENTITY_RENDERING = BUILDER.define("enableEntityRendering", true);
        ENABLE_LINE_RENDERING = BUILDER.define("enableLineRendering", true);
        ENABLE_BOX_RENDERING = BUILDER.define("enableBoxRendering", true);
        ENABLE_TEXT_RENDERING = BUILDER.define("enableTextRendering", true);
        LINE_COLOR_PLAYER = BUILDER.defineInRange("lineColorPlayer", 0xFFFFFF, 0, 0xFFFFFF);
        LINE_COLOR_MONSTER = BUILDER.defineInRange("lineColorMonster", 0xFF0000, 0, 0xFFFFFF);
        LINE_COLOR_ANIMAL = BUILDER.defineInRange("lineColorAnimal", 0x00FF00, 0, 0xFFFFFF);
        LINE_COLOR_OTHER = BUILDER.defineInRange("lineColorOther", 0xFFFF00, 0, 0xFFFFFF);
        LINE_COLOR_SEARCH = BUILDER.defineInRange("lineColorSearch", 0x00BFFF, 0, 0xFFFFFF);
        LINE_WIDTH = BUILDER.defineInRange("lineWidth", 2.0, 1.0, 10.0);
        CROSSHAIR_DISTANCE = BUILDER.defineInRange("crosshairDistance", 5.0, 1.0, 20.0);
        BOX_COLOR_PLAYER = BUILDER.defineInRange("boxColorPlayer", 0xFFFFFF, 0, 0xFFFFFF);
        BOX_COLOR_MONSTER = BUILDER.defineInRange("boxColorMonster", 0xFF0000, 0, 0xFFFFFF);
        BOX_COLOR_ANIMAL = BUILDER.defineInRange("boxColorAnimal", 0x00FF00, 0, 0xFFFFFF);
        BOX_COLOR_OTHER = BUILDER.defineInRange("boxColorOther", 0xFFFF00, 0, 0xFFFFFF);
        BOX_COLOR_SEARCH = BUILDER.defineInRange("boxColorSearch", 0x00BFFF, 0, 0xFFFFFF);
        BOX_ALPHA = BUILDER.defineInRange("boxAlpha", 1.0, 0.0, 1.0);
        BOX_MODE = BUILDER.defineEnum("boxMode", BoxMode.FULL_3D);
        BOX_SCALE = BUILDER.defineInRange("boxScale", 1.0, 0.5, 2.0);
        TEXT_SHOW_NAME = BUILDER.define("textShowName", true);
        TEXT_SHOW_HEALTH = BUILDER.define("textShowHealth", true);
        TEXT_SHOW_ARMOR = BUILDER.define("textShowArmor", true);
        TEXT_SHOW_DISTANCE = BUILDER.define("textShowDistance", false);
        TEXT_SHOW_ENTITY_TYPE = BUILDER.define("textShowEntityType", false);
        TEXT_COLOR = BUILDER.defineInRange("textColor", 0xFFFFFF, 0, 0xFFFFFF);
        TEXT_SCALE = BUILDER.defineInRange("textScale", 0.025, 0.01, 0.1);
        TEXT_BACKGROUND = BUILDER.define("textBackground", false);
        TEXT_SHADOW = BUILDER.define("textShadow", true);
        TEXT_MAX_DISTANCE = BUILDER.defineInRange("textMaxDistance", 64.0, 1.0, 256.0);
        RENDER_MAX_ENTITIES = BUILDER.defineInRange("renderMaxEntities", 50, 1, 500);
        TEXT_FOLLOW_FILTER = BUILDER.define("textFollowFilter", false);
        BUILDER.pop();

        BUILDER.push("target_glow");
        GLOW_TARGET = BUILDER.define("glowTarget", true);
        GLOW_COLOR = BUILDER.defineInRange("glowColor", 0xFF00FF, 0, 0xFFFFFF);
        BUILDER.pop();

        BUILDER.push("ui");
        SHOW_AIM_INFO = BUILDER.define("showAimInfo", true);
        SHOW_AIM_PANEL = BUILDER.define("showAimPanel", true);
        AIM_PANEL_SHOW_NAME = BUILDER.define("aimPanelShowName", true);
        AIM_PANEL_SHOW_HEALTH = BUILDER.define("aimPanelShowHealth", true);
        AIM_PANEL_SHOW_ARMOR = BUILDER.define("aimPanelShowArmor", true);
        AIM_PANEL_SHOW_DISTANCE = BUILDER.define("aimPanelShowDistance", true);
        AIM_PANEL_SHOW_TYPE = BUILDER.define("aimPanelShowType", false);
        AIM_INFO_X_PERCENT = BUILDER.defineInRange("aimInfoXPercent", 1.0, 0.0, 100.0);
        AIM_INFO_Y_PERCENT = BUILDER.defineInRange("aimInfoYPercent", 1.0, 0.0, 100.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // Getter 方法（包括新增的 getMaxLockDistance）
    public static int getSearchInterval() { return SEARCH_INTERVAL.get(); }
    public static boolean isDistanceCulling() { return DISTANCE_CULLING.get(); }
    public static double getCullDistance() { return CULL_DISTANCE.get(); }
    public static int getSearchRange() { return SEARCH_RANGE.get(); }
    public static double getFovAngle() { return FOV_ANGLE.get(); }
    public static double getDistanceWeight() { return DISTANCE_WEIGHT.get(); }
    public static double getSmoothFactor() { return SMOOTH_FACTOR.get(); }
    public static boolean isAllowWallPenetration() { return ALLOW_WALL_PENETRATION.get(); }
    public static boolean isAllowTargetSwitching() { return ALLOW_TARGET_SWITCHING.get(); }
    public static TargetPriority getTargetPriority() { return TARGET_PRIORITY.get(); }
    public static LockPart getLockOnPart() { return LOCK_ON_PART.get(); }
    public static KeyMode getAimKeyMode() { return AIM_KEY_MODE.get(); }
    public static boolean isAutoAttack() { return AUTO_ATTACK.get(); }
    public static int getAutoAttackDelay() { return AUTO_ATTACK_DELAY.get(); }
    public static boolean isIgnoreInvisible() { return IGNORE_INVISIBLE.get(); }
    public static List<? extends String> getExclusionList() { return EXCLUSION_LIST.get(); }
    public static boolean isLockSoundEnabled() { return LOCK_SOUND_ENABLED.get(); }
    public static double getLockSoundVolume() { return LOCK_SOUND_VOLUME.get(); }
    public static double getMaxLockDistance() { return MAX_LOCK_DISTANCE.get(); }   // 新增

    public static FilterMode getFilterMode() { return FILTER_MODE.get(); }
    public static List<? extends String> getSearchList() { return SEARCH_LIST.get(); }

    public static boolean isEnableEntityRendering() { return ENABLE_ENTITY_RENDERING.get(); }
    public static boolean isEnableLineRendering() { return ENABLE_LINE_RENDERING.get(); }
    public static boolean isEnableBoxRendering() { return ENABLE_BOX_RENDERING.get(); }
    public static boolean isEnableTextRendering() { return ENABLE_TEXT_RENDERING.get(); }
    public static int getLineColorPlayer() { return LINE_COLOR_PLAYER.get(); }
    public static int getLineColorMonster() { return LINE_COLOR_MONSTER.get(); }
    public static int getLineColorAnimal() { return LINE_COLOR_ANIMAL.get(); }
    public static int getLineColorOther() { return LINE_COLOR_OTHER.get(); }
    public static int getLineColorSearch() { return LINE_COLOR_SEARCH.get(); }
    public static double getLineWidth() { return LINE_WIDTH.get(); }
    public static double getCrosshairDistance() { return CROSSHAIR_DISTANCE.get(); }
    public static int getBoxColorPlayer() { return BOX_COLOR_PLAYER.get(); }
    public static int getBoxColorMonster() { return BOX_COLOR_MONSTER.get(); }
    public static int getBoxColorAnimal() { return BOX_COLOR_ANIMAL.get(); }
    public static int getBoxColorOther() { return BOX_COLOR_OTHER.get(); }
    public static int getBoxColorSearch() { return BOX_COLOR_SEARCH.get(); }
    public static double getBoxAlpha() { return BOX_ALPHA.get(); }
    public static BoxMode getBoxMode() { return BOX_MODE.get(); }
    public static double getBoxScale() { return BOX_SCALE.get(); }
    public static boolean isTextShowName() { return TEXT_SHOW_NAME.get(); }
    public static boolean isTextShowHealth() { return TEXT_SHOW_HEALTH.get(); }
    public static boolean isTextShowArmor() { return TEXT_SHOW_ARMOR.get(); }
    public static boolean isTextShowDistance() { return TEXT_SHOW_DISTANCE.get(); }
    public static boolean isTextShowEntityType() { return TEXT_SHOW_ENTITY_TYPE.get(); }
    public static int getTextColor() { return TEXT_COLOR.get(); }
    public static double getTextScale() { return TEXT_SCALE.get(); }
    public static boolean isTextBackground() { return TEXT_BACKGROUND.get(); }
    public static boolean isTextShadow() { return TEXT_SHADOW.get(); }
    public static double getTextMaxDistance() { return TEXT_MAX_DISTANCE.get(); }
    public static int getRenderMaxEntities() { return RENDER_MAX_ENTITIES.get(); }
    public static boolean isTextFollowFilter() { return TEXT_FOLLOW_FILTER.get(); }
    public static boolean isGlowTarget() { return GLOW_TARGET.get(); }
    public static int getGlowColor() { return GLOW_COLOR.get(); }
    public static boolean isShowAimInfo() { return SHOW_AIM_INFO.get(); }
    public static boolean isShowAimPanel() { return SHOW_AIM_PANEL.get(); }
    public static boolean isAimPanelShowName() { return AIM_PANEL_SHOW_NAME.get(); }
    public static boolean isAimPanelShowHealth() { return AIM_PANEL_SHOW_HEALTH.get(); }
    public static boolean isAimPanelShowArmor() { return AIM_PANEL_SHOW_ARMOR.get(); }
    public static boolean isAimPanelShowDistance() { return AIM_PANEL_SHOW_DISTANCE.get(); }
    public static boolean isAimPanelShowType() { return AIM_PANEL_SHOW_TYPE.get(); }
    public static double getAimInfoXPercent() { return AIM_INFO_X_PERCENT.get(); }
    public static double getAimInfoYPercent() { return AIM_INFO_Y_PERCENT.get(); }
}