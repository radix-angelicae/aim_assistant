package com.example.aimassistant.client;

import com.example.aimassistant.AimAssistant;
import com.example.aimassistant.config.AimConfig;
import com.example.aimassistant.config.AimConfig.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigScreen extends Screen {

    private enum Tab {
        PERFORMANCE("性能优化"),
        GENERAL("常规设置"),
        FILTER("过滤器"),
        RENDERING("渲染设置"),
        GLOW("目标发光"),
        UI("界面"),
        EXCLUSION("排除列表");

        final String name;
        Tab(String name) { this.name = name; }
    }

    private Tab currentTab = Tab.PERFORMANCE;
    private final Map<TextFieldWidget, ForgeConfigSpec.IntValue> intBindings = new HashMap<>();
    private final Map<TextFieldWidget, ForgeConfigSpec.DoubleValue> doubleBindings = new HashMap<>();
    private int contentScrollY = 0;
    private int sidebarScrollY = 0;
    private int contentHeight = 0;
    private int sidebarTotalHeight = 0;

    private TextFieldWidget exclusionInput;
    private int selectedExclusionIdx = -1;
    private List<String> exclusionCache = new ArrayList<>();
    private String lastExclusionInput = "";

    private TextFieldWidget searchInput;
    private int selectedSearchIdx = -1;
    private List<String> searchCache = new ArrayList<>();
    private String lastSearchInput = "";

    private static List<String> allEntityIdsCache = null;

    private static final int SIDEBAR_W = 70;
    private static final int LABEL_W = 110;
    private static final int TOP_OFFSET = 30;
    private static final int BOTTOM_RESERVED = 35;
    private static final int SUGGESTION_MAX = 5;

    // 手动渲染列表
    private final List<TextFieldWidget> allFields = new ArrayList<>();
    private final List<LabelEntry> labels = new ArrayList<>();
    private final List<ColorPreview> colorPreviews = new ArrayList<>();

    private static class LabelEntry {
        final String text; final int x, y, color;
        LabelEntry(String t, int x, int y, int c) { this.text = t; this.x = x; this.y = y; this.color = c; }
    }

    private static class ColorPreview {
        final int x, y, size;
        final java.util.function.IntSupplier colorSupplier;
        ColorPreview(int x, int y, int size, java.util.function.IntSupplier supplier) {
            this.x = x; this.y = y; this.size = size; this.colorSupplier = supplier;
        }
    }

    public ConfigScreen() {
        super(new StringTextComponent("瞄准助手配置"));
    }

    @Override
    protected void init() {
        this.children.clear();
        this.buttons.clear();
        intBindings.clear();
        doubleBindings.clear();
        allFields.clear();
        labels.clear();
        colorPreviews.clear();

        sidebarTotalHeight = Tab.values().length * 25 + 10;
        sidebarScrollY = clamp(sidebarScrollY, 0, Math.max(0, sidebarTotalHeight - (this.height - 60)));

        int y = TOP_OFFSET - sidebarScrollY;
        for (Tab tab : Tab.values()) {
            Button btn = new Button(5, y, SIDEBAR_W - 5, 20, new StringTextComponent(tab.name), b -> {
                if (currentTab != tab) {
                    currentTab = tab;
                    contentScrollY = 0;
                    selectedExclusionIdx = -1;
                    selectedSearchIdx = -1;
                    lastExclusionInput = "";
                    lastSearchInput = "";
                    setFocused(null);
                    init();
                }
            });
            addButton(btn);
            y += 25;
        }

        int contentX = SIDEBAR_W + 20;
        int yContent = TOP_OFFSET - contentScrollY;
        switch (currentTab) {
            case PERFORMANCE: yContent = buildPerformance(contentX, yContent); break;
            case GENERAL: yContent = buildGeneral(contentX, yContent); break;
            case FILTER: yContent = buildFilter(contentX, yContent); break;
            case RENDERING: yContent = buildRendering(contentX, yContent); break;
            case GLOW: yContent = buildGlow(contentX, yContent); break;
            case UI: yContent = buildUI(contentX, yContent); break;
            case EXCLUSION: yContent = buildExclusion(contentX, yContent); break;
        }
        contentHeight = yContent - (TOP_OFFSET - contentScrollY);
        int visibleH = this.height - TOP_OFFSET - BOTTOM_RESERVED;
        contentScrollY = clamp(contentScrollY, 0, Math.max(0, contentHeight - visibleH));

        addButton(new Button(width / 2 - 50, height - 30, 100, 20, new StringTextComponent("保存并关闭"), b -> saveAndClose()));
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(v, max)); }

    // ---------- 各标签页 ----------
    private int buildPerformance(int x, int y) {
        addTitle(x, y, "性能优化"); y += 20;
        y = addIntInput(x, y, "搜索间隔(刻)", AimConfig.SEARCH_INTERVAL);
        y = addToggle(x, y, "距离裁剪", AimConfig.DISTANCE_CULLING);
        if (AimConfig.DISTANCE_CULLING.get()) y = addDoubleInput(x+20, y, "裁剪距离", AimConfig.CULL_DISTANCE);
        return y+10;
    }

    private int buildGeneral(int x, int y) {
        addTitle(x, y, "常规设置"); y+=20;
        y = addToggle(x, y, "允许穿墙锁定", AimConfig.ALLOW_WALL_PENETRATION);
        y = addToggle(x, y, "允许目标切换", AimConfig.ALLOW_TARGET_SWITCHING);
        y = addEnumSelector(x, y, "目标优先级", AimConfig.TARGET_PRIORITY, TargetPriority.class);
        y = addEnumSelector(x, y, "锁定部位", AimConfig.LOCK_ON_PART, LockPart.class);
        y = addEnumSelector(x, y, "瞄准按键模式", AimConfig.AIM_KEY_MODE, KeyMode.class);
        y = addIntInput(x, y, "搜索范围", AimConfig.SEARCH_RANGE);
        y = addDoubleInput(x, y, "视野角度", AimConfig.FOV_ANGLE);
        y = addDoubleInput(x, y, "距离权重", AimConfig.DISTANCE_WEIGHT);
        y = addDoubleInput(x, y, "平滑系数", AimConfig.SMOOTH_FACTOR);
        y = addDoubleInput(x, y, "最远锁定距离", AimConfig.MAX_LOCK_DISTANCE);
        y = addToggle(x, y, "自动攻击", AimConfig.AUTO_ATTACK);
        y = addToggle(x, y, "忽略隐身", AimConfig.IGNORE_INVISIBLE);
        y = addToggle(x, y, "锁定音效", AimConfig.LOCK_SOUND_ENABLED);
        if (AimConfig.LOCK_SOUND_ENABLED.get()) y = addDoubleInput(x+20, y, "音效音量", AimConfig.LOCK_SOUND_VOLUME);
        return y+10;
    }

    private int buildFilter(int x, int y) {
        addTitle(x, y, "目标过滤器"); y+=20;
        y = addEnumSelector(x, y, "过滤模式", AimConfig.FILTER_MODE, FilterMode.class);
        y+=4;
        addTitle(x, y, "检索列表（仅在检索模式下生效）"); y+=16;
        addLabel(x, y, "支持 * 通配符，例如 minecraft:zombie", 0xAAAAAA); y+=14;

        searchInput = createTextField(x, y, 160, lastSearchInput, s -> { lastSearchInput = s; init(); });

        addButton(new Button(x+165, y, 40, 20, new StringTextComponent("添加"), btn -> {
            String txt = searchInput.getValue().trim();
            if (!txt.isEmpty() && !AimConfig.SEARCH_LIST.get().contains(txt)) {
                List<String> lst = new ArrayList<>(AimConfig.SEARCH_LIST.get());
                lst.add(txt);
                AimConfig.SEARCH_LIST.set(lst);
                lastSearchInput = "";
                selectedSearchIdx = -1;
                init();
            }
        }));
        Button delSearch = new Button(x+210, y, 40, 20, new StringTextComponent("删除"), btn -> {
            if (selectedSearchIdx>=0 && selectedSearchIdx<AimConfig.SEARCH_LIST.get().size()) {
                List<String> lst = new ArrayList<>(AimConfig.SEARCH_LIST.get());
                lst.remove(selectedSearchIdx);
                AimConfig.SEARCH_LIST.set(lst);
                selectedSearchIdx = -1;
                init();
            }
        });
        delSearch.active = selectedSearchIdx >= 0;
        addButton(delSearch);
        y += 22;

        // 自动补全
        String inputText = lastSearchInput.trim();
        if (!inputText.isEmpty()) {
            List<String> sug = getFilteredEntityIds(inputText);
            int shown = Math.min(sug.size(), SUGGESTION_MAX);
            for (int i=0; i<shown; i++) {
                String id = sug.get(i);
                addButton(new Button(x, y, 300, 20, new StringTextComponent("§7" + id), b -> {
                    lastSearchInput = id;
                    init();
                }));
                y += 22;
            }
            if (sug.size() > SUGGESTION_MAX) y += 14;
        }
        y += 4;
        List<? extends String> curList = AimConfig.SEARCH_LIST.get();
        searchCache.clear();
        searchCache.addAll(curList);
        for (int i=0; i<searchCache.size(); i++) {
            final int idx = i;
            Button itemBtn = new Button(x, y, 300, 20, new StringTextComponent(searchCache.get(i)), b -> {
                selectedSearchIdx = idx;
                init();
            });
            if (idx == selectedSearchIdx) itemBtn.setFGColor(0xFFFF00);
            addButton(itemBtn);
            y += 22;
        }
        return y+10;
    }

    private int buildExclusion(int x, int y) {
        addTitle(x, y, "排除列表"); y+=20;
        addLabel(x, y, "支持 * 通配符，例如 minecraft:zombie", 0xAAAAAA); y+=14;

        exclusionInput = createTextField(x, y, 160, lastExclusionInput, s -> { lastExclusionInput = s; init(); });

        addButton(new Button(x+165, y, 40, 20, new StringTextComponent("添加"), btn -> {
            String txt = exclusionInput.getValue().trim();
            if (!txt.isEmpty() && !AimConfig.EXCLUSION_LIST.get().contains(txt)) {
                List<String> lst = new ArrayList<>(AimConfig.EXCLUSION_LIST.get());
                lst.add(txt);
                AimConfig.EXCLUSION_LIST.set(lst);
                lastExclusionInput = "";
                selectedExclusionIdx = -1;
                init();
            }
        }));
        Button delEx = new Button(x+210, y, 40, 20, new StringTextComponent("删除"), btn -> {
            if (selectedExclusionIdx>=0 && selectedExclusionIdx<AimConfig.EXCLUSION_LIST.get().size()) {
                List<String> lst = new ArrayList<>(AimConfig.EXCLUSION_LIST.get());
                lst.remove(selectedExclusionIdx);
                AimConfig.EXCLUSION_LIST.set(lst);
                selectedExclusionIdx = -1;
                init();
            }
        });
        delEx.active = selectedExclusionIdx >= 0;
        addButton(delEx);
        y += 22;

        String inputText = lastExclusionInput.trim();
        if (!inputText.isEmpty()) {
            List<String> sug = getFilteredEntityIds(inputText);
            int shown = Math.min(sug.size(), SUGGESTION_MAX);
            for (int i=0; i<shown; i++) {
                String id = sug.get(i);
                addButton(new Button(x, y, 300, 20, new StringTextComponent("§7" + id), b -> {
                    lastExclusionInput = id;
                    init();
                }));
                y += 22;
            }
            if (sug.size() > SUGGESTION_MAX) y += 14;
        }
        y += 4;
        List<? extends String> curList = AimConfig.EXCLUSION_LIST.get();
        exclusionCache.clear();
        exclusionCache.addAll(curList);
        for (int i=0; i<exclusionCache.size(); i++) {
            final int idx = i;
            Button itemBtn = new Button(x, y, 300, 20, new StringTextComponent(exclusionCache.get(i)), b -> {
                selectedExclusionIdx = idx;
                init();
            });
            if (idx == selectedExclusionIdx) itemBtn.setFGColor(0xFFFF00);
            addButton(itemBtn);
            y += 22;
        }
        return y+10;
    }

    private int buildRendering(int x, int y) {
        addTitle(x, y, "渲染设置"); y+=20;
        y = addToggle(x, y, "启用实体渲染", AimConfig.ENABLE_ENTITY_RENDERING);
        y = addToggle(x, y, "显示连线", AimConfig.ENABLE_LINE_RENDERING);
        y = addToggle(x, y, "显示方框", AimConfig.ENABLE_BOX_RENDERING);
        y = addToggle(x, y, "显示文字标签", AimConfig.ENABLE_TEXT_RENDERING);
        y = addDoubleInput(x, y, "线宽", AimConfig.LINE_WIDTH);
        y = addDoubleInput(x, y, "准星距离", AimConfig.CROSSHAIR_DISTANCE);
        y = addEnumSelector(x, y, "方框渲染模式", AimConfig.BOX_MODE, BoxMode.class);
        y = addDoubleInput(x, y, "方框大小缩放", AimConfig.BOX_SCALE);

        int leftX = x;
        int rightX = x + 220;
        int colY = y + 5;
        y = addColorInput(leftX, colY, "玩家连线颜色", AimConfig.LINE_COLOR_PLAYER);
        int tmpY = addColorInput(rightX, colY, "敌对方框颜色", AimConfig.BOX_COLOR_MONSTER);
        y = Math.max(y, tmpY) + 5;

        tmpY = addColorInput(leftX, y, "动物连线颜色", AimConfig.LINE_COLOR_ANIMAL);
        int tmpY2 = addColorInput(rightX, y, "其他方框颜色", AimConfig.BOX_COLOR_OTHER);
        y = Math.max(tmpY, tmpY2) + 5;

        tmpY = addColorInput(leftX, y, "检索连线颜色", AimConfig.LINE_COLOR_SEARCH);
        tmpY2 = addColorInput(rightX, y, "玩家方框颜色", AimConfig.BOX_COLOR_PLAYER);
        y = Math.max(tmpY, tmpY2) + 5;

        tmpY = addColorInput(leftX, y, "动物方框颜色", AimConfig.BOX_COLOR_ANIMAL);
        tmpY2 = addColorInput(rightX, y, "检索方框颜色", AimConfig.BOX_COLOR_SEARCH);
        y = Math.max(tmpY, tmpY2) + 5;

        y += 4;
        y = addDoubleInput(x, y, "方框透明度", AimConfig.BOX_ALPHA);
        y = addToggle(x, y, "显示名称", AimConfig.TEXT_SHOW_NAME);
        y = addToggle(x, y, "显示血量", AimConfig.TEXT_SHOW_HEALTH);
        y = addToggle(x, y, "显示护甲", AimConfig.TEXT_SHOW_ARMOR);
        y = addToggle(x, y, "显示距离", AimConfig.TEXT_SHOW_DISTANCE);
        y = addToggle(x, y, "显示类型", AimConfig.TEXT_SHOW_ENTITY_TYPE);
        y = addColorInput(x, y, "文字颜色", AimConfig.TEXT_COLOR);
        y = addDoubleInput(x, y, "文字缩放", AimConfig.TEXT_SCALE);
        y = addToggle(x, y, "文字背景", AimConfig.TEXT_BACKGROUND);
        y = addToggle(x, y, "文字阴影", AimConfig.TEXT_SHADOW);
        y = addDoubleInput(x, y, "文字最大距离", AimConfig.TEXT_MAX_DISTANCE);
        y = addIntInput(x, y, "最大渲染数", AimConfig.RENDER_MAX_ENTITIES);
        y = addToggle(x, y, "标签跟随过滤器", AimConfig.TEXT_FOLLOW_FILTER);
        return y+10;
    }

    private int buildGlow(int x, int y) {
        addTitle(x, y, "目标发光"); y+=20;
        y = addToggle(x, y, "目标发光", AimConfig.GLOW_TARGET);
        if (AimConfig.GLOW_TARGET.get()) y = addColorInput(x+20, y, "发光颜色", AimConfig.GLOW_COLOR);
        return y+10;
    }

    private int buildUI(int x, int y) {
        addTitle(x, y, "界面"); y+=20;
        y = addToggle(x, y, "显示简单信息", AimConfig.SHOW_AIM_INFO);
        y = addToggle(x, y, "显示信息面板", AimConfig.SHOW_AIM_PANEL);
        y = addDoubleInput(x, y, "信息 X 位置%", AimConfig.AIM_INFO_X_PERCENT);
        y = addDoubleInput(x, y, "信息 Y 位置%", AimConfig.AIM_INFO_Y_PERCENT);
        if (AimConfig.SHOW_AIM_PANEL.get()) {
            y = addToggle(x+20, y, "面板显示名称", AimConfig.AIM_PANEL_SHOW_NAME);
            y = addToggle(x+20, y, "面板显示血量", AimConfig.AIM_PANEL_SHOW_HEALTH);
            y = addToggle(x+20, y, "面板显示护甲", AimConfig.AIM_PANEL_SHOW_ARMOR);
            y = addToggle(x+20, y, "面板显示距离", AimConfig.AIM_PANEL_SHOW_DISTANCE);
            y = addToggle(x+20, y, "面板显示类型", AimConfig.AIM_PANEL_SHOW_TYPE);
        }
        return y+10;
    }

    // ---------- 控件辅助 ----------
    private TextFieldWidget createTextField(int x, int y, int width, String initial, Consumer<String> responder) {
        TextFieldWidget field = new TextFieldWidget(font, x, y, width, 16, new StringTextComponent(""));
        field.setMaxLength(100);
        field.setValue(initial);
        field.setResponder(responder);
        field.setVisible(true);
        this.children.add(field);
        allFields.add(field);
        return field;
    }

    private void addTitle(int x, int y, String text) {
        labels.add(new LabelEntry("§n" + text, x, y, 0xFFFFFF));
    }

    private void addLabel(int x, int y, String text, int color) {
        labels.add(new LabelEntry(text, x, y, color));
    }

    private int addToggle(int x, int y, String label, ForgeConfigSpec.BooleanValue cfg) {
        boolean val = cfg.get();
        addButton(new Button(x, y, 170, 20, new StringTextComponent(label + ": " + (val ? "§a开" : "§c关")), b -> {
            cfg.set(!cfg.get());
            init();
        }));
        return y+22;
    }

    private int addIntInput(int x, int y, String label, ForgeConfigSpec.IntValue cfg) {
        labels.add(new LabelEntry(label, x, y+2, 0xFFFFFF));
        TextFieldWidget f = new TextFieldWidget(font, x+LABEL_W+10, y, 60, 16, new StringTextComponent(""));
        f.setValue(String.valueOf(cfg.get()));
        f.setFilter(s -> s.matches("\\d*"));
        f.setVisible(true);
        this.children.add(f);
        allFields.add(f);
        intBindings.put(f, cfg);
        return y+20;
    }

    private int addDoubleInput(int x, int y, String label, ForgeConfigSpec.DoubleValue cfg) {
        labels.add(new LabelEntry(label, x, y+2, 0xFFFFFF));
        TextFieldWidget f = new TextFieldWidget(font, x+LABEL_W+10, y, 60, 16, new StringTextComponent(""));
        f.setValue(String.format("%.2f", cfg.get()));
        f.setFilter(s -> s.matches("\\d*\\.?\\d*"));
        f.setVisible(true);
        this.children.add(f);
        allFields.add(f);
        doubleBindings.put(f, cfg);
        return y+20;
    }

    private <T extends Enum<T>> int addEnumSelector(int x, int y, String label, ForgeConfigSpec.EnumValue<T> cfg, Class<T> clazz) {
        labels.add(new LabelEntry(label, x, y+2, 0xFFFFFF));
        T cur = cfg.get();
        T[] values = clazz.getEnumConstants();
        addButton(new Button(x+LABEL_W+10, y, 120, 20, new StringTextComponent(cur.toString()), b -> {
            T next = values[(cur.ordinal()+1)%values.length];
            cfg.set(next);
            init();
        }));
        return y+22;
    }

    private int addColorInput(int x, int y, String label, ForgeConfigSpec.IntValue cfg) {
        labels.add(new LabelEntry(label, x, y+2, 0xFFFFFF));
        int color = cfg.get() & 0xFFFFFF;
        String hex = String.format("#%06X", color);
        TextFieldWidget f = new TextFieldWidget(font, x+LABEL_W+10, y, 70, 16, new StringTextComponent(""));
        f.setValue(hex);
        f.setFilter(s -> s.matches("#?[0-9a-fA-F]*"));
        f.setResponder(s -> {
            try { int val = Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16); cfg.set(val & 0xFFFFFF); } catch (NumberFormatException ignored) {}
        });
        f.setVisible(true);
        this.children.add(f);
        allFields.add(f);
        int previewX = x + LABEL_W + 10 + 70 + 2;
        colorPreviews.add(new ColorPreview(previewX, y, 14, () -> cfg.get() | 0xFF000000));
        return y+20;
    }

    // ---------- 渲染与交互 ----------
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partial) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partial);   // 渲染按钮等

        // 手动绘制输入框，确保它们可见，同时保留光标
        for (TextFieldWidget field : allFields) {
            field.render(matrixStack, mouseX, mouseY, partial);
        }

        // 静态标签
        for (LabelEntry label : labels) {
            font.draw(matrixStack, label.text, label.x, label.y, label.color);
        }

        // 颜色预览块
        for (ColorPreview preview : colorPreviews) {
            int col = preview.colorSupplier.getAsInt();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            fill(matrixStack, preview.x, preview.y, preview.x + preview.size, preview.y + preview.size, col);
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < SIDEBAR_W + 10) {
            sidebarScrollY -= (int)(delta * 10);
        } else {
            contentScrollY -= (int)(delta * 10);
        }
        init();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 优先让 TextFieldWidget 处理点击（获取焦点、光标等）
        for (TextFieldWidget field : allFields) {
            if (field.isMouseOver(mouseX, mouseY)) {
                setFocused(field);
                return field.mouseClicked(mouseX, mouseY, button);
            }
        }
        // 如果没点到输入框，清除焦点
        setFocused(null);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveAndClose();
            return true;
        }
        // 将键盘事件传递给当前焦点组件（通常是 TextFieldWidget）
        if (getFocused() instanceof TextFieldWidget) {
            return getFocused().keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (getFocused() instanceof TextFieldWidget) {
            return getFocused().charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void saveAndClose() {
        for (Map.Entry<TextFieldWidget, ForgeConfigSpec.IntValue> e : intBindings.entrySet()) {
            try { e.getValue().set(Integer.parseInt(e.getKey().getValue())); } catch (NumberFormatException ignored) {}
        }
        for (Map.Entry<TextFieldWidget, ForgeConfigSpec.DoubleValue> e : doubleBindings.entrySet()) {
            try { e.getValue().set(Double.parseDouble(e.getKey().getValue())); } catch (NumberFormatException ignored) {}
        }
        if (AimAssistant.CLIENT_CONFIG != null) AimAssistant.CLIENT_CONFIG.save();
        onClose();
    }

    private static synchronized List<String> getAllEntityIds() {
        if (allEntityIdsCache == null) {
            allEntityIdsCache = ForgeRegistries.ENTITIES.getKeys().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .collect(Collectors.toList());
        }
        return allEntityIdsCache;
    }

    private List<String> getFilteredEntityIds(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (!lower.contains("*")) {
            return getAllEntityIds().stream().filter(id -> id.startsWith(lower)).collect(Collectors.toList());
        } else {
            String regex = java.util.regex.Pattern.quote(lower).replace("*", "\\E.*\\Q");
            return getAllEntityIds().stream().filter(id -> id.matches(regex)).collect(Collectors.toList());
        }
    }
}