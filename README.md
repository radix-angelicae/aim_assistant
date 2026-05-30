 `该项目由AI制作，存在一些玄学bug，但不影响正常使用。`
# Aim Assistant - 辅助瞄准模组

**Aim Assistant** 是一个高度可定制的 Minecraft Forge 辅助瞄准模组，提供实体锁定、自动攻击、多模式渲染和丰富的配置选项，适用于 PvP、生存和整合包环境。

---

## ✨ 主要功能

### 🎯 智能瞄准系统
- **按键模式**：支持 **按住** 和 **切换** 两种激活方式，灵活适应不同操作习惯。
- **目标优先级**：可选择 **距离优先**、**角度优先**、**生命值优先** 或 **随机**，满足不同战术需求。
- **锁定部位**：可精确锁定目标的 **头部**、**身体** 或 **脚部**。
- **平滑视角移动**：通过可配置的平滑系数，使视角切换更自然流畅。
- **自动攻击**：当武器冷却完毕时自动攻击锁定目标（不附加额外延迟，完全遵循原版攻速）。
- **条件激活**：可设置仅在手持特定物品（或物品标签）时启用辅助瞄准，避免误触发。

### 🛡️ 高级过滤与锁定
- **过滤模式**：
  - **全部**（玩家除外）
  - **敌对生物**（支持智能检测，识别任何主动攻击玩家的生物）
  - **动物**
  - **自定义检索列表**
- **排除列表**：支持实体 ID、通配符（`*`）和实体标签（`#forge:bosses`），被排除的实体不会被瞄准或渲染。
- **检索列表**：仅在“检索模式”下生效，同样支持通配符和标签。
- **智能敌对检测**：开启后，任何将玩家作为攻击目标的生物（即使不是原版怪物）都会被识别为敌对。
- **队友与宠物保护**：自动忽略同队伍玩家和自己驯服的宠物。
- **忽略隐身实体**：可选择是否忽略隐身的实体。

### 🔒 强制锁定（Force Lock）
- 按下独立按键可强制锁定当前目标，即使目标被方块遮挡也不会丢失锁定。
- 锁定后目标框线会加粗并改变颜色（线宽和颜色可单独配置），便于识别。
- 再次按下按键取消强制锁定。

### 🖼️ 双模式渲染
模组提供了 **世界渲染** 和 **屏幕渲染** 两种模式，可在配置中自由切换。

#### 世界渲染模式
- 在游戏世界中绘制 **连线**、**方框** 和 **文字标签**。
- 连线从准星位置延伸至实体。
- 方框支持 **完整 3D 框**、**角框**、**底面**、**顶面** 四种样式，大小和透明度可调。
- 文字标签穿墙显示，可自定义颜色、大小、背景、阴影，并显示名称、生命值、护甲、距离、实体类型等信息。

#### 屏幕渲染模式
- 将所有标记（连线、方框、标签）以 UI 形式直接绘制在屏幕上，不受游戏雾效和天空颜色影响。
- 连线从屏幕中心指向实体投影位置，不随视角晃动。
- 标签大小会根据距离动态缩放，避免远处实体被标签遮挡。

### ✨ 目标发光与反馈
- 锁定目标时显示 **高亮方框**，颜色和线宽可配置。
- 强制锁定时使用独立的颜色和线宽，视觉效果更加醒目。
- **锁定音效**：成功锁定目标时播放提示音（音量可调），提供听觉反馈。

### 📊 信息面板
- 可显示详细的锁定目标信息，包括：名称、生命值、最大生命值、护甲、距离、实体类型、状态效果、手持物品等。
- 每项信息可独立开关，面板位置可通过百分比坐标调整。
- 简约信息条：仅显示名称、生命值、距离，适合轻量化使用。

### ⚙️ 高度可定制配置
- 内置图形化配置界面，支持中文/英文等多语言。
- 所有视觉元素（连线、方框、标签）的颜色均可独立设置。
- 可分别调整 **搜索范围**、**锁定距离**、**FOV 角度**、**最大渲染实体数**、**文字最大距离** 等参数。
- 支持导入外部实体分类配置文件（`entity_categories.json`），自定义哪些实体被视为敌对或友好。

### 🌐 外部分类支持
- 在 `config/aim_assistant/entity_categories.json` 中可定义自定义实体分类，整合包作者可以轻松适配非原版生物。
- 使用 `/aim_reload` 命令热重载分类文件，无需重启游戏。

---

## 🎮 默认按键绑定
所有按键均无默认键位，需要玩家手动设置：

| 功能 | 描述 |
|------|------|
| **辅助瞄准** | 激活瞄准锁定 |
| **持续渲染开关** | 手动开关实体渲染 |
| **打开配置界面** | 打开图形化配置菜单 |
| **切换过滤器** | 循环切换过滤模式 |
| **开关目标切换** | 允许/禁止自动切换目标 |
| **下一个目标** | 手动切换到下一个候选目标 |
| **锁死当前目标** | 强制锁定当前目标（忽略方块遮挡） |

---

## 📁 配置文件
所有设置保存在 `.minecraft/config/aim_assistant-client.toml`，可通过游戏内配置界面修改，也可直接编辑文件。

实体分类文件位于 `.minecraft/config/aim_assistant/entity_categories.json`，格式如下：
```json
{
  "hostile": ["minecraft:zombie", "mymod:fire_dragon"],
  "friendly": ["minecraft:villager"],
  "neutral": ["minecraft:wolf"]
}  全部（玩家除外）。
- `HOSTILE` – Only monsters (instances of `Monster`).  
  仅敌对生物。
- `ANIMAL` – Only animals (instances of `Animal`).  
  仅动物。
- `SEARCH` – Only entities whose registry ID (e.g. `minecraft:zombie`) matches any pattern in the **Search List**. Supports wildcard `*`.  
  仅检索列表中的实体。支持通配符 `*`，例如 `minecraft:zombie*` 可匹配所有僵尸变种。

> **Note / 注意**  
> In `SEARCH` mode, the search list is also used for line and box colors (`LINE_COLOR_SEARCH` / `BOX_COLOR_SEARCH`), so you can visually distinguish listed targets.  
> 在 `SEARCH` 模式下，检索列表也会决定连线颜色和方框颜色，便于视觉区分。

### 3. Exclusion List / 排除列表

Entities matching any pattern in this list are **completely ignored** – not aimed at, not drawn with lines or boxes, not considered for anything.  
匹配此列表中任意模式的实体将被**完全忽略** – 不瞄准、不绘制连线/方框、不参与任何判定。

- Supports wildcard `*`, e.g. `minecraft:wolf` excludes wolves, `minecraft:*_zombie` excludes all entities ending with `_zombie`.  
  支持通配符 `*`，例如 `minecraft:wolf` 排除狼，`minecraft:*_zombie` 排除所有以 `_zombie` 结尾的实体。

- Exclusion list is applied **after** the filter mode. If an entity passes the filter but is excluded, it still won’t be targeted or rendered.  
  排除列表在过滤模式**之后**应用。如果实体通过了过滤器但被排除，仍然不会成为目标或被渲染。

### 4. Visual Rendering / 视觉渲染

All rendering is independent of aiming and can be toggled on/off with the **Persistent Render** key binding.  
所有渲染独立于瞄准功能，可通过**持续渲染开关**按键单独开启/关闭。

#### Lines (from crosshair point to entity) / 连线（从准星点到实体）
- Color configurable by entity type (Player / Monster / Animal / Other / Search).  
  颜色按实体类型独立配置（玩家/敌对/动物/其他/检索）。
- Line width adjustable.  
  线宽可调。

#### Bounding Boxes / 包围盒
- **Box Mode / 方框模式**  
  - `FULL_3D` – Complete wireframe cube.  
    完整立方体线框。  
  - `CORNERS` – Only 3 small lines at each corner.  
    仅每个角上的三根短边（角框）。  
  - `BOTTOM` – Only the bottom face.  
    仅底面。  
  - `TOP` – Only the top face.  
    仅顶面。
- **Box Scale / 框大小缩放**  
  Adjusts the bounding box size relative to the entity’s actual hitbox. Useful for making boxes slightly larger or smaller than the model.  
  调整包围盒相对于实体实际碰撞箱的大小。可用于让框比模型稍大或稍小。
- **Box Alpha / 方框透明度** – 0 = fully transparent, 1 = fully opaque.  
  0 = 完全透明，1 = 完全不透明。

#### Text Labels / 文字标签
- Displays floating text above entities.  
  在实体上方显示悬浮文字。
- Configurable fields: Name, Health (❤), Armor (🛡), Distance, Entity Type.  
  可配置字段：名称、血量（❤）、护甲（🛡）、距离、实体类型。
- **Text Scale / 文字缩放** – Because 3D world scale differs from screen scale, values are typically very small (default 0.025). Increase to make text larger.  
  因 3D 世界尺度与屏幕尺度不同，该值通常很小（默认 0.025）。增大可使文字变大。
- **Text Max Distance / 文字最大距离** – Labels beyond this range are not rendered.  
  超出此距离的标签不渲染。
- **Text Follow Filter / 标签跟随过滤器**  
  - ON: Only shows labels for entities that pass the current filter mode.  
    开启：仅对通过当前过滤器的实体显示标签。  
  - OFF: Shows labels for **all** living entities (except excluded ones).  
    关闭：对**所有**生物显示标签（排除列表中的除外）。

#### Target Glow / 目标发光
- When a target is locked, a glowing outline (with configurable color) highlights it.  
  锁定目标时，目标会显示发光轮廓（颜色可配置）。

### 5. Auto Attack / 自动攻击

When enabled, the mod will automatically attack the locked target once the player’s attack strength (weapon cooldown) reaches 100% and the target is within reach.  
启用后，当玩家的攻击强度（武器冷却）达到 100% 且目标在触及范围内时，模组会自动攻击锁定的目标。

- Uses `Minecraft.gameMode.attack()` – same as left-clicking.  
  使用 `Minecraft.gameMode.attack()` – 等同于左键攻击。
- Does not bypass any game mechanics (reach limit, invincibility frames, etc.).  
  不会绕过任何游戏机制（触及距离限制、无敌帧等）。

### 6. Wall Penetration & Invisibility / 穿墙 & 隐身

- **Allow Wall Penetration / 允许穿墙锁定**  
  OFF: Aiming requires a clear line of sight (raycast to target’s eye position).  
  关闭：瞄准需要视线无阻挡（射线检测到目标眼睛位置）。  
  ON: Line of sight is ignored.  
  开启：忽略视线阻挡。

- **Ignore Invisible / 忽略隐身**  
  ON: Invisible entities (e.g. from potion effects) are never targeted.  
  开启：永远不会瞄准隐身实体（如药水效果）。

### 7. Aim Key Modes / 瞄准按键模式

- `HOLD` – Aiming is active only while the key is pressed down.  
  按住 – 仅当按键按住时瞄准激活。
- `TOGGLE` – Press once to enable aiming, press again to disable.  
  切换 – 按一次启用瞄准，再按一次禁用。

### 8. Target Switching / 目标切换

- **Allow Target Switching / 允许目标切换**  
  ON: The mod continuously re-evaluates the best target and can switch to a better one.  
  开启：模组持续重新评估最佳目标，可切换到更优目标。  
  OFF: Once locked, the target will not change until it dies, goes out of range, or becomes invalid.  
  关闭：锁定后，目标不会改变，直到其死亡、超出范围或变为无效。

- A dedicated key binding `Toggle Target Switch` can turn this on/off in-game without opening the config screen.  
  专用按键绑定 `Toggle Target Switch` 可在游戏中开关此功能，无需打开配置界面。

### 9. HUD Info & Panel / 界面信息

- **Simple Aim Info / 简单信息** – One line showing current target’s name, health, and distance. Position adjustable by percentage from top-left.  
  简单信息 – 一行文字显示当前目标的名称、血量、距离。位置可调（基于左上角的百分比）。
- **Aim Panel / 信息面板** – A multi-line panel (position fixed at top-left) showing detailed fields: name, health, armor, distance, type. Each field can be toggled individually.  
  信息面板 – 多行面板（位置固定在左上角），显示详细字段：名称、血量、护甲、距离、类型。每个字段可单独开关。

### 10. Configuration Screen / 配置界面

A full GUI to adjust every setting:  
完整的图形界面，可调整所有设置：

- Numeric fields with range validation  
  带范围验证的数字输入框
- Color pickers (hex input + live preview swatch)  
  颜色选择器（十六进制输入 + 实时预览色块）
- Entity list editors with wildcard suggestions (auto-completes from registered entity types)  
  实体列表编辑器，带通配符建议（自动补全已注册的实体类型）
- Scrolling for long lists and content  
  长列表和内容支持滚动

Press the configured **Open Config** key to open it. All changes are saved when you close the screen.  
按下配置的 **打开配置界面** 键打开。关闭界面时自动保存所有更改。
