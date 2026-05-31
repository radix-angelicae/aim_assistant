package com.example.aimassistant.client;

import com.example.aimassistant.AimAssistant;
import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigScreen extends Screen {

    private enum Tab {
        GENERAL("tab.aim_assistant.general"),
        FILTER("tab.aim_assistant.filter"),
        RENDERING("tab.aim_assistant.rendering"),
        GLOW("tab.aim_assistant.glow"),
        UI("tab.aim_assistant.ui"),
        EXCLUSION("tab.aim_assistant.exclusion"),
        SCREEN_RENDER("tab.aim_assistant.screen_render");

        final String translationKey;
        Tab(String key) { this.translationKey = key; }
    }

    private Tab currentTab = Tab.GENERAL; // 默认改为常规
    private final List<ColorPreview> colorPreviews = new ArrayList<>();
    private final Map<EditBox, ForgeConfigSpec.IntValue> intInputBindings = new HashMap<>();
    private final Map<EditBox, ForgeConfigSpec.DoubleValue> doubleInputBindings = new HashMap<>();
    private int contentScrollY = 0;
    private int sidebarScrollY = 0;
    private int contentHeight = 0;
    private int sidebarTotalHeight = 0;

    private final Map<ForgeConfigSpec.IntValue, String> intCache = new HashMap<>();
    private final Map<ForgeConfigSpec.DoubleValue, String> doubleCache = new HashMap<>();

    private EditBox exclusionInputField;
    private int selectedExclusionIndex = -1;
    private final List<String> exclusionItemCache = new ArrayList<>();
    private String lastExclusionInput = "";
    private boolean suppressExclusionRefresh = false;

    private EditBox searchInputField;
    private int selectedSearchIndex = -1;
    private final List<String> searchItemCache = new ArrayList<>();
    private String lastSearchInput = "";
    private boolean suppressSearchRefresh = false;

    public static boolean showRangeHelper = false;

    private static List<String> allEntityIdsCache = null;

    private static final int SIDEBAR_WIDTH = 70;
    private static final int LABEL_WIDTH = 110;
    private static final int TOP_OFFSET = 30;
    private static final int BOTTOM_RESERVED = 35;
    private static final int SUGGESTION_MAX = 5;

    public ConfigScreen() {
        super(Component.translatable("screen.aim_assistant.config"));
    }

    @Override
    protected void init() {
        for (var entry : intInputBindings.entrySet()) intCache.put(entry.getValue(), entry.getKey().getValue());
        for (var entry : doubleInputBindings.entrySet()) doubleCache.put(entry.getValue(), entry.getKey().getValue());

        suppressExclusionRefresh = true;
        suppressSearchRefresh = true;
        clearWidgets();
        colorPreviews.clear();
        intInputBindings.clear();
        doubleInputBindings.clear();

        sidebarTotalHeight = Tab.values().length * 25 + 10;
        sidebarScrollY = clamp(sidebarScrollY, 0, Math.max(0, sidebarTotalHeight - (this.height - 60)));

        int y = TOP_OFFSET - sidebarScrollY;
        for (Tab tab : Tab.values()) {
            Button btn = Button.builder(Component.translatable(tab.translationKey), b -> {
                if (currentTab != tab) {
                    currentTab = tab;
                    contentScrollY = 0;
                    if (tab != Tab.EXCLUSION) selectedExclusionIndex = -1;
                    if (tab != Tab.FILTER) selectedSearchIndex = -1;
                    init();
                }
            }).pos(5, y).size(SIDEBAR_WIDTH - 5, 20).build();
            if (tab == currentTab) btn.active = false;
            addRenderableWidget(btn);
            y += 25;
        }

        int contentX = SIDEBAR_WIDTH + 20;
        int yContent = TOP_OFFSET - contentScrollY;
        switch (currentTab) {
            case GENERAL -> yContent = buildGeneral(contentX, yContent);
            case FILTER -> yContent = buildFilter(contentX, yContent);
            case RENDERING -> yContent = buildRendering(contentX, yContent);
            case GLOW -> yContent = buildGlow(contentX, yContent);
            case UI -> yContent = buildUI(contentX, yContent);
            case EXCLUSION -> yContent = buildExclusion(contentX, yContent);
            case SCREEN_RENDER -> yContent = buildScreenRender(contentX, yContent);
        }
        contentHeight = yContent - (TOP_OFFSET - contentScrollY);
        int visibleHeight = this.height - TOP_OFFSET - BOTTOM_RESERVED;
        contentScrollY = clamp(contentScrollY, 0, Math.max(0, contentHeight - visibleHeight));

        addRenderableWidget(Button.builder(Component.translatable("config.aim_assistant.save_close"), btn -> saveAndClose())
                .pos(width / 2 - 50, height - 30).size(100, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable(showRangeHelper ? "config.aim_assistant.hide_range" : "config.aim_assistant.show_range"),
                btn -> { showRangeHelper = !showRangeHelper; init(); }
        ).pos(width - 90, 5).size(80, 20).build());

        if (currentTab == Tab.EXCLUSION && exclusionInputField != null) {
            setFocused(exclusionInputField);
            exclusionInputField.setFocused(true);
            exclusionInputField.moveCursorToEnd();
        }
        if (currentTab == Tab.FILTER && searchInputField != null) {
            setFocused(searchInputField);
            searchInputField.setFocused(true);
            searchInputField.moveCursorToEnd();
        }

        suppressExclusionRefresh = false;
        suppressSearchRefresh = false;
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(v, max)); }

    private void addSectionTitle(int x, int y, String key) {
        addRenderableWidget(new StringWidget(x, y, 200, 12, Component.translatable(key), font));
    }

    private int addToggle(int x, int y, String key, ForgeConfigSpec.BooleanValue config) {
        String state = config.get() ? "§aON" : "§cOFF";
        addRenderableWidget(Button.builder(
                Component.translatable(key).append(": " + state),
                btn -> {
                    config.set(!config.get());
                    btn.setMessage(Component.translatable(key).append(": " + (config.get() ? "§aON" : "§cOFF")));
                }
        ).pos(x, y).size(170, 20).build());
        return y + 22;
    }

    private int addIntInput(int x, int y, String key, ForgeConfigSpec.IntValue config, int min, int max) {
        addRenderableWidget(new StringWidget(x, y, LABEL_WIDTH, 12, Component.translatable(key), font));
        String cached = intCache.getOrDefault(config, String.valueOf(config.get()));
        EditBox field = new EditBox(font, x + LABEL_WIDTH + 10, y, 60, 16, Component.empty());
        field.setValue(cached);
        field.setFilter(s -> s.matches("\\d*"));
        addRenderableWidget(field);
        intInputBindings.put(field, config);
        return y + 20;
    }

    private int addDoubleInput(int x, int y, String key, ForgeConfigSpec.DoubleValue config, double min, double max) {
        addRenderableWidget(new StringWidget(x, y, LABEL_WIDTH, 12, Component.translatable(key), font));
        String cached = doubleCache.getOrDefault(config, String.format("%.2f", config.get()));
        EditBox field = new EditBox(font, x + LABEL_WIDTH + 10, y, 60, 16, Component.empty());
        field.setValue(cached);
        field.setFilter(s -> s.matches("\\d*\\.?\\d*"));
        addRenderableWidget(field);
        doubleInputBindings.put(field, config);
        return y + 20;
    }

    private int addStringInput(int x, int y, String key, ForgeConfigSpec.ConfigValue<String> config) {
        addRenderableWidget(new StringWidget(x, y, LABEL_WIDTH, 12, Component.translatable(key), font));
        EditBox field = new EditBox(font, x + LABEL_WIDTH + 10, y, 160, 16, Component.empty());
        field.setValue(config.get());
        field.setResponder(s -> config.set(s));
        addRenderableWidget(field);
        return y + 20;
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> int addEnumSelector(int x, int y, String key, ForgeConfigSpec.EnumValue<T> config, Class<T> clazz) {
        addRenderableWidget(new StringWidget(x, y, LABEL_WIDTH, 12, Component.translatable(key), font));
        T[] values = clazz.getEnumConstants();
        int btnX = x + LABEL_WIDTH + 10;
        int btnWidth = 35;
        for (T val : values) {
            boolean selected = config.get() == val;
            String enumKey = "enum.aim_assistant." + clazz.getSimpleName() + "." + val.name();
            Component name = Component.translatable(enumKey);
            Button btn = Button.builder(name, b -> {
                config.set(val);
                init();
            }).pos(btnX, y).size(btnWidth, 20).build();
            if (selected) {
                btn.active = false;
                btn.setFGColor(0xFFFF00);
            }
            addRenderableWidget(btn);
            btnX += btnWidth + 2;
        }
        return y + 22;
    }

    private int addColorInput(int x, int y, String key, ForgeConfigSpec.IntValue config) {
        addRenderableWidget(new StringWidget(x, y, LABEL_WIDTH, 12, Component.translatable(key), font));
        int rgb = config.get() & 0xFFFFFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        EditBox rField = new EditBox(font, x + LABEL_WIDTH + 10, y, 24, 16, Component.empty());
        rField.setValue(String.valueOf(r));
        rField.setFilter(s -> s.matches("\\d{0,3}"));
        EditBox gField = new EditBox(font, x + LABEL_WIDTH + 38, y, 24, 16, Component.empty());
        gField.setValue(String.valueOf(g));
        gField.setFilter(s -> s.matches("\\d{0,3}"));
        EditBox bField = new EditBox(font, x + LABEL_WIDTH + 66, y, 24, 16, Component.empty());
        bField.setValue(String.valueOf(b));
        bField.setFilter(s -> s.matches("\\d{0,3}"));

        Runnable updateColor = () -> {
            try {
                int newR = Integer.parseInt(rField.getValue());
                int newG = Integer.parseInt(gField.getValue());
                int newB = Integer.parseInt(bField.getValue());
                config.set(((newR & 0xFF) << 16) | ((newG & 0xFF) << 8) | (newB & 0xFF));
            } catch (NumberFormatException ignored) {}
        };
        rField.setResponder(s -> updateColor.run());
        gField.setResponder(s -> updateColor.run());
        bField.setResponder(s -> updateColor.run());

        addRenderableWidget(rField);
        addRenderableWidget(gField);
        addRenderableWidget(bField);
        colorPreviews.add(new ColorPreview(() -> config.get() | 0xFF000000, x + LABEL_WIDTH + 95, y, 18, 18));
        return y + 20;
    }

    private int buildGeneral(int x, int y) {
        addSectionTitle(x, y, "config.aim_assistant.general"); y += 20;
        y = addToggle(x, y, "config.aim_assistant.wall_penetration", AimConfig.ALLOW_WALL_PENETRATION);
        y = addToggle(x, y, "config.aim_assistant.target_switching", AimConfig.ALLOW_TARGET_SWITCHING);
        y = addEnumSelector(x, y, "config.aim_assistant.target_priority", AimConfig.TARGET_PRIORITY, TargetPriority.class);
        y = addEnumSelector(x, y, "config.aim_assistant.lock_part", AimConfig.LOCK_ON_PART, LockPart.class);
        y = addEnumSelector(x, y, "config.aim_assistant.key_mode", AimConfig.AIM_KEY_MODE, KeyMode.class);
        y = addIntInput(x, y, "config.aim_assistant.search_range", AimConfig.SEARCH_RANGE, 1, 256);
        y = addDoubleInput(x, y, "config.aim_assistant.lock_distance", AimConfig.LOCK_DISTANCE, 1, 256);
        y = addDoubleInput(x, y, "config.aim_assistant.fov_angle", AimConfig.FOV_ANGLE, 0, 180);
        y = addDoubleInput(x, y, "config.aim_assistant.distance_weight", AimConfig.DISTANCE_WEIGHT, 0, 1);
        y = addDoubleInput(x, y, "config.aim_assistant.smooth_factor", AimConfig.SMOOTH_FACTOR, 0.01, 1);
        y = addToggle(x, y, "config.aim_assistant.auto_attack", AimConfig.AUTO_ATTACK);
        y = addToggle(x, y, "config.aim_assistant.ignore_invisible", AimConfig.IGNORE_INVISIBLE);
        y = addToggle(x, y, "config.aim_assistant.lock_sound", AimConfig.LOCK_SOUND_ENABLED);
        if (AimConfig.LOCK_SOUND_ENABLED.get()) y = addDoubleInput(x + 20, y, "config.aim_assistant.sound_volume", AimConfig.LOCK_SOUND_VOLUME, 0, 1);
        y = addToggle(x, y, "config.aim_assistant.ignore_teammates", AimConfig.IGNORE_TEAM_MATES);
        y = addToggle(x, y, "config.aim_assistant.ignore_tamed_pets", AimConfig.IGNORE_TAMED_PETS);
        y = addToggle(x, y, "config.aim_assistant.conditional_activation", AimConfig.CONDITIONAL_ACTIVATION);
        if (AimConfig.CONDITIONAL_ACTIVATION.get()) y = addStringInput(x + 20, y, "config.aim_assistant.activation_item", AimConfig.ACTIVATION_ITEM);
        y = addToggle(x, y, "config.aim_assistant.smart_hostile_detection", AimConfig.USE_SMART_HOSTILE_DETECTION);
        return y + 10;
    }

    private int buildFilter(int x, int y) {
        addSectionTitle(x, y, "config.aim_assistant.filter"); y += 20;
        y = addEnumSelector(x, y, "config.aim_assistant.filter_mode", AimConfig.FILTER_MODE, FilterMode.class);
        y += 4;
        addSectionTitle(x, y, "config.aim_assistant.search_list"); y += 16;
        addRenderableWidget(new StringWidget(x, y, 250, 12, Component.translatable("config.aim_assistant.list_hint"), font)); y += 16;

        searchInputField = new EditBox(font, x, y, 160, 16, Component.empty());
        searchInputField.setMaxLength(100);
        searchInputField.setValue(lastSearchInput);
        searchInputField.setResponder(s -> { lastSearchInput = s; if (!suppressSearchRefresh) init(); });
        addRenderableWidget(searchInputField);

        Button addBtn = Button.builder(Component.translatable("config.aim_assistant.add"), btn -> {
            String text = searchInputField.getValue().trim();
            if (!text.isEmpty()) {
                List<String> newList = new ArrayList<>(AimConfig.SEARCH_LIST.get());
                if (!newList.contains(text)) { newList.add(text); AimConfig.SEARCH_LIST.set(newList); lastSearchInput = ""; selectedSearchIndex = -1; init(); }
            }
        }).pos(x + 165, y).size(40, 20).build();
        addRenderableWidget(addBtn);
        Button delBtn = Button.builder(Component.translatable("config.aim_assistant.delete"), btn -> {
            if (selectedSearchIndex >= 0 && selectedSearchIndex < AimConfig.SEARCH_LIST.get().size()) {
                List<String> newList = new ArrayList<>(AimConfig.SEARCH_LIST.get()); newList.remove(selectedSearchIndex); AimConfig.SEARCH_LIST.set(newList); selectedSearchIndex = -1; init();
            }
        }).pos(x + 210, y).size(40, 20).build();
        delBtn.active = selectedSearchIndex >= 0;
        addRenderableWidget(delBtn);
        y += 22;

        String inputText = lastSearchInput.trim();
        if (!inputText.isEmpty()) {
            List<String> suggestions = getFilteredEntityIds(inputText);
            int maxS = Math.min(suggestions.size(), SUGGESTION_MAX);
            for (int i = 0; i < maxS; i++) {
                String id = suggestions.get(i);
                Button sugBtn = Button.builder(Component.literal("§7" + id), btn -> { lastSearchInput = id; init(); }).pos(x, y).size(300, 20).build();
                addRenderableWidget(sugBtn);
                y += 22;
            }
            if (suggestions.size() > SUGGESTION_MAX) {
                addRenderableWidget(new StringWidget(x, y, 100, 12, Component.translatable("config.aim_assistant.more_suggestions", suggestions.size() - SUGGESTION_MAX), font));
                y += 14;
            }
        }
        y += 4;

        List<? extends String> currentList = AimConfig.SEARCH_LIST.get();
        searchItemCache.clear();
        searchItemCache.addAll(currentList.stream().map(String::valueOf).toList());
        for (int i = 0; i < searchItemCache.size(); i++) {
            final int idx = i;
            Button itemBtn = Button.builder(Component.literal(searchItemCache.get(i)), btn -> { selectedSearchIndex = idx; init(); }).pos(x, y).size(300, 20).build();
            if (idx == selectedSearchIndex) itemBtn.setFGColor(0xFFFF00);
            addRenderableWidget(itemBtn);
            y += 22;
        }
        if (searchItemCache.isEmpty() && inputText.isEmpty()) {
            addRenderableWidget(new StringWidget(x, y, 200, 12, Component.translatable("config.aim_assistant.empty_list"), font));
            y += 14;
        }
        return y + 10;
    }

    private int buildExclusion(int x, int y) {
        addSectionTitle(x, y, "config.aim_assistant.exclusion"); y += 20;
        addRenderableWidget(new StringWidget(x, y, 250, 12, Component.translatable("config.aim_assistant.list_hint"), font)); y += 16;

        exclusionInputField = new EditBox(font, x, y, 160, 16, Component.empty());
        exclusionInputField.setMaxLength(100);
        exclusionInputField.setValue(lastExclusionInput);
        exclusionInputField.setResponder(s -> { lastExclusionInput = s; if (!suppressExclusionRefresh) init(); });
        addRenderableWidget(exclusionInputField);

        Button addBtn = Button.builder(Component.translatable("config.aim_assistant.add"), btn -> {
            String text = exclusionInputField.getValue().trim();
            if (!text.isEmpty()) {
                List<String> newList = new ArrayList<>(AimConfig.EXCLUSION_LIST.get());
                if (!newList.contains(text)) { newList.add(text); AimConfig.EXCLUSION_LIST.set(newList); lastExclusionInput = ""; selectedExclusionIndex = -1; init(); }
            }
        }).pos(x + 165, y).size(40, 20).build();
        addRenderableWidget(addBtn);
        Button delBtn = Button.builder(Component.translatable("config.aim_assistant.delete"), btn -> {
            if (selectedExclusionIndex >= 0 && selectedExclusionIndex < AimConfig.EXCLUSION_LIST.get().size()) {
                List<String> newList = new ArrayList<>(AimConfig.EXCLUSION_LIST.get()); newList.remove(selectedExclusionIndex); AimConfig.EXCLUSION_LIST.set(newList); selectedExclusionIndex = -1; init();
            }
        }).pos(x + 210, y).size(40, 20).build();
        delBtn.active = selectedExclusionIndex >= 0;
        addRenderableWidget(delBtn);
        y += 22;

        String inputText = lastExclusionInput.trim();
        if (!inputText.isEmpty()) {
            List<String> suggestions = getFilteredEntityIds(inputText);
            int maxS = Math.min(suggestions.size(), SUGGESTION_MAX);
            for (int i = 0; i < maxS; i++) {
                String id = suggestions.get(i);
                Button sugBtn = Button.builder(Component.literal("§7" + id), btn -> { lastExclusionInput = id; init(); }).pos(x, y).size(300, 20).build();
                addRenderableWidget(sugBtn);
                y += 22;
            }
            if (suggestions.size() > SUGGESTION_MAX) {
                addRenderableWidget(new StringWidget(x, y, 100, 12, Component.translatable("config.aim_assistant.more_suggestions", suggestions.size() - SUGGESTION_MAX), font));
                y += 14;
            }
        }
        y += 4;

        List<? extends String> currentList = AimConfig.EXCLUSION_LIST.get();
        exclusionItemCache.clear();
        exclusionItemCache.addAll(currentList.stream().map(String::valueOf).toList());
        for (int i = 0; i < exclusionItemCache.size(); i++) {
            final int idx = i;
            Button itemBtn = Button.builder(Component.literal(exclusionItemCache.get(i)), btn -> { selectedExclusionIndex = idx; init(); }).pos(x, y).size(300, 20).build();
            if (idx == selectedExclusionIndex) itemBtn.setFGColor(0xFFFF00);
            addRenderableWidget(itemBtn);
            y += 22;
        }
        if (exclusionItemCache.isEmpty() && inputText.isEmpty()) {
            addRenderableWidget(new StringWidget(x, y, 200, 12, Component.translatable("config.aim_assistant.empty_list"), font));
            y += 14;
        }
        return y + 10;
    }

    private int buildRendering(int x, int y) {
        addSectionTitle(x, y, "config.aim_assistant.rendering"); y += 20;
        y = addToggle(x, y, "config.aim_assistant.enable_rendering", AimConfig.ENABLE_ENTITY_RENDERING);
        y = addToggle(x, y, "config.aim_assistant.show_lines", AimConfig.ENABLE_LINE_RENDERING);
        y = addToggle(x, y, "config.aim_assistant.show_boxes", AimConfig.ENABLE_BOX_RENDERING);
        y = addToggle(x, y, "config.aim_assistant.show_text", AimConfig.ENABLE_TEXT_RENDERING);
        y = addDoubleInput(x, y, "config.aim_assistant.line_width", AimConfig.LINE_WIDTH, 1, 10);
        y = addDoubleInput(x, y, "config.aim_assistant.crosshair_dist", AimConfig.CROSSHAIR_DISTANCE, 1, 20);
        y = addEnumSelector(x, y, "config.aim_assistant.box_mode", AimConfig.BOX_MODE, BoxMode.class);
        y = addDoubleInput(x, y, "config.aim_assistant.box_scale", AimConfig.BOX_SCALE, 0.5, 2.0);

        int colY = y + 5;
        y = addColorInput(x, colY, "config.aim_assistant.line_player", AimConfig.LINE_COLOR_PLAYER);
        int tmpY = addColorInput(x + 200, colY, "config.aim_assistant.line_monster", AimConfig.LINE_COLOR_MONSTER);
        y = Math.max(y, tmpY) + 5;
        tmpY = addColorInput(x, y, "config.aim_assistant.line_animal", AimConfig.LINE_COLOR_ANIMAL);
        int tmpY2 = addColorInput(x + 200, y, "config.aim_assistant.line_other", AimConfig.LINE_COLOR_OTHER);
        y = Math.max(tmpY, tmpY2) + 5;
        y = addColorInput(x, y, "config.aim_assistant.line_search", AimConfig.LINE_COLOR_SEARCH);
        y += 4;

        colY = y + 5;
        y = addColorInput(x, colY, "config.aim_assistant.box_player", AimConfig.BOX_COLOR_PLAYER);
        tmpY = addColorInput(x + 200, colY, "config.aim_assistant.box_monster", AimConfig.BOX_COLOR_MONSTER);
        y = Math.max(y, tmpY) + 5;
        tmpY = addColorInput(x, y, "config.aim_assistant.box_animal", AimConfig.BOX_COLOR_ANIMAL);
        tmpY2 = addColorInput(x + 200, y, "config.aim_assistant.box_other", AimConfig.BOX_COLOR_OTHER);
        y = Math.max(tmpY, tmpY2) + 5;
        y = addColorInput(x, y, "config.aim_assistant.box_search", AimConfig.BOX_COLOR_SEARCH);
        y += 4;

        y = addDoubleInput(x, y, "config.aim_assistant.box_alpha", AimConfig.BOX_ALPHA, 0, 1);
        y = addToggle(x, y, "config.aim_assistant.text_name", AimConfig.TEXT_SHOW_NAME);
        y = addToggle(x, y, "config.aim_assistant.text_health", AimConfig.TEXT_SHOW_HEALTH);
        y = addToggle(x, y, "config.aim_assistant.text_armor", AimConfig.TEXT_SHOW_ARMOR);
        y = addToggle(x, y, "config.aim_assistant.text_distance", AimConfig.TEXT_SHOW_DISTANCE);
        y = addToggle(x, y, "config.aim_assistant.text_type", AimConfig.TEXT_SHOW_ENTITY_TYPE);
        y = addColorInput(x, y, "config.aim_assistant.text_color", AimConfig.TEXT_COLOR);
        y = addDoubleInput(x, y, "config.aim_assistant.text_scale", AimConfig.TEXT_SCALE, 0.01, 0.1);
        y = addToggle(x, y, "config.aim_assistant.text_background", AimConfig.TEXT_BACKGROUND);
        y = addToggle(x, y, "config.aim_assistant.text_shadow", AimConfig.TEXT_SHADOW);
        y = addDoubleInput(x, y, "config.aim_assistant.text_max_dist", AimConfig.TEXT_MAX_DISTANCE, 1, 256);
        y = addIntInput(x, y, "config.aim_assistant.max_entities", AimConfig.RENDER_MAX_ENTITIES, 1, 500);
        y = addToggle(x, y, "config.aim_assistant.label_follow_filter", AimConfig.TEXT_FOLLOW_FILTER);
        return y + 10;
    }

    private int buildGlow(int x, int y) {
        addSectionTitle(x, y, "config.aim_assistant.glow"); y += 20;
        y = addToggle(x, y, "config.aim_assistant.glow_target", AimConfig.GLOW_TARGET);
        if (AimConfig.GLOW_TARGET.get()) {
            y = addColorInput(x + 20, y, "config.aim_assistant.glow_color", AimConfig.GLOW_COLOR);
            y = addColorInput(x + 20, y, "config.aim_assistant.force_lock_glow_color", AimConfig.FORCE_LOCK_GLOW_COLOR);
            y = addDoubleInput(x + 20, y, "config.aim_assistant.force_lock_line_width", AimConfig.FORCE_LOCK_LINE_WIDTH, 1.0, 10.0);
        }
        return y + 10;
    }

    private int buildUI(int x, int y) {
        addSectionTitle(x, y, "config.aim_assistant.ui"); y += 20;
        y = addToggle(x, y, "config.aim_assistant.show_aim_info", AimConfig.SHOW_AIM_INFO);
        y = addToggle(x, y, "config.aim_assistant.show_aim_panel", AimConfig.SHOW_AIM_PANEL);
        y = addDoubleInput(x, y, "config.aim_assistant.info_x", AimConfig.AIM_INFO_X_PERCENT, 0, 100);
        y = addDoubleInput(x, y, "config.aim_assistant.info_y", AimConfig.AIM_INFO_Y_PERCENT, 0, 100);
        if (AimConfig.SHOW_AIM_PANEL.get()) {
            y = addToggle(x + 20, y, "config.aim_assistant.panel_name", AimConfig.AIM_PANEL_SHOW_NAME);
            y = addToggle(x + 20, y, "config.aim_assistant.panel_health", AimConfig.AIM_PANEL_SHOW_HEALTH);
            y = addToggle(x + 20, y, "config.aim_assistant.panel_max_health", AimConfig.AIM_PANEL_SHOW_MAX_HEALTH);
            y = addToggle(x + 20, y, "config.aim_assistant.panel_armor", AimConfig.AIM_PANEL_SHOW_ARMOR);
            y = addToggle(x + 20, y, "config.aim_assistant.panel_distance", AimConfig.AIM_PANEL_SHOW_DISTANCE);
            y = addToggle(x + 20, y, "config.aim_assistant.panel_type", AimConfig.AIM_PANEL_SHOW_TYPE);
            y = addToggle(x + 20, y, "config.aim_assistant.panel_effects", AimConfig.AIM_PANEL_SHOW_EFFECTS);
            y = addToggle(x + 20, y, "config.aim_assistant.panel_held_item", AimConfig.AIM_PANEL_SHOW_HELD_ITEM);
        }
        return y + 10;
    }

    private int buildScreenRender(int x, int y) {
        addSectionTitle(x, y, "config.aim_assistant.screen_render"); y += 20;
        y = addToggle(x, y, "config.aim_assistant.screen_render_mode", AimConfig.SCREEN_RENDER_MODE);
        return y + 10;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.fill(SIDEBAR_WIDTH + 5, 20, SIDEBAR_WIDTH + 6, height - 20, 0x80FFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        for (ColorPreview prev : colorPreviews) guiGraphics.fill(prev.x, prev.y, prev.x + prev.width, prev.y + prev.height, prev.color.get());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < SIDEBAR_WIDTH + 10) {
            sidebarScrollY -= (int) (delta * 10);
        } else {
            contentScrollY -= (int) (delta * 10);
        }
        init();
        return true;
    }

    private void saveAndClose() {
        for (var entry : intInputBindings.entrySet()) {
            try { entry.getValue().set(Integer.parseInt(entry.getKey().getValue())); } catch (NumberFormatException ignored) {}
        }
        for (var entry : doubleInputBindings.entrySet()) {
            try { entry.getValue().set(Double.parseDouble(entry.getKey().getValue())); } catch (NumberFormatException ignored) {}
        }
        if (AimAssistant.CLIENT_CONFIG != null) AimAssistant.CLIENT_CONFIG.save();
        showRangeHelper = false;
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { saveAndClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static class ColorPreview {
        final Supplier<Integer> color; final int x, y, width, height;
        ColorPreview(Supplier<Integer> color, int x, int y, int width, int height) { this.color = color; this.x = x; this.y = y; this.width = width; this.height = height; }
    }

    private static synchronized List<String> getAllEntityIds() {
        if (allEntityIdsCache == null) {
            allEntityIdsCache = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .collect(Collectors.toList());
        }
        return allEntityIdsCache;
    }

    private List<String> getFilteredEntityIds(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (!lower.contains("*")) {
            return getAllEntityIds().stream()
                    .filter(id -> id.startsWith(lower))
                    .collect(Collectors.toList());
        } else {
            String regex = Pattern.quote(lower).replace("*", "\\E.*\\Q");
            return getAllEntityIds().stream()
                    .filter(id -> id.matches(regex))
                    .collect(Collectors.toList());
        }
    }
}
